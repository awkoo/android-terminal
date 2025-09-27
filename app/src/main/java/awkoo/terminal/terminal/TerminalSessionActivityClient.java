package awkoo.terminal.terminal;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import java.util.Properties;

import awkoo.terminal.R;
import awkoo.terminal.TerminalService;
import awkoo.terminal.activities.MainActivity;
import awkoo.terminal.shell.TerminalShell;
import awkoo.terminal.utils.UI;
import awkoo.terminal.utils.interact.ShareUtils;
import awkoo.terminal.utils.interact.TextInputDialogUtils;

/**
 * {@link TerminalSessionClient} 的实现，可能需要 {@link Activity} 来实现其接口方法。
 */
public class TerminalSessionActivityClient extends TerminalSessionClientBase {

    private final MainActivity mActivity;

    public TerminalSessionActivityClient(MainActivity activity) {
        this.mActivity = activity;
    }

    /**
     * 应在 mActivity.onCreate() 调用时调用
     */
    public void onCreate() {
        // 设置终端字体和颜色
        checkForFontAndColors();
    }

    /**
     * 应在 mActivity.onStart() 调用时调用
     */
    public void onStart() {
        // 服务已连接，但数据可能自上次在前台以来已更改。
        // 获取由 {@link #onStop} 存储在共享首选项中的会话（如果有效），
        // 否则获取当前正在运行的最后一个会话。
        if (mActivity.getTermuxService() != null) {
            setCurrentSession(getCurrentStoredSessionOrLast());
            termuxSessionListNotifyUpdated();
        }

        // 当前终端会话可能在离开期间已更改，强制刷新显示的终端。
        mActivity.getTerminalView().onScreenUpdated();
    }

    /**
     * 应在 mActivity.onStop() 调用时调用
     */
    public void onStop() {
        // 将当前会话存储在共享首选项中，以便以后在 {@link #onStart} 中需要时恢复。
        setCurrentStoredSession();
    }


    @Override
    public void onTextChanged(@NonNull TerminalSession changedSession) {
        if (!mActivity.isVisible()) return;

        if (mActivity.getCurrentSession() == changedSession)
            mActivity.getTerminalView().onScreenUpdated();
    }

    @Override
    public void onTitleChanged(@NonNull TerminalSession updatedSession) {
        if (!mActivity.isVisible()) return;

        if (updatedSession != mActivity.getCurrentSession()) {
            // 仅显示非当前会话的 toast，因为用户
            // 可能有意识地导致当前会话中的标题更改，
            // 并且不希望因此出现烦人的 toast。
            UI.showToast(mActivity, toToastTitle(updatedSession), true);
        }

        termuxSessionListNotifyUpdated();
    }

