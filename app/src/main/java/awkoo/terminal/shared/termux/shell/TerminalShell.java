package awkoo.terminal.shared.termux.shell;

import android.content.Context;
import android.content.SharedPreferences;
import android.system.OsConstants;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import awkoo.terminal.R;
import awkoo.terminal.shared.errors.Errno;
import awkoo.terminal.shared.shell.ShellUtils;
import awkoo.terminal.shared.shell.command.ShellCommand;
import awkoo.terminal.shared.shell.command.environment.ShellEnvironment;
import awkoo.terminal.shared.shell.command.environment.ShellEnvironmentUtils;
import awkoo.terminal.shared.shell.command.result.ResultData;
import awkoo.terminal.terminal.TerminalSession;
import awkoo.terminal.terminal.TerminalSessionClient;

/**
 * A class that maintains info for foreground Termux sessions.
 * It also provides a way to link each {@link TerminalSession} with the {@link ShellCommand}
 * that started it.
 */
public class TerminalShell {

    private final TerminalSession mTerminalSession;
    private final ShellCommand mShellCommand;
    private final TermuxSessionClient mTermuxSessionClient;
    private final boolean mSetStdoutOnExit;

    private TerminalShell(
        @NonNull final TerminalSession terminalSession,
        @NonNull final ShellCommand shellCommand,
        final TermuxSessionClient termuxSessionClient,
        final boolean setStdoutOnExit
    ) {
        this.mTerminalSession = terminalSession;
        this.mShellCommand = shellCommand;
        this.mTermuxSessionClient = termuxSessionClient;
        this.mSetStdoutOnExit = setStdoutOnExit;
    }

    /**
     * Start execution of an {@link ShellCommand} with {@link Runtime#exec(String[], String[], File)}.
     * <p>
     * The {@link ShellCommand#executable}, must be set, {@link ShellCommand#commandLabel},
     * {@link ShellCommand#arguments} and {@link ShellCommand#workingDirectory} may optionally
     * be set.
     * <p>
     * If {@link ShellCommand#executable} is {@code null}, then a default shell is automatically
     * chosen.
     *
     * @param context               The {@link Context} for operations. This must be the context for
     *                              the current package and not the context of a `sharedUserId` package,
     *                              since environment setup may be dependent on current package.
     * @param shellCommand      The {@link ShellCommand} containing the information for execution command.
     * @param terminalSessionClient The {@link TerminalSessionClient} interface implementation.
     * @param termuxSessionClient   The {@link TermuxSessionClient} interface implementation.
     * @param additionalEnvironment The additional shell environment variables to export. Existing
     *                              variables will be overridden.
     * @param setStdoutOnExit       If set to {@code true}, then the {@link ResultData#stdout}
     *                              available in the {@link TermuxSessionClient#onSessionExited(TerminalShell)}
     *                              callback will be set to the {@link TerminalSession} transcript. The session
     *                              transcript will contain both stdout and stderr combined, basically
     *                              anything sent to the the pseudo terminal /dev/pts, including PS1 prefixes.
     *                              Set this to {@code true} only if the session transcript is required,
     *                              since this requires extra processing to get it.
     * @return Returns the {@link TerminalShell}. This will be {@code null} if failed to start the execution command.
     */
    public static TerminalShell execute(
        Context context,
        ShellCommand shellCommand,
        TerminalSessionClient terminalSessionClient,
        TermuxSessionClient termuxSessionClient,
        HashMap<String, String> additionalEnvironment,
        Boolean setStdoutOnExit
    ) {
        if (shellCommand.executable == null || shellCommand.executable.isEmpty())
            shellCommand.executable = ShellEnvironment.defaultShell;
        if (shellCommand.workingDirectory == null || shellCommand.workingDirectory.isEmpty())
            shellCommand.workingDirectory = ShellEnvironment.defaultWorkingPath;
        if (shellCommand.commandLabel == null || shellCommand.commandLabel.isEmpty())
            shellCommand.commandLabel = shellCommand.executable;


        // Setup command environment
        ShellEnvironment environment = new ShellEnvironment();
        environment.put("HOME", context.getFilesDir().getAbsolutePath());
        //在使用su前设置，防止被覆盖
        environment.put("SHELL", shellCommand.executable);
        if (additionalEnvironment != null)
            environment.putAll(additionalEnvironment);
        List<String> environmentList = ShellEnvironmentUtils.convertEnvironmentToEnviron(environment);
        Collections.sort(environmentList);
        String[] environmentArray = environmentList.toArray(new String[0]);


        // Setup command args
        List<String> result = new ArrayList<>();
        // root
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (shellCommand.mode == ShellCommand.Mode.ROOT) {
            result.add(preferences.getString("su_path", "/system/bin/su"));
            result.add("-s");
        }
        result.add(shellCommand.executable);
        if (shellCommand.arguments != null)
            Collections.addAll(result, shellCommand.arguments);
        String[] commandArgs = result.toArray(new String[0]);
        shellCommand.executable = commandArgs[0];
        String[] arguments = new String[commandArgs.length];
        arguments[0] = shellCommand.executable;
        if (commandArgs.length > 1)
            System.arraycopy(commandArgs, 1, arguments, 1, commandArgs.length - 1);
        shellCommand.arguments = arguments;

        if (!shellCommand.setState(ShellCommand.State.EXECUTING)) {
            shellCommand.setStateFailed(
                Errno.ERRNO_FAILED.getCode(),
                context.getString(
                    R.string.error_failed_to_execute_termux_session_command,
                    shellCommand.getCommandIdAndLabelLogString()
                )
            );
            TerminalShell.processTermuxSessionResult(null, shellCommand);
            return null;
        }

        TerminalSession terminalSession = new TerminalSession(
            context,
            shellCommand.executable,
            shellCommand.workingDirectory,
            shellCommand.arguments,
            environmentArray,
            shellCommand.stdin,
            shellCommand.terminalTranscriptRows,
            terminalSessionClient
        );

        if (shellCommand.shellName != null) {
            terminalSession.mSessionName = shellCommand.shellName;
        }

        return new TerminalShell(terminalSession, shellCommand, termuxSessionClient, setStdoutOnExit);
    }

