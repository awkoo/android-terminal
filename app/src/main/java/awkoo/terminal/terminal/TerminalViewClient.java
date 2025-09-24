package awkoo.terminal.terminal;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.media.AudioManager;
import android.view.Gravity;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.widget.EditText;
import android.widget.ListView;

import androidx.drawerlayout.widget.DrawerLayout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import awkoo.terminal.Constants;
import awkoo.terminal.R;
import awkoo.terminal.activities.MainActivity;
import awkoo.terminal.extrakeys.SpecialButton;
import awkoo.terminal.shell.ShellUtils;
import awkoo.terminal.terminal.io.KeyboardShortcut;
import awkoo.terminal.utils.KeyboardUtils;
import awkoo.terminal.utils.data.DataUtils;
import awkoo.terminal.utils.data.UrlUtils;
import awkoo.terminal.utils.interact.ShareUtils;
import awkoo.terminal.utils.properties.TermuxPropertyConstants;
import awkoo.terminal.utils.properties.TermuxSharedProperties;
import awkoo.terminal.view.TerminalView;

public class TerminalViewClient extends TerminalViewClientBase {

    final MainActivity mActivity;

    final TerminalSessionActivityClient mTerminalSessionActivityClient;

    /**
     * 跟踪作为软键盘和其他硬件键的 Ctrl 和 Fn 的特殊键。
     */
    boolean mVirtualControlKeyDown, mVirtualFnKeyDown;

    private Runnable mShowSoftKeyboardRunnable;

    private boolean mShowSoftKeyboardIgnoreOnce;
    private boolean mShowSoftKeyboardWithDelayOnce;

    private boolean mTerminalCursorBlinkerStateAlreadySet;

    private List<KeyboardShortcut> mSessionShortcuts;

    public TerminalViewClient(MainActivity activity, TerminalSessionActivityClient terminalSessionActivityClient) {
        this.mActivity = activity;
        this.mTerminalSessionActivityClient = terminalSessionActivityClient;
    }

    public MainActivity getActivity() {
        return mActivity;
    }

    /**
     * 应在 mActivity.onCreate() 调用时调用
     */
    public void onCreate() {
        onReloadProperties();

        mActivity.getTerminalView().setTextSize(mActivity.getPreferences().getFontSize());
        mActivity.getTerminalView().setKeepScreenOn(mActivity.getPreferences().shouldKeepScreenOn());
    }

    /**
     * 应在 mActivity.onResume() 调用时调用
     */
    public void onResume() {
        // 如果需要，显示软键盘
        setSoftKeyboardState(true, mActivity.isActivityRecreated());

        mTerminalCursorBlinkerStateAlreadySet = false;

        if (mActivity.getTerminalView().mEmulator != null) {
            // 如果启用，开始终端光标闪烁
            // 如果模拟器已设置，则立即开始闪烁，否则等待 onEmulatorSet()
            // 事件启动它。这是必要的，因为在设备显示超时后，通过双击而不是电源按钮启动 MainActivity 后，可能不会调用 onEmulatorSet()。
            setTerminalCursorBlinkerState(true);
            mTerminalCursorBlinkerStateAlreadySet = true;
        }
    }

    /**
     * 应在 mActivity.onStop() 调用时调用
     */
    public void onStop() {
        // 如果启用，停止终端光标闪烁
        setTerminalCursorBlinkerState(false);
    }

    /**
     * 应在 mActivity.reloadProperties() 调用时调用
     */
    public void onReloadProperties() {
        setSessionShortcuts();
    }

    /**
     * 应在 {@link TerminalView#mEmulator} 设置时调用
     */
    @Override
    public void onEmulatorSet() {
        if (!mTerminalCursorBlinkerStateAlreadySet) {
            // 如果启用，开始终端光标闪烁
            // 我们需要等待第一个会话附加（在 MainActivity.onServiceConnected() 中设置），然后是多次调用 TerminalView.updateSize()，
            // 最终在 width/height 不为 0 时设置 mEmulator。否则，如果 MainActivity 在双击返回退出后再次启动，
            // 闪烁器将不会再次启动。请检查 TerminalView.setTerminalCursorBlinkerState()。
            setTerminalCursorBlinkerState(true);
            mTerminalCursorBlinkerStateAlreadySet = true;
        }
    }


