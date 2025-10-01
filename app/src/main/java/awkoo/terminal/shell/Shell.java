package awkoo.terminal.shell;

import android.content.Context;
import android.content.SharedPreferences;
import android.system.OsConstants;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import java.io.File;
import java.util.HashMap;

import awkoo.terminal.core.TerminalSession;
import awkoo.terminal.core.TerminalSessionClient;

/**
 * 一个维护前台 Termux 会话信息的类。它还提供了一种将每个 {@link TerminalSession} 与启动它的 {@link ShellInfo} 相关联的方法。
 */
public class Shell {

    private final TerminalSession mTerminalSession;
    private final ShellInfo mShellInfo;
    private final ExitCallback mExitCallback;

    /**
     * 使用 {@link Runtime#exec(String[], String[], File)} 启动 {@link ShellInfo} 的执行。
     * <p>
     * 必须设置 {@link ShellInfo#executable}，可以可选设置 {@link ShellInfo#commandLabel}、
     * {@link ShellInfo#arguments} 和 {@link ShellInfo#workingDirectory}。
     * <p>
     * 如果 {@link ShellInfo#executable} 为 {@code null}，则会自动选择默认 shell。
     *
     * @param context               用于操作的 {@link Context}。
     * @param shellInfo          包含执行命令信息的 {@link ShellInfo}。
     * @param terminalSessionClient {@link TerminalSessionClient} 接口实现。
     * @param exitCallback   {@link ExitCallback} 接口实现。
     * @param additionalEnvironment 要导出的附加 shell 环境变量。现有变量将被覆盖。
     */
    public Shell(
        @NonNull Context context,
        @NonNull ShellInfo shellInfo,
        @NonNull TerminalSessionClient terminalSessionClient,
        @Nullable ExitCallback exitCallback,
        @Nullable HashMap<String, String> additionalEnvironment
    ) {
        this.mShellInfo = shellInfo;
        this.mExitCallback = exitCallback;

        if (mShellInfo.executable == null || mShellInfo.executable.isEmpty())
            mShellInfo.executable = ShellEnvironment.defaultShell;
        if (mShellInfo.workingDirectory == null || mShellInfo.workingDirectory.isEmpty())
            mShellInfo.workingDirectory = ShellEnvironment.defaultWorkingPath;
        if (mShellInfo.commandLabel == null || mShellInfo.commandLabel.isEmpty())
            mShellInfo.commandLabel = mShellInfo.executable;


        // 设置命令环境
        mShellInfo.environment.put("HOME", context.getFilesDir().getAbsolutePath());
        if (additionalEnvironment != null)
            mShellInfo.environment.putAll(additionalEnvironment);


        // root权限
        if (mShellInfo.mode == ShellInfo.Mode.ROOT) {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
            // 设置命令参数
            String[] args = new String[(mShellInfo.arguments != null ? mShellInfo.arguments.length : 0) + 2];
            args[0] = "-c";
            args[1] = mShellInfo.executable;
            if (mShellInfo.arguments != null && mShellInfo.arguments.length > 0)
                System.arraycopy(mShellInfo.arguments, 0, args, 2, mShellInfo.arguments.length);
            mShellInfo.arguments = args;
            mShellInfo.executable = preferences.getString("su_path", "/system/bin/su");
        }

        // 第一个元素必须为可执行文件
        String[] arguments = new String[(mShellInfo.arguments != null ? mShellInfo.arguments.length : 0) + 1];
        arguments[0] = mShellInfo.executable;
        if (mShellInfo.arguments != null && mShellInfo.arguments.length > 0)
            System.arraycopy(mShellInfo.arguments, 0, arguments, 1, mShellInfo.arguments.length);

        mShellInfo.setState(ShellInfo.State.EXECUTING);

        mTerminalSession = new TerminalSession(
            context,
            mShellInfo.executable,
            mShellInfo.workingDirectory,
            arguments,
            mShellInfo.environment.toArray(),
            mShellInfo.stdin,
            mShellInfo.terminalTranscriptRows,
            terminalSessionClient
        );

        if (mShellInfo.shellName != null) {
            mTerminalSession.mSessionName = mShellInfo.shellName;
        }
    }

    /**
     * 发出此 {@link Shell} 已完成的信号。当调用方收到 {@link TerminalSessionClient#onSessionFinished(TerminalSession)} 回调时，应调用此方法。
     * <p>
     * 如果进程已完成，则为 {@link #mShellInfo} 设置 {@link ShellInfo#exitCode}，
     * 然后调用 {@link #processResult()} 处理结果。
     */
    public void finish() {
        // 如果进程仍在运行，则忽略此调用
        if (mTerminalSession.isRunning()) return;

        int exitCode = mTerminalSession.getExitStatus();

        // 如果执行命令已经失败，例如发送了 SIGKILL，则不要继续
        if (mShellInfo.isStateFailed()) return;

        mShellInfo.exitCode = exitCode;

        if (!mShellInfo.setState(ShellInfo.State.EXECUTED))
            return;

        processResult();
    }

    /**
     * 如果此 {@link Shell} 仍在执行，则通过向其 {@link #mTerminalSession} 发送 {@link OsConstants#SIGILL} 来终止它。
     *
     * @param processResult 如果设置为 {@code true}，则将调用 {@link #processResult()}
     *                      来处理失败。
     */
    public void killIfExecuting(boolean processResult) {
        // 如果执行命令已经完成，则无需处理结果或发送 SIGKILL
        if (mShellInfo.hasExecuted()) return;

        if (mShellInfo.setStateFailed()) {
            if (processResult) {
                mShellInfo.exitCode = 137; // SIGKILL
                processResult();
            }
        }

        // 发送 SIGKILL 到进程
        mTerminalSession.finishIfRunning();
    }

    private void processResult() {
        if (mShellInfo.shouldNotProcessResults()) return;

        if (this.mExitCallback != null) {
            this.mExitCallback.onShellExited(this);
        } else {
            // 如果未设置回调且执行命令未失败，则我们现在设置成功状态
            // 否则，回调宿主可以在完成 Shell 后自行设置
            if (!mShellInfo.isStateFailed())
                mShellInfo.setState(ShellInfo.State.SUCCESS);
        }
    }

    public TerminalSession getTerminalSession() {
        return mTerminalSession;
    }


    public interface ExitCallback {

        /**
         * 当 {@link Shell} 退出时的回调函数。
         *
         * @param shell 退出的 {@link Shell}。
         */
        void onShellExited(Shell shell);

    }

}