    /**
     * Signal that this {@link TerminalShell} has finished.  This should be called when
     * {@link TerminalSessionClient#onSessionFinished(TerminalSession)} callback is received by the caller.
     * <p>
     * If the processes has finished, then sets {@link ResultData#stdout}, {@link ResultData#stderr}
     * and {@link ResultData#exitCode} for the {@link #mShellCommand} of the {@code termuxTask}
     * and then calls {@link #processTermuxSessionResult(TerminalShell, ShellCommand)} to process the result}.
     */
    public void finish() {
        // If process is still running, then ignore the call
        if (mTerminalSession.isRunning()) return;

        int exitCode = mTerminalSession.getExitStatus();

        // If the execution command has already failed, like SIGKILL was sent, then don't continue
        if (mShellCommand.isStateFailed()) return;

        mShellCommand.resultData.exitCode = exitCode;

        if (this.mSetStdoutOnExit)
            mShellCommand.resultData.stdout.append(
                ShellUtils.getTerminalSessionTranscriptText(
                    mTerminalSession,
                    true,
                    false
                )
            );

        if (!mShellCommand.setState(ShellCommand.State.EXECUTED))
            return;

        TerminalShell.processTermuxSessionResult(this, null);
    }

    /**
     * Kill this {@link TerminalShell} by sending a {@link OsConstants#SIGILL} to its {@link #mTerminalSession}
     * if its still executing.
     *
     * @param context       The {@link Context} for operations.
     * @param processResult If set to {@code true}, then the {@link #processTermuxSessionResult(TerminalShell, ShellCommand)}
     *                      will be called to process the failure.
     */
    public void killIfExecuting(@NonNull final Context context, boolean processResult) {
        // If execution command has already finished executing, then no need to process results or send SIGKILL
        if (mShellCommand.hasExecuted()) return;

        if (mShellCommand.setStateFailed(Errno.ERRNO_FAILED.getCode(), context.getString(R.string.error_sending_sigkill_to_process))) {
            if (processResult) {
                mShellCommand.resultData.exitCode = 137; // SIGKILL

                // Get whatever output has been set till now in case its needed
                if (this.mSetStdoutOnExit)
                    mShellCommand.resultData.stdout.append(ShellUtils.getTerminalSessionTranscriptText(mTerminalSession, true, false));

                TerminalShell.processTermuxSessionResult(this, null);
            }
        }

        // Send SIGKILL to process
        mTerminalSession.finishIfRunning();
    }

    /**
     * Process the results of {@link TerminalShell} or {@link ShellCommand}.
     * <p>
     * Only one of {@code terminalShell} and {@code shellCommand} must be set.
     * <p>
     * If the {@code terminalShell} and its {@link #mTermuxSessionClient} are not {@code null},
     * then the {@link TerminalShell.TermuxSessionClient#onSessionExited(TerminalShell)}
     * callback will be called.
     *
     * @param terminalShell    The {@link TerminalShell}, which should be set if
     *                         {@link #execute(Context, ShellCommand, TerminalSessionClient, TermuxSessionClient, HashMap, Boolean)}
     *                         successfully started the process.
     * @param shellCommand The {@link ShellCommand}, which should be set if
     *                         {@link #execute(Context, ShellCommand, TerminalSessionClient, TermuxSessionClient, HashMap, Boolean)}
     *                         failed to start the process.
     */
    private static void processTermuxSessionResult(final TerminalShell terminalShell, ShellCommand shellCommand) {
        if (terminalShell != null)
            shellCommand = terminalShell.mShellCommand;

        if (shellCommand == null) return;

        if (shellCommand.shouldNotProcessResults()) return;

        if (terminalShell != null && terminalShell.mTermuxSessionClient != null) {
            terminalShell.mTermuxSessionClient.onSessionExited(terminalShell);
        } else {
            // If a callback is not set and execution command didn't fail, then we set success state now
            // Otherwise, the callback host can set it himself when its done with the terminalShell
            if (!shellCommand.isStateFailed())
                shellCommand.setState(ShellCommand.State.SUCCESS);
        }
    }

    public TerminalSession getTerminalSession() {
        return mTerminalSession;
    }

    public ShellCommand getExecutionCommand() {
        return mShellCommand;
    }


    public interface TermuxSessionClient {

        /**
         * Callback function for when {@link TerminalShell} exits.
         *
         * @param terminalShell The {@link TerminalShell} that exited.
         */
        void onSessionExited(TerminalShell terminalShell);

    }

}