    @Override
    public float onScale(float scale) {
        if (scale < 0.9f || scale > 1.1f) {
            boolean increase = scale > 1.f;
            changeFontSize(increase);
            return 1.0f;
        }
        return scale;
    }


    @Override
    public void onSingleTapUp(MotionEvent e) {
        TerminalEmulator term = mActivity.getCurrentSession().getEmulator();

        if (false) {
            int[] columnAndRow = mActivity.getTerminalView().getColumnAndRow(e, true);
            String wordAtTap = term.getScreen().getWordAtLocation(columnAndRow[0], columnAndRow[1]);
            LinkedHashSet<CharSequence> urlSet = UrlUtils.extractUrls(wordAtTap);

            if (!urlSet.isEmpty()) {
                String url = (String) urlSet.iterator().next();
                ShareUtils.openUrl(mActivity, url);
                return;
            }
        }

        if (!term.isMouseTrackingActive() && !e.isFromSource(InputDevice.SOURCE_MOUSE)) {
            if (!KeyboardUtils.areDisableSoftKeyboardFlagsSet(mActivity))
                KeyboardUtils.showSoftKeyboard(mActivity, mActivity.getTerminalView());
        }
    }

    @Override
    public boolean shouldBackButtonBeMappedToEscape() {
        return TermuxSharedProperties.isBackKeyTheEscapeKey();
    }

    @Override
    public boolean shouldEnforceCharBasedInput() {
        return false;
    }

    @Override
    public boolean shouldUseCtrlSpaceWorkaround() {
        return false;
    }

    @Override
    public boolean isTerminalViewSelected() {
        return mActivity.getTerminalToolbarViewPager() == null || mActivity.isTerminalViewSelected() || mActivity.getTerminalView().hasFocus();
    }


    @Override
    public void copyModeChanged(boolean copyMode) {
        // 复制时禁用抽屉。
        mActivity.getDrawer().setDrawerLockMode(copyMode ? DrawerLayout.LOCK_MODE_LOCKED_CLOSED : DrawerLayout.LOCK_MODE_UNLOCKED);
    }


