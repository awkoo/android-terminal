package awkoo.terminal.shell;

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
import awkoo.terminal.terminal.TerminalSession;
import awkoo.terminal.terminal.TerminalSessionClient;
import awkoo.terminal.utils.errors.Errno;

/**
 * 一个维护前台 Termux 会话信息的类。它还提供了一种将每个 {@link TerminalSession} 与启动它的 {@link ShellCommand} 相关联的方法。
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
     * 使用 {@link Runtime#exec(String[], String[], File)} 启动 {@link ShellCommand} 的执行。
     * <p>
     * 必须设置 {@link ShellCommand#executable}，可以可选设置 {@link ShellCommand#commandLabel}、
     * {@link ShellCommand#arguments} 和 {@link ShellCommand#workingDirectory}。
     * <p>
     * 如果 {@link ShellCommand#executable} 为 {@code null}，则会自动选择默认 shell。
     *
     * @param context               用于操作的 {@link Context}。这必须是当前包的上下文，而不是 `sharedUserId` 包的上下文，
     *                              因为环境设置可能依赖于当前包。
     * @param shellCommand          包含执行命令信息的 {@link ShellCommand}。
     * @param terminalSessionClient {@link TerminalSessionClient} 接口实现。
     * @param termuxSessionClient   {@link TermuxSessionClient} 接口实现。
     * @param additionalEnvironment 要导出的附加 shell 环境变量。现有变量将被覆盖。
     * @param setStdoutOnExit       如果设置为 {@code true}，则 {@link TermuxSessionClient#onSessionExited(TerminalShell)} 回调中可用的 {@link ResultData#stdout}
     *                              将设置为 {@link TerminalSession} 脚本。会话脚本将包含 stdout 和 stderr 的组合，基本上是发送到伪终端 /dev/pts 的任何内容，包括 PS1 前缀。
     *                              仅当需要会话脚本时才将其设置为 {@code true}，因为这需要额外的处理才能获取它。
     * @return 返回 {@link TerminalShell}。如果无法启动执行命令，则返回 {@code null}。
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


        // 设置命令环境
        ShellEnvironment environment = new ShellEnvironment();
        environment.put("HOME", context.getFilesDir().getAbsolutePath());
        if (additionalEnvironment != null)
            environment.putAll(additionalEnvironment);

        List<String> environmentList = new ArrayList<>(environment.size());
        for (String name : environment.keySet())
            environmentList.add(name + "=" + environment.get(name));
        Collections.sort(environmentList);
        String[] environmentArray = environmentList.toArray(new String[0]);


        // 设置命令参数
        List<String> result = new ArrayList<>();
        // root权限
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

        shellCommand.setState(ShellCommand.State.EXECUTING);

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
     * 发出此 {@link TerminalShell} 已完成的信号。当调用方收到 {@link TerminalSessionClient#onSessionFinished(TerminalSession)} 回调时，应调用此方法。
     * <p>
     * 如果进程已完成，则为 {@code termuxTask} 的 {@link #mShellCommand} 设置 {@link ResultData#stdout}、{@link ResultData#stderr}
     * 和 {@link ResultData#exitCode}，然后调用 {@link #processTermuxSessionResult(TerminalShell, ShellCommand)} 处理结果。
     */
    public void finish() {
        // 如果进程仍在运行，则忽略此调用
        if (mTerminalSession.isRunning()) return;

        int exitCode = mTerminalSession.getExitStatus();

        // 如果执行命令已经失败，例如发送了 SIGKILL，则不要继续
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
     * 如果此 {@link TerminalShell} 仍在执行，则通过向其 {@link #mTerminalSession} 发送 {@link OsConstants#SIGILL} 来终止它。
     *
     * @param context       用于操作的 {@link Context}。
     * @param processResult 如果设置为 {@code true}，则将调用 {@link #processTermuxSessionResult(TerminalShell, ShellCommand)}
     *                      来处理失败。
     */
    public void killIfExecuting(@NonNull final Context context, boolean processResult) {
        // 如果执行命令已经完成，则无需处理结果或发送 SIGKILL
        if (mShellCommand.hasExecuted()) return;

        if (mShellCommand.setStateFailed(Errno.ERRNO_FAILED.getCode(), context.getString(R.string.error_sending_sigkill_to_process))) {
            if (processResult) {
                mShellCommand.resultData.exitCode = 137; // SIGKILL

                // 获取迄今为止已设置的任何输出，以防需要
                if (this.mSetStdoutOnExit)
                    mShellCommand.resultData.stdout.append(ShellUtils.getTerminalSessionTranscriptText(mTerminalSession, true, false));

                TerminalShell.processTermuxSessionResult(this, null);
            }
        }

        // 发送 SIGKILL 到进程
        mTerminalSession.finishIfRunning();
    }

    /**
     * 处理 {@link TerminalShell} 或 {@link ShellCommand} 的结果。
     * <p>
     * {@code terminalShell} 和 {@code shellCommand} 中只能设置一个。
     * <p>
     * 如果 {@code terminalShell} 及其 {@link #mTermuxSessionClient} 不为 {@code null}，
     * 则将调用 {@link TerminalShell.TermuxSessionClient#onSessionExited(TerminalShell)} 回调。
     *
     * @param terminalShell {@link TerminalShell}，如果
     *                      {@link #execute(Context, ShellCommand, TerminalSessionClient, TermuxSessionClient, HashMap, Boolean)}
     *                      成功启动进程，则应设置此参数。
     * @param shellCommand  {@link ShellCommand}，如果
     *                      {@link #execute(Context, ShellCommand, TerminalSessionClient, TermuxSessionClient, HashMap, Boolean)}
     *                      未能启动进程，则应设置此参数。
     */
    private static void processTermuxSessionResult(final TerminalShell terminalShell, ShellCommand shellCommand) {
        if (terminalShell != null)
            shellCommand = terminalShell.mShellCommand;

        if (shellCommand == null) return;

        if (shellCommand.shouldNotProcessResults()) return;

        if (terminalShell != null && terminalShell.mTermuxSessionClient != null) {
            terminalShell.mTermuxSessionClient.onSessionExited(terminalShell);
        } else {
            // 如果未设置回调且执行命令未失败，则我们现在设置成功状态
            // 否则，回调宿主可以在完成 TerminalShell 后自行设置
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
         * 当 {@link TerminalShell} 退出时的回调函数。
         *
         * @param terminalShell 退出的 {@link TerminalShell}。
         */
        void onSessionExited(TerminalShell terminalShell);

    }

}