    @Override
    public void onSessionFinished(@NonNull TerminalSession finishedSession) {
        TerminalService service = mActivity.getTermuxService();

        if (service == null || service.wantsToStop()) {
            // 服务希望尽快停止。
            mActivity.finishActivityIfNotFinishing();
            return;
        }

        int index = service.getIndexOfSession(finishedSession);

        if (mActivity.isVisible() && finishedSession != mActivity.getCurrentSession()) {
            // 显示非当前会话退出时的 toast。
            // 验证会话在我们被告知其结束之前未被删除：
            if (index >= 0)
                UI.showToast(mActivity, toToastTitle(finishedSession) + " - exited", true);
        }

        if (mActivity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_LEANBACK)) {
            // 在 Android TV 设备上，我们需要使用旧的行为，因为我们可能
            // 无法拥有多个启动器图标。
            if (service.getShellList().size() > 1) {
                removeFinishedSession(finishedSession);
            }
        } else {
            // 一旦我们为故障安全会话提供了单独的启动器图标，
            // 退出代码为 '0' 或 '130' 时，自动关闭会话应该安全。
            if (finishedSession.getExitStatus() == 0 || finishedSession.getExitStatus() == 130) {
                removeFinishedSession(finishedSession);
            }
        }
    }

    @Override
    public void onCopyTextToClipboard(String text) {
        if (!mActivity.isVisible()) return;

        ShareUtils.copyTextToClipboard(mActivity, text);
    }

    @Override
    public void onPasteTextFromClipboard() {
        if (!mActivity.isVisible()) return;

        String text = ShareUtils.getTextStringFromClipboardIfSet(mActivity, true);
        if (text != null)
            mActivity.getTerminalView().mEmulator.paste(text);
    }

    @Override
    public void onColorsChanged(@NonNull TerminalSession changedSession) {
        if (mActivity.getCurrentSession() == changedSession)
            updateBackgroundColor();
    }

    @Override
    public void onTerminalCursorStateChange(boolean enabled) {
        // 如果活动不可见，则不启动光标闪烁线程
        if (enabled && !mActivity.isVisible()) {
            return;
        }

        // 如果现在光标已启用，则如果闪烁已启用则启动光标闪烁
        // 否则停止光标闪烁
        mActivity.getTerminalView().setTerminalCursorBlinkerState(enabled, false);
    }


    /**
     * 应在 mActivity.onResetTerminalSession() 调用时调用
     */
    public void onResetTerminalSession() {
        // 确保在重置后重新开始闪烁，如果光标闪烁在重置前被禁用，例如
        // 使用 "tput civis" 会调用 onTerminalCursorStateChange()
        mActivity.getTerminalView().setTerminalCursorBlinkerState(true, true);
    }


    @Override
    public Integer getTerminalCursorStyle() {
        return TerminalEmulator.DEFAULT_TERMINAL_CURSOR_STYLE;
    }


    /**
     * 尝试切换会话。
     */
    public void setCurrentSession(TerminalSession session) {
        if (session == null) return;

        if (mActivity.getTerminalView().attachSession(session)) {
            // 如果尚未显示会话，则通知会话已切换
            notifyOfSessionChange();
        }

        // 即使会话已在显示，我们也会调用以下方法，因为配置可能已过时，
        // 例如未选择或未滚动到当前会话。
        checkAndScrollToSession(session);
        updateBackgroundColor();
    }

    void notifyOfSessionChange() {
        if (!mActivity.isVisible()) return;

        TerminalSession session = mActivity.getCurrentSession();
        UI.showToast(mActivity, toToastTitle(session), false);
    }

    public void switchToSession(boolean forward) {
        TerminalService service = mActivity.getTermuxService();
        if (service == null) return;

        TerminalSession currentTerminalSession = mActivity.getCurrentSession();
        int index = service.getIndexOfSession(currentTerminalSession);
        int size = service.getShellList().size();
        if (forward) {
            if (++index >= size) index = 0;
        } else {
            if (--index < 0) index = size - 1;
        }

        TerminalShell terminalShell = service.getSession(index);
        if (terminalShell != null)
            setCurrentSession(terminalShell.getTerminalSession());
    }

    public void switchToSession(int index) {
        TerminalService service = mActivity.getTermuxService();
        if (service == null) return;

        TerminalShell terminalShell = service.getSession(index);
        if (terminalShell != null)
            setCurrentSession(terminalShell.getTerminalSession());
    }

    public void renameSession(final TerminalSession sessionToRename) {
        if (sessionToRename == null) return;

        TextInputDialogUtils.textInput(
            mActivity,
            R.string.title_rename_session, // 会话重命名标题
            sessionToRename.mSessionName,
            R.string.action_rename_session_confirm, // 确认重命名操作
            text -> {
                renameSession(sessionToRename, text);
                termuxSessionListNotifyUpdated();
            },
            -1,
            null,
            -1,
            null,
            null
        );
    }

    private void renameSession(TerminalSession sessionToRename, String text) {
        if (sessionToRename == null) return;
        sessionToRename.mSessionName = text;
        TerminalService service = mActivity.getTermuxService();
        if (service != null) {
            TerminalShell terminalShell = service.getShellFromSession(sessionToRename);
            if (terminalShell != null)
                terminalShell.getExecutionCommand().shellName = text;
        }
    }

    public void addNewSession(String sessionName) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mActivity);

        TerminalService service = mActivity.getTermuxService();
        if (service == null) return;

        TerminalShell newTerminalShell = service.createSession(
            null,
            null,
            preferences.getString("shell_startup_commands", ""), // 获取shell启动命令
            mActivity.getFilesDir().getAbsolutePath(),
            sessionName,
            preferences.getBoolean("session_with_root", false) // 判断是否以root权限启动会话
        );

        TerminalSession newTerminalSession = newTerminalShell.getTerminalSession();
        setCurrentSession(newTerminalSession);

        mActivity.getDrawer().closeDrawers();
    }

    public void setCurrentStoredSession() {
        TerminalSession currentSession = mActivity.getCurrentSession();
        if (currentSession != null)
            mActivity.getPreferences().setCurrentSession(currentSession.mHandle);
        else
            mActivity.getPreferences().setCurrentSession(null);
    }

    /**
     * 已存储的当前会话，如果不存在则为最后一个会话。
     */
    public TerminalSession getCurrentStoredSessionOrLast() {
        TerminalSession stored = getCurrentStoredSession();

        if (stored != null) {
            // 如果存储的会话在当前运行的会话列表中，则返回它
            return stored;
        } else {
            // 否则返回当前运行的最后一个会话
            TerminalService service = mActivity.getTermuxService();
            if (service == null) return null;

            TerminalShell terminalShell = service.getLastSession();
            if (terminalShell != null)
                return terminalShell.getTerminalSession();
            else
                return null;
        }
    }

    private TerminalSession getCurrentStoredSession() {
        String sessionHandle = mActivity.getPreferences().getCurrentSession();

        // 如果共享首选项中没有存储会话
        if (sessionHandle == null)
            return null;

        // 检查找到的会话句柄是否与当前运行的会话之一匹配
        TerminalService service = mActivity.getTermuxService();
        if (service == null) return null;

        return service.getSessionFromHandle(sessionHandle);
    }

    public void removeFinishedSession(TerminalSession finishedSession) {
        // 按下返回键并完成会话 - 删除它。
        TerminalService service = mActivity.getTermuxService();
        if (service == null) return;

        int index = service.removeSession(finishedSession);

        int size = service.getShellList().size();
        if (size == 0) {
            // 没有会话可显示，因此结束活动。
            mActivity.finishActivityIfNotFinishing();
        } else {
            if (index >= size) {
                index = size - 1;
            }
            TerminalShell terminalShell = service.getSession(index);
            if (terminalShell != null)
                setCurrentSession(terminalShell.getTerminalSession());
        }
    }

    public void termuxSessionListNotifyUpdated() {
        mActivity.termuxSessionListNotifyUpdated();
    }

    public void checkAndScrollToSession(TerminalSession session) {
        if (!mActivity.isVisible()) return;
        TerminalService service = mActivity.getTermuxService();
        if (service == null) return;

        final int indexOfSession = service.getIndexOfSession(session);
        if (indexOfSession < 0) return;

        ListView sessionsList = mActivity.binding.terminalSessionsList;
        sessionsList.setItemChecked(indexOfSession, true);
        // 延迟是必要的，否则有时不会滚动到新添加的会话
        sessionsList.postDelayed(
            () -> sessionsList.smoothScrollToPosition(indexOfSession), 1000
        );
    }


    String toToastTitle(TerminalSession session) {
        TerminalService service = mActivity.getTermuxService();
        if (service == null) return null;

        final int indexOfSession = service.getIndexOfSession(session);
        if (indexOfSession < 0) return null;
        StringBuilder toastTitle = new StringBuilder("[" + (indexOfSession + 1) + "]");
        if (!TextUtils.isEmpty(session.mSessionName)) {
            toastTitle.append(" ").append(session.mSessionName);
        }
        String title = session.getTitle();
        if (!TextUtils.isEmpty(title)) {
            // "[${NR}] 后的空格或会话名称后的换行符：
            toastTitle.append(session.mSessionName == null ? " " : "\n");
            toastTitle.append(title);
        }
        return toastTitle.toString();
    }


    public void checkForFontAndColors() {
        try {
//            File colorsFile = Constants.TERMUX_COLOR_PROPERTIES_FILE; // 颜色文件
//            File fontFile = Constants.TERMUX_FONT_FILE; // 字体文件

            final Properties props = new Properties();
//            if (colorsFile.isFile()) {
//                try (InputStream in = new FileInputStream(colorsFile)) {
//                    props.load(in);
//                }
//            }

            TerminalColors.COLOR_SCHEME.updateWith(props);
            TerminalSession session = mActivity.getCurrentSession();
            if (session != null && session.getEmulator() != null) {
                session.getEmulator().mColors.reset();
            }
            updateBackgroundColor();

//            final Typeface newTypeface = (fontFile.exists() && fontFile.length() > 0) ? Typeface.createFromFile(fontFile) : Typeface.MONOSPACE;
            final Typeface newTypeface = Typeface.MONOSPACE; // 新字体为等宽字体
            mActivity.getTerminalView().setTypeface(newTypeface);
        } catch (Exception ignored) {
        }
    }

    public void updateBackgroundColor() {
        if (!mActivity.isVisible()) return;
        TerminalSession session = mActivity.getCurrentSession();
        if (session != null && session.getEmulator() != null) {
            mActivity
                .getWindow()
                .getDecorView()
                .setBackgroundColor(
                    session
                        .getEmulator()
                        .mColors
                        .mCurrentColors[TextStyle.COLOR_INDEX_BACKGROUND]
                );
        }
    }

}