    @SuppressLint("RtlHardcoded")
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent e, TerminalSession currentSession) {
        if (handleVirtualKeys(keyCode, e, true)) return true;

        if (keyCode == KeyEvent.KEYCODE_ENTER && !currentSession.isRunning()) {
            mTerminalSessionActivityClient.removeFinishedSession(currentSession);
            return true;
        } else if (e.isCtrlPressed() && e.isAltPressed()) {
            // 获取未修改的代码点：
            int unicodeChar = e.getUnicodeChar(0);

            if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN || unicodeChar == 'n'/* 下一个 */) {
                mTerminalSessionActivityClient.switchToSession(true);
            } else if (keyCode == KeyEvent.KEYCODE_DPAD_UP || unicodeChar == 'p' /* 上一个 */) {
                mTerminalSessionActivityClient.switchToSession(false);
            } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                mActivity.getDrawer().openDrawer(Gravity.LEFT);
            } else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                mActivity.getDrawer().closeDrawers();
            } else if (unicodeChar == 'k'/* 键盘 */) {
                onToggleSoftKeyboardRequest();
            } else if (unicodeChar == 'm'/* 菜单 */) {
                mActivity.getTerminalView().showContextMenu();
            } else if (unicodeChar == 'r'/* 重命名 */) {
                mTerminalSessionActivityClient.renameSession(currentSession);
            } else if (unicodeChar == 'c'/* 创建 */) {
                mTerminalSessionActivityClient.addNewSession(null);
            } else if (unicodeChar == 'u' /* 网址 */) {
                showUrlSelection();
            } else if (unicodeChar == 'v') {
                doPaste();
            } else if (unicodeChar == '+' || e.getUnicodeChar(KeyEvent.META_SHIFT_ON) == '+') {
                // 我们还在这里检查移位字符，因为可能需要移位才能生成 '+'，
                // 参见 https://github.com/termux/termux-api/issues/2
                changeFontSize(true);
            } else if (unicodeChar == '-') {
                changeFontSize(false);
            } else if (unicodeChar >= '1' && unicodeChar <= '9') {
                int index = unicodeChar - '1';
                mTerminalSessionActivityClient.switchToSession(index);
            }
            return true;
        }

        return false;

    }


    @Override
    public boolean onKeyUp(int keyCode, KeyEvent e) {
        // 如果模拟器未设置，例如引导安装失败且用户关闭了错误对话框，则退出活动，
        // 否则他们将陷入损坏状态。
        if (keyCode == KeyEvent.KEYCODE_BACK && mActivity.getTerminalView().mEmulator == null) {
            mActivity.finishActivityIfNotFinishing();
            return true;
        }

        return handleVirtualKeys(keyCode, e, false);
    }

    /**
     * 如果适用，将专用音量按钮作为虚拟键处理。
     */
    private boolean handleVirtualKeys(int keyCode, KeyEvent event, boolean down) {
        InputDevice inputDevice = event.getDevice();
        if (TermuxSharedProperties.areVirtualVolumeKeysDisabled()) {
            return false;
        } else if (inputDevice != null && inputDevice.getKeyboardType() == InputDevice.KEYBOARD_TYPE_ALPHABETIC) {
            // 不要从完整的外部键盘窃取专用按钮。
            return false;
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            mVirtualControlKeyDown = down;
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            mVirtualFnKeyDown = down;
            return true;
        }
        return false;
    }


    @Override
    public boolean readControlKey() {
        return readExtraKeysSpecialButton(SpecialButton.CTRL) || mVirtualControlKeyDown;
    }

    @Override
    public boolean readAltKey() {
        return readExtraKeysSpecialButton(SpecialButton.ALT);
    }

    @Override
    public boolean readShiftKey() {
        return readExtraKeysSpecialButton(SpecialButton.SHIFT);
    }

    @Override
    public boolean readFnKey() {
        return readExtraKeysSpecialButton(SpecialButton.FN);
    }

    public boolean readExtraKeysSpecialButton(SpecialButton specialButton) {
        if (mActivity.getExtraKeysView() == null) return false;
        Boolean state = mActivity.getExtraKeysView().readSpecialButton(specialButton, true);
        return Objects.requireNonNullElse(state, false);
    }


    @Override
    public boolean onCodePoint(final int codePoint, boolean ctrlDown, TerminalSession session) {
        if (mVirtualFnKeyDown) {
            int resultingKeyCode = -1;
            int resultingCodePoint = -1;
            boolean altDown = false;
            int lowerCase = Character.toLowerCase(codePoint);
            switch (lowerCase) {
                // 方向键。
                case 'w':
                    resultingKeyCode = KeyEvent.KEYCODE_DPAD_UP;
                    break;
                case 'a':
                    resultingKeyCode = KeyEvent.KEYCODE_DPAD_LEFT;
                    break;
                case 's':
                    resultingKeyCode = KeyEvent.KEYCODE_DPAD_DOWN;
                    break;
                case 'd':
                    resultingKeyCode = KeyEvent.KEYCODE_DPAD_RIGHT;
                    break;

                // 向上和向下翻页。
                case 'p':
                    resultingKeyCode = KeyEvent.KEYCODE_PAGE_UP;
                    break;
                case 'n':
                    resultingKeyCode = KeyEvent.KEYCODE_PAGE_DOWN;
                    break;

                // 一些特殊键：
                case 't':
                    resultingKeyCode = KeyEvent.KEYCODE_TAB;
                    break;
                case 'i':
                    resultingKeyCode = KeyEvent.KEYCODE_INSERT;
                    break;
                case 'h':
                    resultingCodePoint = '~';
                    break;

                // 要输入的特殊字符。
                case 'u':
                    resultingCodePoint = '_';
                    break;
                case 'l':
                    resultingCodePoint = '|';
                    break;

                // 功能键。
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                case '8':
                case '9':
                    resultingKeyCode = (codePoint - '1') + KeyEvent.KEYCODE_F1;
                    break;
                case '0':
                    resultingKeyCode = KeyEvent.KEYCODE_F10;
                    break;

                // 其他特殊键。
                case 'e':
                    resultingCodePoint = /*转义*/ 27;
                    break;
                case '.':
                    resultingCodePoint = /*^.*/ 28;
                    break;

                case 'b': // alt+b，在 readline 中向后跳转。
                case 'f': // alt+f，在 readline 中向前跳转。
                case 'x': // alt+x，在 emacs 中常见。
                    resultingCodePoint = lowerCase;
                    altDown = true;
                    break;

                // 音量控制。
                case 'v':
                    AudioManager audio = (AudioManager) mActivity.getSystemService(Context.AUDIO_SERVICE);
                    audio.adjustSuggestedStreamVolume(AudioManager.ADJUST_SAME, AudioManager.USE_DEFAULT_STREAM_TYPE, AudioManager.FLAG_SHOW_UI);
                    break;

                // 写入模式：
                case 'q':
                case 'k':
                    mActivity.toggleTerminalToolbar();
                    mVirtualFnKeyDown = false; // 强制禁用 Fn 键，以恢复键盘输入到终端视图，修复 termux/termux-app#1420
                    break;
            }

            if (resultingKeyCode != -1) {
                TerminalEmulator term = session.getEmulator();
                session.write(KeyHandler.getCode(resultingKeyCode, 0, term.isCursorKeysApplicationMode(), term.isKeypadApplicationMode()));
            } else if (resultingCodePoint != -1) {
                session.writeCodePoint(altDown, resultingCodePoint);
            }
            return true;
        } else if (ctrlDown) {
            if (codePoint == 106 /* Ctrl+j 或 \n */ && !session.isRunning()) {
                mTerminalSessionActivityClient.removeFinishedSession(session);
                return true;
            }

            List<KeyboardShortcut> shortcuts = mSessionShortcuts;
            if (shortcuts != null && !shortcuts.isEmpty()) {
                int codePointLowerCase = Character.toLowerCase(codePoint);
                for (int i = shortcuts.size() - 1; i >= 0; i--) {
                    KeyboardShortcut shortcut = shortcuts.get(i);
                    if (codePointLowerCase == shortcut.codePoint()) {
                        switch (shortcut.shortcutAction()) {
                            case TermuxPropertyConstants.ACTION_SHORTCUT_CREATE_SESSION:
                                mTerminalSessionActivityClient.addNewSession(null);
                                return true;
                            case TermuxPropertyConstants.ACTION_SHORTCUT_NEXT_SESSION:
                                mTerminalSessionActivityClient.switchToSession(true);
                                return true;
                            case TermuxPropertyConstants.ACTION_SHORTCUT_PREVIOUS_SESSION:
                                mTerminalSessionActivityClient.switchToSession(false);
                                return true;
                            case TermuxPropertyConstants.ACTION_SHORTCUT_RENAME_SESSION:
                                mTerminalSessionActivityClient.renameSession(mActivity.getCurrentSession());
                                return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    /**
     * 设置终端会话快捷方式。
     */
    private void setSessionShortcuts() {
        mSessionShortcuts = new ArrayList<>();

        // {@link TermuxPropertyConstants#MAP_SESSION_SHORTCUTS} 存储会话快捷键和操作对
        for (Map.Entry<String, Integer> entry : TermuxPropertyConstants.MAP_SESSION_SHORTCUTS.entrySet()) {
            // mMap 在加载属性时存储会话快捷方式的代码点
            Integer codePoint = (Integer) TermuxSharedProperties.getInternalTermuxPropertyValueFromValue(entry.getKey());
            // 如果 codePoint 为 null，则会话快捷方式在属性中不存在或无效
            // （如由 {@link #getCodePointForSessionShortcuts(String,String)} 解析）
            // 如果 codePoint 不为 null，则获取 MAP_SESSION_SHORTCUTS 键的操作并
            // 将代码点添加到 sessionShortcuts
            if (codePoint != null)
                mSessionShortcuts.add(new KeyboardShortcut(codePoint, entry.getValue()));
        }
    }


    public void changeFontSize(boolean increase) {
        mActivity.getPreferences().changeFontSize(increase);
        mActivity.getTerminalView().setTextSize(mActivity.getPreferences().getFontSize());
    }


    /**
     * 当用户通过抽屉或额外按键中的“键盘”切换按钮，或通过 ctrl+alt+k 硬件键盘快捷键请求切换软键盘时调用。
     */
    public void onToggleSoftKeyboardRequest() {
        // 如果软键盘切换行为是启用/禁用
        if (TermuxSharedProperties.shouldEnableDisableSoftKeyboardOnToggle()) {
            // 如果软键盘可见
            if (!KeyboardUtils.areDisableSoftKeyboardFlagsSet(mActivity)) {
                mActivity.getPreferences().setSoftKeyboardEnabled(false);
                KeyboardUtils.disableSoftKeyboard(mActivity, mActivity.getTerminalView());
            } else {
                // 延迟显示，否则如果用户之前禁用了键盘，切换回另一个应用后按键盘切换将不会显示键盘。
                // 此外，请求焦点，因为如果键盘被禁用，启动时 setSoftKeyboardState 不会请求焦点。#2112
                mActivity.getPreferences().setSoftKeyboardEnabled(true);
                KeyboardUtils.clearDisableSoftKeyboardFlags(mActivity);
                if (mShowSoftKeyboardWithDelayOnce) {
                    mShowSoftKeyboardWithDelayOnce = false;
                    mActivity.getTerminalView().postDelayed(getShowSoftKeyboardRunnable(), 500);
                    mActivity.getTerminalView().requestFocus();
                } else
                    KeyboardUtils.showSoftKeyboard(mActivity, mActivity.getTerminalView());
            }
        }
        // 如果软键盘切换行为是显示/隐藏
        else {
            // 如果用户为 Termux 禁用了软键盘
            if (!mActivity.getPreferences().isSoftKeyboardEnabled()) {
                KeyboardUtils.disableSoftKeyboard(mActivity, mActivity.getTerminalView());
            } else {
                KeyboardUtils.clearDisableSoftKeyboardFlags(mActivity);
                KeyboardUtils.toggleSoftKeyboard(mActivity);
            }
        }
    }

    public void setSoftKeyboardState(boolean isStartup, boolean isReloadTermuxProperties) {
        boolean noShowKeyboard = false;

        // 无论软键盘在启动时是要禁用还是隐藏，请求终端视图焦点都是必要的，
        // 否则，如果连接了硬件键盘，用户在首次点击终端之前开始在硬件键盘上打字，
        // 则终端将添加颜色色调以突出显示焦点视图。请使用浅色主题进行测试。
        // 对于 Android 8.+，TerminalView 布局中的 "defaultFocusHighlightEnabled" 属性也设置为 false 以解决此问题。

        // 如果用户为 Termux 禁用了软键盘（请查看函数文档以获取 Termux 行为信息）
        if (KeyboardUtils.shouldSoftKeyboardBeDisabled(mActivity,
            mActivity.getPreferences().isSoftKeyboardEnabled(),
            mActivity.getPreferences().isSoftKeyboardEnabledOnlyIfNoHardware())) {
            KeyboardUtils.disableSoftKeyboard(mActivity, mActivity.getTerminalView());
            mActivity.getTerminalView().requestFocus();
            noShowKeyboard = true;
            // 仅当像 Termux 应用通过双击返回键退出时调用 onCreate() 时才需要延迟，
            // 而不是当 Termux 应用从另一个应用切换回来并按下键盘切换以启用键盘时
            if (isStartup && mActivity.isOnResumeAfterOnCreate())
                mShowSoftKeyboardWithDelayOnce = true;
        } else {
            // 设置标志以在键盘打开时自动向上推 TerminalView，而不是在其上方显示
            KeyboardUtils.setSoftInputModeAdjustResize(mActivity);

            // 清除任何以前的禁用软键盘标志，以防设置更新
            KeyboardUtils.clearDisableSoftKeyboardFlags(mActivity);

            // 如果软键盘在启动时要隐藏
            if (isStartup && false) {
                // 需要在 Termux 应用从另一个应用切换回来时保持键盘隐藏
                KeyboardUtils.setSoftKeyboardAlwaysHiddenFlags(mActivity);

                KeyboardUtils.hideSoftKeyboard(mActivity, mActivity.getTerminalView());
                mActivity.getTerminalView().requestFocus();
                noShowKeyboard = true;
                // 需要在应用启动时保持键盘隐藏
                mShowSoftKeyboardIgnoreOnce = true;
            }
        }

        mActivity.getTerminalView().setOnFocusChangeListener((view, hasFocus) -> {
            // 如果 TerminalView 或工具栏文本输入视图有焦点，则强制显示软键盘，否则关闭
            boolean textInputViewHasFocus = false;
            final EditText textInputView = mActivity.findViewById(R.id.terminal_toolbar_text_input);
            if (textInputView != null) textInputViewHasFocus = textInputView.hasFocus();

            if (hasFocus || textInputViewHasFocus) {
                if (mShowSoftKeyboardIgnoreOnce) {
                    mShowSoftKeyboardIgnoreOnce = false;
                    return;
                }
            }

            KeyboardUtils.setSoftKeyboardVisibility(getShowSoftKeyboardRunnable(), mActivity, mActivity.getTerminalView(), hasFocus || textInputViewHasFocus);
        });

        // 如果使用硬件键盘运行 termux-reload-settings 命令，或者软键盘要隐藏或已禁用，则不要强制显示软键盘
        if (!isReloadTermuxProperties && !noShowKeyboard) {
            // 请求 TerminalView 焦点
            // 此外，显示键盘，因为如果 TerminalView 在启动时已获得焦点以显示键盘，则不会调用 onFocusChange，
            // 例如通过上下文菜单“选择 URL”长按打开 URL 并通过返回按钮返回 Termux 应用时。这
            // 即使在打开 URL 之前键盘已关闭，也会显示键盘。#2111
            //        logMessage(Log.VERBOSE, tag, message);
            mActivity.getTerminalView().requestFocus();
            mActivity.getTerminalView().postDelayed(getShowSoftKeyboardRunnable(), 300);
        }
    }

    private Runnable getShowSoftKeyboardRunnable() {
        if (mShowSoftKeyboardRunnable == null) {
            mShowSoftKeyboardRunnable = () -> KeyboardUtils.showSoftKeyboard(mActivity, mActivity.getTerminalView());
        }
        return mShowSoftKeyboardRunnable;
    }


    public void setTerminalCursorBlinkerState(boolean start) {
        if (start) {
            // 如果设置/更新光标闪烁速率成功，则启用光标闪烁器
            if (mActivity.getTerminalView().setTerminalCursorBlinkerRate(Constants.DEFAULT_IVALUE_TERMINAL_CURSOR_BLINK_RATE))
                mActivity.getTerminalView().setTerminalCursorBlinkerState(true, true);
        } else {
            // 禁用光标闪烁器
            mActivity.getTerminalView().setTerminalCursorBlinkerState(false, true);
        }
    }


    public void shareSessionTranscript() {
        TerminalSession session = mActivity.getCurrentSession();
        if (session == null) return;

        String transcriptText = ShellUtils.getTerminalSessionTranscriptText(session, false, true);
        if (transcriptText == null) return;

        // 参见 https://github.com/termux/termux-app/issues/1166。
        transcriptText = DataUtils.getTruncatedCommandOutput(transcriptText, DataUtils.TRANSACTION_SIZE_LIMIT_IN_BYTES, false, true, false).trim();
        ShareUtils.shareText(mActivity, mActivity.getString(R.string.title_share_transcript),
            transcriptText, mActivity.getString(R.string.title_share_transcript_with));
    }

    public void shareSelectedText() {
        String selectedText = mActivity.getTerminalView().getStoredSelectedText();
        if (DataUtils.isNullOrEmpty(selectedText)) return;
        ShareUtils.shareText(mActivity, mActivity.getString(R.string.title_share_selected_text),
            selectedText, mActivity.getString(R.string.title_share_selected_text_with));
    }

    public void showUrlSelection() {
        TerminalSession session = mActivity.getCurrentSession();
        if (session == null) return;

        String text = ShellUtils.getTerminalSessionTranscriptText(session, true, true);

        LinkedHashSet<CharSequence> urlSet = UrlUtils.extractUrls(text);
        if (urlSet.isEmpty()) {
            new AlertDialog.Builder(mActivity).setMessage(R.string.title_select_url_none_found).show();
            return;
        }

        final CharSequence[] urls = urlSet.toArray(new CharSequence[0]);
        Collections.reverse(Arrays.asList(urls)); // 最新优先。

        // 点击复制 URL 到剪贴板：
        final AlertDialog dialog = new AlertDialog.Builder(mActivity).setItems(urls, (di, which) -> {
            String url = (String) urls[which];
            ShareUtils.copyTextToClipboard(mActivity, url, mActivity.getString(R.string.msg_select_url_copied_to_clipboard));
        }).setTitle(R.string.title_select_url_dialog).create();

        // 长按打开 URL：
        dialog.setOnShowListener(di -> {
            ListView lv = dialog.getListView(); // 这是一个包含您的“芽”的 ListView
            lv.setOnItemLongClickListener((parent, view, position, id) -> {
                dialog.dismiss();
                String url = (String) urls[position];
                ShareUtils.openUrl(mActivity, url);
                return true;
            });
        });

        dialog.show();
    }

    public void doPaste() {
        TerminalSession session = mActivity.getCurrentSession();
        if (session == null) return;
        if (!session.isRunning()) return;

        String text = ShareUtils.getTextStringFromClipboardIfSet(mActivity, true);
        if (text != null)
            session.getEmulator().paste(text);
    }

}
