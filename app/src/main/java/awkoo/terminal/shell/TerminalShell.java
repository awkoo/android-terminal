package awkoo.terminal.shell;

import android.content.Context;
import android.content.SharedPreferences;
import android.system.OsConstants;

import androidx.preference.PreferenceManager;

import java.io.File;
import java.util.HashMap;

import awkoo.terminal.terminal.TerminalSession;
import awkoo.terminal.terminal.TerminalSessionClient;

/**
 * 一个维护前台 Termux 会话信息的类。它还提供了一种将每个 {@link TerminalSession} 与启动它的 {@link ShellCommand} 相关联的方法。
 */
public class TerminalShell {

    private final TerminalSession mTerminalSession;
    private final ShellCommand mShellCommand;
    private final TermuxSessionClient mTermuxSessionClient;

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
     */
    public TerminalShell(
        Context context,
        ShellCommand shellCommand,
        TerminalSessionClient terminalSessionClient,
        TermuxSessionClient termuxSessionClient,
        HashMap<String, String> additionalEnvironment
    ) {
        this.mShellCommand = shellCommand;
        this.mTermuxSessionClient = termuxSessionClient;

        if (mShellCommand.executable == null || mShellCommand.executable.isEmpty())
            mShellCommand.executable = ShellEnvironment.defaultShell;
        if (mShellCommand.workingDirectory == null || mShellCommand.workingDirectory.isEmpty())
            mShellCommand.workingDirectory = ShellEnvironment.defaultWorkingPath;
        if (mShellCommand.commandLabel == null || mShellCommand.commandLabel.isEmpty())
            mShellCommand.commandLabel = mShellCommand.executable;


        // 设置命令环境
        mShellCommand.environment.put("HOME", context.getFilesDir().getAbsolutePath());
        if (additionalEnvironment != null)
            mShellCommand.environment.putAll(additionalEnvironment);


        // root权限
        if (mShellCommand.mode == ShellCommand.Mode.ROOT) {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
            // 设置命令参数
            String[] args = new String[(mShellCommand.arguments != null ? mShellCommand.arguments.length : 0) + 2];
            args[0] = "-c";
            args[1] = mShellCommand.executable;
            if (mShellCommand.arguments != null && mShellCommand.arguments.length > 0)
                System.arraycopy(mShellCommand.arguments, 0, args, 2, mShellCommand.arguments.length);
            mShellCommand.arguments = args;
            mShellCommand.executable = preferences.getString("su_path", "/system/bin/su");
        }

        // 第一个元素必须为可执行文件
        String[] arguments = new String[(mShellCommand.arguments != null ? mShellCommand.arguments.length : 0) + 1];
        arguments[0] = mShellCommand.executable;
        if (mShellCommand.arguments != null && mShellCommand.arguments.length > 0)
            System.arraycopy(mShellCommand.arguments, 0, arguments, 1, mShellCommand.arguments.length);

        mShellCommand.setState(ShellCommand.State.EXECUTING);

        mTerminalSession = new TerminalSession(
            context,
            mShellCommand.executable,
            mShellCommand.workingDirectory,
            arguments,
            mShellCommand.environment.toArray(),
            mShellCommand.stdin, // Nullable
            mShellCommand.terminalTranscriptRows,
            terminalSessionClient
        );

        if (mShellCommand.shellName != null) {
            mTerminalSession.mSessionName = mShellCommand.shellName;
        }
    }

    /**
     * 发出此 {@link TerminalShell} 已完成的信号。当调用方收到 {@link TerminalSessionClient#onSessionFinished(TerminalSession)} 回调时，应调用此方法。
     * <p>
     * 如果进程已完成，则为 {@link #mShellCommand} 设置 {@link ShellCommand#exitCode}，
     * 然后调用 {@link #processTermuxSessionResult(TerminalShell, ShellCommand)} 处理结果。
     */
    public void finish() {
        // 如果进程仍在运行，则忽略此调用
        if (mTerminalSession.isRunning()) return;

        int exitCode = mTerminalSession.getExitStatus();

        // 如果执行命令已经失败，例如发送了 SIGKILL，则不要继续
        if (mShellCommand.isStateFailed()) return;

        mShellCommand.exitCode = exitCode;

        if (!mShellCommand.setState(ShellCommand.State.EXECUTED))
            return;

        processTermuxSessionResult(this, null);
    }

    /**
     * 如果此 {@link TerminalShell} 仍在执行，则通过向其 {@link #mTerminalSession} 发送 {@link OsConstants#SIGILL} 来终止它。
     *
     * @param processResult 如果设置为 {@code true}，则将调用 {@link #processTermuxSessionResult(TerminalShell, ShellCommand)}
     *                      来处理失败。
     */
    public void killIfExecuting(boolean processResult) {
        // 如果执行命令已经完成，则无需处理结果或发送 SIGKILL
        if (mShellCommand.hasExecuted()) return;

        if (mShellCommand.setStateFailed()) {
            if (processResult) {
                mShellCommand.exitCode = 137; // SIGKILL
                processTermuxSessionResult(this, null);
            }
        }

        // 发送 SIGKILL 到进程
        mTerminalSession.finishIfRunning();
    }

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
