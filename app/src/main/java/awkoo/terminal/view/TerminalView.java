package awkoo.terminal.view;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.ActionMode;
import android.view.HapticFeedbackConstants;
import android.view.InputDevice;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewTreeObserver;
import android.view.accessibility.AccessibilityManager;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.Scroller;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;

import awkoo.terminal.terminal.KeyHandler;
import awkoo.terminal.terminal.TerminalEmulator;
import awkoo.terminal.terminal.TerminalSession;
import awkoo.terminal.terminal.TerminalSessionClient;
import awkoo.terminal.terminal.TerminalViewClientBase;
import awkoo.terminal.view.textselection.TextSelectionCursorController;

/**
 * 显示并与 {@link TerminalSession} 交互的视图。
 */
public final class TerminalView extends View {

    /**
     * 当前显示的终端会话，其模拟器为 {@link #mEmulator}。
     */
    public TerminalSession mTermSession;
    /**
     * 我们的终端模拟器，其会话为 {@link #mTermSession}。
     */
    public TerminalEmulator mEmulator;

    public TerminalRenderer mRenderer;

    public TerminalViewClientBase mClient;

    private TextSelectionCursorController mTextSelectionCursorController;

    private Handler mTerminalCursorBlinkerHandler;
    private TerminalCursorBlinkerRunnable mTerminalCursorBlinkerRunnable;
    private int mTerminalCursorBlinkerRate;
    public static final int TERMINAL_CURSOR_BLINK_RATE_MIN = 100;
    public static final int TERMINAL_CURSOR_BLINK_RATE_MAX = 2000;

    /**
     * 要显示文本的顶行。范围从 -activeTranscriptRows 到 0。
     */
    int mTopRow;
    final int[] mDefaultSelectors = new int[]{-1, -1, -1, -1};

    float mScaleFactor = 1.f;
    final GestureAndScaleRecognizer mGestureRecognizer;

    /**
     * 跟踪鼠标触摸事件的起始位置，我们将其报告为鼠标滚动。
     */
    private int mMouseScrollStartX = -1, mMouseScrollStartY = -1;
    /**
     * 跟踪导致发送鼠标滚动事件的触摸事件的开始时间。
     */
    private long mMouseStartDownTime = -1;

    final Scroller mScroller;

    /**
     * 滚动运动中剩余的部分。
     */
    float mScrollRemainder;

    /**
     * 如果非零，这是收到的最后一个 Unicode 码点（如果它是一个组合字符）。
     */
    int mCombiningAccent;

    private final boolean mAccessibilityEnabled;

    /**
     * {@link KeyEvent} 是从虚拟键盘生成的，例如手动使用 {@link KeyEvent#KeyEvent(int, int)} 构造函数。
     */
    public final static int KEY_EVENT_SOURCE_VIRTUAL_KEYBOARD = KeyCharacterMap.VIRTUAL_KEYBOARD; // -1

    /**
     * {@link KeyEvent} 是从非物理设备生成的，例如如果 {@link KeyEvent#getDeviceId()} 返回 0 值。
     */
    public final static int KEY_EVENT_SOURCE_SOFT_KEYBOARD = 0;

    public TerminalView(Context context, AttributeSet attributes) { // NO_UCD (未使用代码)
        super(context, attributes);
        mGestureRecognizer = new GestureAndScaleRecognizer(context, new GestureAndScaleRecognizer.Listener() {

            boolean scrolledWithFinger;

            @Override
            public void onUp(MotionEvent event) {
                mScrollRemainder = 0.0f;
                if (mEmulator != null && mEmulator.isMouseTrackingActive() && !event.isFromSource(InputDevice.SOURCE_MOUSE) && !isSelectingText() && !scrolledWithFinger) {
                    // 当鼠标跟踪处于活动状态时快速处理事件 - 不要等待检查双击
                    // 进行缩放。
                    sendMouseEventCode(event, TerminalEmulator.MOUSE_LEFT_BUTTON, true);
                    sendMouseEventCode(event, TerminalEmulator.MOUSE_LEFT_BUTTON, false);
                    return;
                }
                scrolledWithFinger = false;
            }

            @Override
            public boolean onSingleTapUp(MotionEvent event) {
                if (mEmulator == null) return true;

                if (isSelectingText()) {
                    stopTextSelectionMode();
                    return true;
                }
                requestFocus();
                mClient.onSingleTapUp(event);
                return true;
            }

            @Override
            public boolean onScroll(MotionEvent e, float distanceX, float distanceY) {
                if (mEmulator == null) return true;
                if (mEmulator.isMouseTrackingActive() && e.isFromSource(InputDevice.SOURCE_MOUSE)) {
                    // 如果在按下按钮的同时移动鼠标指针，则报告该操作而不是滚动。
                    // 这意味着我们永远不会报告触摸输入的带按钮按下事件的移动，
                    // 因为我们不能在没有起始按下事件的情况下开始发送这些事件，
                    // 而我们不会为触摸输入执行此操作，只会在 onTouchEvent() 中处理鼠标。
                    sendMouseEventCode(e, TerminalEmulator.MOUSE_LEFT_BUTTON_MOVED, true);
                } else {
                    scrolledWithFinger = true;
                    distanceY += mScrollRemainder;
                    int deltaRows = (int) (distanceY / mRenderer.mFontLineSpacing);
                    mScrollRemainder = distanceY - deltaRows * mRenderer.mFontLineSpacing;
                    doScroll(e, deltaRows);
                }
                return true;
            }

            @Override
            public boolean onScale(float focusX, float focusY, float scale) {
                if (mEmulator == null || isSelectingText()) return true;
                mScaleFactor *= scale;
                mScaleFactor = mClient.onScale(mScaleFactor);
                return true;
            }

            @Override
            public boolean onFling(final MotionEvent e2, float velocityX, float velocityY) {
                if (mEmulator == null) return true;
                // 在处理完上次滑动之前不要开始滚动：
                if (!mScroller.isFinished()) return true;

                final boolean mouseTrackingAtStartOfFling = mEmulator.isMouseTrackingActive();
                float SCALE = 0.25f;
                if (mouseTrackingAtStartOfFling) {
                    mScroller.fling(0, 0, 0, -(int) (velocityY * SCALE), 0, 0, -mEmulator.mRows / 2, mEmulator.mRows / 2);
                } else {
                    mScroller.fling(0, mTopRow, 0, -(int) (velocityY * SCALE), 0, 0, -mEmulator.getScreen().getActiveTranscriptRows(), 0);
                }

                post(new Runnable() {
                    private int mLastY = 0;

                    @Override
                    public void run() {
                        if (mouseTrackingAtStartOfFling != mEmulator.isMouseTrackingActive()) {
                            mScroller.abortAnimation();
                            return;
                        }
                        if (mScroller.isFinished()) return;
                        boolean more = mScroller.computeScrollOffset();
                        int newY = mScroller.getCurrY();
                        int diff = mouseTrackingAtStartOfFling ? (newY - mLastY) : (newY - mTopRow);
                        doScroll(e2, diff);
                        mLastY = newY;
                        if (more) post(this);
                    }
                });

                return true;
            }

            @Override
            public boolean onDown(float x, float y) {
                // 为什么不在这里返回 true？
                // https://developer.android.com/training/gestures/detector.html#detect-a-subset-of-supported-gestures
                // 尽管将其设置为 true 仍然无法解决在终端视图文本区域长按时出现的以下错误
                // ViewDragHelper: 忽略 pointerId=0，因为在 ACTION_MOVE 之前没有收到此指针的 ACTION_DOWN
                // 注释掉 GestureAndScaleRecognizer#onTouchEvent() 中对 mGestureDetector.onTouchEvent(event) 的调用会删除
                // 错误日志，因此问题与 GestureDetector 有关
                return false;
            }

            @Override
            public boolean onDoubleTap(MotionEvent event) {
                // 不要将其视为一次确认的点击 - 后面可能会跟着缩放。
                return false;
            }

            @Override
            public void onLongPress(MotionEvent event) {
                if (mGestureRecognizer.isInProgress()) return;
                if (mClient.onLongPress(event)) return;
                if (!isSelectingText()) {
                    performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                    startTextSelectionMode(event);
                }
            }
        });
        mScroller = new Scroller(context);
        AccessibilityManager am = (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
        mAccessibilityEnabled = am.isEnabled();
    }


    /**
     * @param client {@link TerminalViewClientBase} 接口实现，允许 {@link TerminalView} 及其客户端之间的通信。
     */
    public void setTerminalViewClient(TerminalViewClientBase client) {
        this.mClient = client;
    }


    /**
     * 将 {@link TerminalSession} 附加到此视图。
     *
     * @param session 此视图将显示的 {@link TerminalSession}。
     */
    public boolean attachSession(TerminalSession session) {
        if (session == mTermSession) return false;
        mTopRow = 0;

        mTermSession = session;
        mEmulator = null;
        mCombiningAccent = 0;

        updateSize();

        // 等待启用滚动条，直到我们有一个终端可以获取滚动位置。
        setVerticalScrollBarEnabled(true);

        return true;
    }

    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        // 确保只有当 TerminalView 是选定的带有键盘的视图且
        // 未选择替代视图（如 EditText）时才设置 inputType。如果 Activity
        // 最初是通过替代视图启动的，或者如果 Activity 从另一个应用程序返回
        // 并且替代视图是上次选定的视图，则这是必需的。
        if (mClient.isTerminalViewSelected()) {
            if (mClient.shouldEnforceCharBasedInput()) {
                // 某些键盘似乎不会在 TYPE_NULL 上重置内部状态。
                // 主要影响三星自带键盘。
                // https://github.com/termux/termux-app/issues/686
                // 但是，根据 AOSP，这不是一个有效值，因为未设置 `InputType.TYPE_CLASS_*`，
                // 并且会记录警告：
                // W/InputAttributes: Unexpected input class: inputType=0x00080090 imeOptions=0x02000000
                // https://cs.android.com/android/platform/superproject/+/android-11.0.0_r40:packages/inputmethods/LatinIME/java/src/com/android/inputmethod/latin/InputAttributes.java;l=79
                outAttrs.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS;
            } else {
                // 使用 InputType.NULL 是最正确的输入类型，并避免了其他 hack 的问题。
                //
                // 以前的键盘问题：
                // https://github.com/termux/termux-packages/issues/25
                // https://github.com/termux/termux-app/issues/87。
                // https://github.com/termux/termux-app/issues/126。
                // https://github.com/termux/termux-app/issues/137 (日文字符和 TYPE_NULL)。
                outAttrs.inputType = InputType.TYPE_NULL;
            }
        }

        // 请注意，不能使用 IME_ACTION_NONE，因为这使得在 Android TV 上使用屏幕键盘输入换行符变得不可能（参见 https://github.com/termux/termux-app/issues/221）。
        outAttrs.imeOptions = EditorInfo.IME_FLAG_NO_FULLSCREEN;

        return new BaseInputConnection(this, true) {

            @Override
            public boolean finishComposingText() {
                super.finishComposingText();

                sendTextToTerminal(Objects.requireNonNull(getEditable()));
                getEditable().clear();
                return true;
            }

            @Override
            public boolean commitText(CharSequence text, int newCursorPosition) {
                super.commitText(text, newCursorPosition);

                if (mEmulator == null) return true;

                Editable content = getEditable();
                sendTextToTerminal(Objects.requireNonNull(content));
                content.clear();
                return true;
            }

            @Override
            public boolean deleteSurroundingText(int leftLength, int rightLength) {
                // 启用了“自动拼写检查”的三星自带键盘会发送 leftLength > 1。
                KeyEvent deleteKey = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL);
                for (int i = 0; i < leftLength; i++) sendKeyEvent(deleteKey);
                return super.deleteSurroundingText(leftLength, rightLength);
            }

            void sendTextToTerminal(CharSequence text) {
                stopTextSelectionMode();
                final int textLengthInChars = text.length();
                for (int i = 0; i < textLengthInChars; i++) {
                    char firstChar = text.charAt(i);
                    int codePoint;
                    if (Character.isHighSurrogate(firstChar)) {
                        if (++i < textLengthInChars) {
                            codePoint = Character.toCodePoint(firstChar, text.charAt(i));
                        } else {
                            // 在字符串末尾，没有紧随高代理项的低代理项：
                            codePoint = TerminalEmulator.UNICODE_REPLACEMENT_CHAR;
                        }
                    } else {
                        codePoint = firstChar;
                    }

                    // 详情请参阅 onKeyDown()。
                    if (mClient.readShiftKey())
                        codePoint = Character.toUpperCase(codePoint);

                    boolean ctrlHeld = false;
                    if (codePoint <= 31 && codePoint != 27) {
                        if (codePoint == '\n') {
                            // AOSP 键盘及其派生品似乎在按下 Enter 键时发送 \n 作为文本，
                            // 而不是像大多数其他键盘应用那样发送按键事件。终端期望 Enter 键为 \r
                            // （尽管在启用 icrnl 时这没有区别 - 运行 'stty -icrnl' 检查行为）。
                            codePoint = '\r';
                        }

                        // 例如，用于 ctrl 输入的 penti 键盘。
                        ctrlHeld = true;
                        switch (codePoint) {
                            case 31:
                                codePoint = '_';
                                break;
                            case 30:
                                codePoint = '^';
                                break;
                            case 29:
                                codePoint = ']';
                                break;
                            case 28:
                                codePoint = '\\';
                                break;
                            default:
                                codePoint += 96;
                                break;
                        }
                    }

                    inputCodePoint(KEY_EVENT_SOURCE_SOFT_KEYBOARD, codePoint, ctrlHeld, false);
                }
            }

        };
    }

    @Override
    protected int computeVerticalScrollRange() {
        return mEmulator == null ? 1 : mEmulator.getScreen().getActiveRows();
    }

    @Override
    protected int computeVerticalScrollExtent() {
        return mEmulator == null ? 1 : mEmulator.mRows;
    }

    @Override
    protected int computeVerticalScrollOffset() {
        return mEmulator == null ? 1 : mEmulator.getScreen().getActiveRows() + mTopRow - mEmulator.mRows;
    }

    public void onScreenUpdated() {
        onScreenUpdated(false);
    }

    public void onScreenUpdated(boolean skipScrolling) {
        if (mEmulator == null) return;

        int rowsInHistory = mEmulator.getScreen().getActiveTranscriptRows();
        if (mTopRow < -rowsInHistory) mTopRow = -rowsInHistory;

        if (isSelectingText() || mEmulator.isAutoScrollDisabled()) {

            // 选择文本时不滚动。
            int rowShift = mEmulator.getScrollCounter();
            if (-mTopRow + rowShift > rowsInHistory) {
                // .. 除非我们达到了历史记录的末尾，在这种情况下
                // 我们中止文本选择并滚动到末尾。
                if (isSelectingText())
                    stopTextSelectionMode();

                if (mEmulator.isAutoScrollDisabled()) {
                    mTopRow = -rowsInHistory;
                    skipScrolling = true;
                }
            } else {
                skipScrolling = true;
                mTopRow -= rowShift;
                decrementYTextSelectionCursors(rowShift);
            }
        }

        if (!skipScrolling && mTopRow != 0) {
            // 如果尚未滚动到底部，则向下滚动。
            if (mTopRow < -3) {
                // 仅在滚动量明显时才唤醒滚动条
                // - 我们不希望在正常每次输入一行时出现可见的滚动条。
                awakenScrollBars();
            }
            mTopRow = 0;
        }

        mEmulator.clearScrollCounter();

        invalidate();
        if (mAccessibilityEnabled) setContentDescription(getText());
    }

    /**
     * 当 {@link TerminalView} 的上下文菜单由
     * {@link TextSelectionCursorController#ACTION_MORE} 启动并关闭时，
     * 托管 Activity 必须在 {@link Activity#onContextMenuClosed(Menu)} 中调用此方法。
     */
    public void onContextMenuClosed() {
        // 取消设置存储的文本，因为它不应再使用，并且应从内存中清除
        unsetStoredSelectedText();
    }

    /**
     * 设置文本大小，这反过来又设置了行数和列数。
     *
     * @param textSize 新的字体大小，以密度无关像素为单位。
     */
    public void setTextSize(int textSize) {
        mRenderer = new TerminalRenderer(textSize, mRenderer == null ? Typeface.MONOSPACE : mRenderer.mTypeface);
        updateSize();
    }

    public void setTypeface(Typeface newTypeface) {
        mRenderer = new TerminalRenderer(mRenderer.mTextSize, newTypeface);
        updateSize();
        invalidate();
    }

    @Override
    public boolean onCheckIsTextEditor() {
        return true;
    }

    @Override
    public boolean isOpaque() {
        return true;
    }

    /**
     * 获取事件位置对应的终端视图的零索引列和行。
     *
     * @param event            带有位置的事件，用于获取列和行。
     * @param relativeToScroll 如果为 true，则列号将考虑滚动
     *                         位置。例如，如果向上滚动 3 行，并且事件
     *                         位置在左上角，如果 relativeToScroll 为 true，则列将为 -3；
     *                         如果 relativeToScroll 为 false，则列将为 0。
     * @return 包含列和行的数组。
     */
    public int[] getColumnAndRow(MotionEvent event, boolean relativeToScroll) {
        int column = (int) (event.getX() / mRenderer.mFontWidth);
        int row = (int) ((event.getY() - mRenderer.mFontLineSpacingAndAscent) / mRenderer.mFontLineSpacing);
        if (relativeToScroll) {
            row += mTopRow;
        }
        return new int[]{column, row};
    }

    /**
     * 向终端发送单个鼠标事件代码。
     */
    void sendMouseEventCode(MotionEvent e, int button, boolean pressed) {
        int[] columnAndRow = getColumnAndRow(e, false);
        int x = columnAndRow[0] + 1;
        int y = columnAndRow[1] + 1;
        if (pressed && (button == TerminalEmulator.MOUSE_WHEELDOWN_BUTTON || button == TerminalEmulator.MOUSE_WHEELUP_BUTTON)) {
            if (mMouseStartDownTime == e.getDownTime()) {
                x = mMouseScrollStartX;
                y = mMouseScrollStartY;
            } else {
                mMouseStartDownTime = e.getDownTime();
                mMouseScrollStartX = x;
                mMouseScrollStartY = y;
            }
        }
        mEmulator.sendMouseEvent(button, x, y, pressed);
    }

    /**
     * 执行滚动，无论是通过拖动屏幕还是通过滚动鼠标滚轮。
     */
    void doScroll(MotionEvent event, int rowsDown) {
        boolean up = rowsDown < 0;
        int amount = Math.abs(rowsDown);
        for (int i = 0; i < amount; i++) {
            if (mEmulator.isMouseTrackingActive()) {
                sendMouseEventCode(event, up ? TerminalEmulator.MOUSE_WHEELUP_BUTTON : TerminalEmulator.MOUSE_WHEELDOWN_BUTTON, true);
            } else if (mEmulator.isAlternateBufferActive()) {
                // 发送上下方向键事件进行滚动，有些终端就是这样做的，以便在
                // 例如 less 中实现滚动功能，less 会切换到备用屏幕而没有鼠标处理。
                handleKeyCode(up ? KeyEvent.KEYCODE_DPAD_UP : KeyEvent.KEYCODE_DPAD_DOWN, 0);
            } else {
                mTopRow = Math.min(0, Math.max(-(mEmulator.getScreen().getActiveTranscriptRows()), mTopRow + (up ? -1 : 1)));
                if (!awakenScrollBars()) invalidate();
            }
        }
    }

    /**
     * 重写 {@link View#onGenericMotionEvent(MotionEvent)}。
     */
    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        if (mEmulator != null && event.isFromSource(InputDevice.SOURCE_MOUSE) && event.getAction() == MotionEvent.ACTION_SCROLL) {
            // 处理鼠标滚轮滚动。
            boolean up = event.getAxisValue(MotionEvent.AXIS_VSCROLL) > 0.0f;
            doScroll(event, up ? -3 : 3);
            return true;
        }
        return false;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mEmulator == null) return true;
        final int action = event.getAction();

        if (isSelectingText()) {
            updateFloatingToolbarVisibility(event);
            mGestureRecognizer.onTouchEvent(event);
            return true;
        } else if (event.isFromSource(InputDevice.SOURCE_MOUSE)) {
            if (event.isButtonPressed(MotionEvent.BUTTON_SECONDARY)) {
                if (action == MotionEvent.ACTION_DOWN) showContextMenu();
                return true;
            } else if (event.isButtonPressed(MotionEvent.BUTTON_TERTIARY)) {
                ClipboardManager clipboardManager = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clipData = clipboardManager.getPrimaryClip();
                if (clipData != null) {
                    ClipData.Item clipItem = clipData.getItemAt(0);
                    if (clipItem != null) {
                        CharSequence text = clipItem.coerceToText(getContext());
                        if (!TextUtils.isEmpty(text)) mEmulator.paste(text.toString());
                    }
                }
            } else if (mEmulator.isMouseTrackingActive()) { // 主要按钮。
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                    case MotionEvent.ACTION_UP:
                        sendMouseEventCode(event, TerminalEmulator.MOUSE_LEFT_BUTTON, event.getAction() == MotionEvent.ACTION_DOWN);
                        break;
                    case MotionEvent.ACTION_MOVE:
                        sendMouseEventCode(event, TerminalEmulator.MOUSE_LEFT_BUTTON_MOVED, true);
                        break;
                }
            }
        }

        mGestureRecognizer.onTouchEvent(event);
        return true;
    }

    @Override
    public boolean onKeyPreIme(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (isSelectingText()) {
                stopTextSelectionMode();
                return true;
            } else if (mClient.shouldBackButtonBeMappedToEscape()) {
                // 拦截返回按钮并将其视为 ESC：
                switch (event.getAction()) {
                    case KeyEvent.ACTION_DOWN:
                        return onKeyDown(keyCode, event);
                    case KeyEvent.ACTION_UP:
                        return onKeyUp(keyCode, event);
                }
            }
        } else if (mClient.shouldUseCtrlSpaceWorkaround() &&
            keyCode == KeyEvent.KEYCODE_SPACE && event.isCtrlPressed()) {
            /* 在某些 ROM 上，如果没有此变通方法，Ctrl+Space 将无法工作。
               但是，这会在那些开箱即用的设备上破坏此功能。 */
            return onKeyDown(keyCode, event);
        }
        return super.onKeyPreIme(keyCode, event);
    }

    /**
     * 软键盘上的按键通常不会触发此侦听器，尽管有些情况下可能会选择这样做。不要依赖此方法捕获软键盘按键。
     * 当 shouldEnforceCharBasedInput() 禁用 (InputType.TYPE_NULL) 时，Gboard 会调用此方法而不是 commitText()，deviceId=-1。
     * 然而，Hacker's Keyboard、OpenBoard、LG Keyboard 会调用 commitText()。
     * <p>
     * 此函数也可以在没有 Android 调用它的情况下直接调用，例如通过 `TerminalExtraKeys` 手动生成一个 KeyEvent，
     * 该事件使用 {@link KeyCharacterMap#VIRTUAL_KEYBOARD} 作为设备 (deviceId=-1)，Gboard 也是如此。
     * 这通常会使用 `/system/usr/keychars/Virtual.kcm` 中定义的映射。您可以运行 `dumpsys input` 来查找
     * 虚拟键盘或硬件键盘使用的 `KeyCharacterMapFile`。请注意，虚拟键盘设备与软键盘（如 Gboard 等）不同。
     * 它是一个用于生成事件和测试的虚拟设备。
     * <p>
     * 我们在 `commitText()` 中处理 Shift 键，通过调用 {@link Character#toUpperCase(int)} 将码点转换为大写，
     * 但在这里我们依赖 getUnicodeChar() 进行 keyCode 转换，这适用于硬件键盘的 Shift 键
     * （通过 effectiveMetaState）和 `mClient.readShiftKey()`，基于 kcm 文件中的值。
     * 这可能会根据键盘和为传递给此函数的事件设置的 Android kcm 文件而导致不同的行为。
     * 这对于非英语语言来说可能是一个问题，因为 `Virtual.kcm` 默认情况下或至少在 AOSP 中只有英语。
     * 对于硬件 Shift 键（通过 effectiveMetaState）和 `mClient.readShiftKey()`，`getUnicodeChar()` 用于 Shift 特定的行为，通常是大写。
     * <p>
     * 对于硬件键盘上的 Fn 键，Android 会检查硬件键盘的 kcm 文件，默认是 `Generic.kcm`，除非定义了供应商特定的文件。
     * 传递的事件将设置 {@link KeyEvent#META_FUNCTION_ON}。如果 kcm 文件只定义了一个字符或 Unicode 码点 `\\uxxxx`，
     * 则只传递一个带有该值的事件。然而，如果 kcm 为 Fn 或其他键定义了 `fallback` 键，例如 `key DPAD_UP { ... fn: fallback PAGE_UP }`，
     * 那么 Android 将首先传递一个带有原始键 `DPAD_UP` 和 {@link KeyEvent#META_FUNCTION_ON} 设置的事件。
     * 但此函数不会消耗它，Android 将传递另一个带有 `PAGE_UP` 且未设置 {@link KeyEvent#META_FUNCTION_ON} 的事件，该事件将被消耗。
     * <p>
     * 现在还有其他一些问题，首先 Ctrl 和 Alt 标志未传递给 `getUnicodeChar()`，因此 kcm 中修改后的键值未被使用。
     * 其次，如果为其他修饰符（如 Shift 或 Fn）的 kcm 文件定义了一个非字母，例如 { fn: '\u0015' } 以充当 DPAD_LEFT，
     * 则 `getUnicodeChar()` 将正确返回 `21` 作为码点，但不会发生操作，因为将 DPAD_LEFT 转换为 `\033[D`
     * 转义序列以使终端执行左操作的 `handleKeyCode()` 函数将不会被调用，因为它在 `getUnicodeChar()` 之前被调用，
     * 终端将改为收到 `21 0x15 Negative Acknowledgement`。
     * 解决此类问题的方法是在调用 `handleKeyCode()` 之前调用 `getUnicodeChar()`，如果用户定义了自定义 kcm 文件，
     * 就像 #2237 中提到的 POC 中所做的那样。请注意，Hacker's Keyboard 会调用 `commitText()`，因此不要用它测试此函数的 Fn/Shift。
     * https://github.com/termux/termux-app/pull/2237
     * https://github.com/agnostic-apollo/termux-app/blob/terminal-code-point-custom-mapping/terminal-view/src/main/java/com/termux/view/TerminalView.java
     * <p>
     * 键字符映射 (kcm) 和键布局 (kl) 文件信息：
     * https://source.android.com/devices/input/key-character-map-files
     * https://source.android.com/devices/input/key-layout-files
     * https://source.android.com/devices/input/keyboard-devices
     * AOSP kcm 和 kl 文件：
     * https://cs.android.com/android/platform/superproject/+/android-11.0.0_r40:frameworks/base/data/keyboards
     * https://cs.android.com/android/platform/superproject/+/android-11.0.0_r40:frameworks/base/packages/InputDevices/res/raw
     * <p>
     * 键码：
     * https://cs.android.com/android/platform/superproject/+/android-11.0.0_r40:frameworks/base/core/java/android/view/KeyEvent.java
     * https://cs.android.com/android/platform/superproject/+/master:frameworks/native/include/android/keycodes.h
     * <p>
     * `dumpsys input`：
     * https://cs.android.com/android/platform/superproject/+/android-11.0.0_r40:frameworks/native/services/inputflinger/reader/EventHub.cpp;l=1917
     * <p>
     * 键盘映射加载：
     * https://cs.android.com/android/platform/superproject/+/android-11.0.0_r40:frameworks/native/services/inputflinger/reader/EventHub.cpp;l=1644
     * https://cs.android.com/android/platform/superproject/+/android-11.0.0_r40:frameworks/native/libs/input/Keyboard.cpp;l=41
     * https://cs.android.com/android/platform/superproject/+/android-11.0.0_r40:frameworks/native/libs/input/InputDevice.cpp
     * 硬件键盘的 OVERLAY 键映射也可以组合：
     * https://cs.android.com/android/platform/superproject/+/android-11.0.0_r40:frameworks/native/libs/input/KeyCharacterMap.cpp;l=165
     * https://cs.android.com/android/platform/superproject/+/android-11.0.0_r40:frameworks/native/libs/input/KeyCharacterMap.cpp;l=831
     * <p>
     * 解析 kcm 文件：
     * https://cs.android.com/android/platform/superproject/+/android-11.0.0_r40:frameworks/native/libs/input/KeyCharacterMap.cpp;l=727
     * 解析键值：
     * https://cs.android.com/android/platform/superproject/+/android-11.0.0_r40:frameworks/native/libs/input/KeyCharacterMap.cpp;l=981
     * <p>
     * `KeyEvent.getUnicodeChar()`
     * https://cs.android.com/android/platform/superproject/+/android-11.0.0_r40:frameworks/base/core/java/android/view/KeyEvent.java;l=2716
     * https://cs.android.com/android/platform/superproject/+/master:frameworks/base/core/java/android/view/KeyCharacterMap.java;l=368
     * https://cs.android.com/android/platform/superproject/+/android-11.0.0_r40:frameworks/base/core/jni/android_view_KeyCharacterMap.cpp;l=117
     * https://cs.android.com/android/platform/superproject/+/android-11.0.0_r40:frameworks/native/libs/input/KeyCharacterMap.cpp;l=231
     * <p>
     * 应用程序宣传的键盘布局，例如通过 #ACTION_QUERY_KEYBOARD_LAYOUTS 用于硬件键盘
     * 配置存储在 `/data/system/input-manager-state.xml` 中
     * https://github.com/ris58h/custom-keyboard-layout
     * 从应用程序加载：
     * https://cs.android.com/android/platform/superproject/+/master:frameworks/base/services/core/java/com/android/server/input/InputManagerService.java;l=1221
     * 设置：
     * https://cs.android.com/android/platform/superproject/+/android-11.0.0_r40:frameworks/base/core/java/android/hardware/input/InputManager.java;l=89
     * https://cs.android.com/android/platform/superproject/+/android-11.0.0_r40:frameworks/base/core/java/android/hardware/input/InputManager.java;l=543
     * https://cs.android.com/android/platform/superproject/+/android-11.0.0_r40:packages/apps/Settings/src/com/android/settings/inputmethod/KeyboardLayoutDialogFragment.java;l=167
     * https://cs.android.com/android/platform/superproject/+/master:frameworks/base/services/core/java/com/android/server/input/InputManagerService.java;l=1385
     * https://cs.android.com/android/platform/superproject/+/master:frameworks/base/services/core/java/com/android/server/input/PersistentDataStore.java
     * 获取叠加键盘布局
     * https://cs.android.com/android/platform/superproject/+/master:frameworks/base/services/core/java/com/android/server/input/InputManagerService.java;l=2158
     * https://cs.android.com/android/platform/superproject/+/android-11.0.0_r40:frameworks/base/services/core/jni/com_android_server_input_InputManagerService.cpp;l=616
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (mEmulator == null) return true;
        if (isSelectingText()) {
            stopTextSelectionMode();
        }

        if (mClient.onKeyDown(keyCode, event, mTermSession)) {
            invalidate();
            return true;
        } else if (event.isSystem() && (!mClient.shouldBackButtonBeMappedToEscape() || keyCode != KeyEvent.KEYCODE_BACK)) {
            return super.onKeyDown(keyCode, event);
        } else if (event.getAction() == KeyEvent.ACTION_MULTIPLE && keyCode == KeyEvent.KEYCODE_UNKNOWN) {
            mTermSession.write(event.getCharacters());
            return true;
        }

        final int metaState = event.getMetaState();
        final boolean controlDown = event.isCtrlPressed() || mClient.readControlKey();
        final boolean leftAltDown = (metaState & KeyEvent.META_ALT_LEFT_ON) != 0 || mClient.readAltKey();
        final boolean shiftDown = event.isShiftPressed() || mClient.readShiftKey();
        final boolean rightAltDownFromEvent = (metaState & KeyEvent.META_ALT_RIGHT_ON) != 0;

        int keyMod = 0;
        if (controlDown) keyMod |= KeyHandler.KEYMOD_CTRL;
        if (event.isAltPressed() || leftAltDown) keyMod |= KeyHandler.KEYMOD_ALT;
        if (shiftDown) keyMod |= KeyHandler.KEYMOD_SHIFT;
        if (event.isNumLockOn()) keyMod |= KeyHandler.KEYMOD_NUM_LOCK;
        // https://github.com/termux/termux-app/issues/731
        if (!event.isFunctionPressed() && handleKeyCode(keyCode, keyMod)) {
            return true;
        }

        // 清除 Ctrl，因为我们自己处理：
        int bitsToClear = KeyEvent.META_CTRL_MASK;
        if (rightAltDownFromEvent) {
            // 允许右 Alt/Alt Gr 用于组合字符。
        }

        // 使用左 Alt 发送给终端（例如，左 Alt+B 跳回一个单词），因此移除：
        bitsToClear |= KeyEvent.META_ALT_ON | KeyEvent.META_ALT_LEFT_ON;

        int effectiveMetaState = event.getMetaState() & ~bitsToClear;

        if (shiftDown) effectiveMetaState |= KeyEvent.META_SHIFT_ON | KeyEvent.META_SHIFT_LEFT_ON;
        if (mClient.readFnKey()) effectiveMetaState |= KeyEvent.META_FUNCTION_ON;

        int result = event.getUnicodeChar(effectiveMetaState);
        if (result == 0) {
            return false;
        }

        int oldCombiningAccent = mCombiningAccent;
        if ((result & KeyCharacterMap.COMBINING_ACCENT) != 0) {
            // 如果之前输入了组合重音符，则将其写入：
            if (mCombiningAccent != 0)
                inputCodePoint(event.getDeviceId(), mCombiningAccent, controlDown, leftAltDown);
            mCombiningAccent = result & KeyCharacterMap.COMBINING_ACCENT_MASK;
        } else {
            if (mCombiningAccent != 0) {
                int combinedChar = KeyCharacterMap.getDeadChar(mCombiningAccent, result);
                if (combinedChar > 0) result = combinedChar;
                mCombiningAccent = 0;
            }
            inputCodePoint(event.getDeviceId(), result, controlDown, leftAltDown);
        }

        if (mCombiningAccent != oldCombiningAccent) invalidate();

        return true;
    }

    public void inputCodePoint(int eventSource, int codePoint, boolean controlDownFromEvent, boolean leftAltDownFromEvent) {

        if (mTermSession == null) return;

        // 确保当按键被按下时（如长按（箭头）键）显示光标
        if (mEmulator != null)
            mEmulator.setCursorBlinkState(true);

        final boolean controlDown = controlDownFromEvent || mClient.readControlKey();
        final boolean altDown = leftAltDownFromEvent || mClient.readAltKey();

        if (mClient.onCodePoint(codePoint, controlDown, mTermSession)) return;

        if (controlDown) {
            if (codePoint >= 'a' && codePoint <= 'z') {
                codePoint = codePoint - 'a' + 1;
            } else if (codePoint >= 'A' && codePoint <= 'Z') {
                codePoint = codePoint - 'A' + 1;
            } else if (codePoint == ' ' || codePoint == '2') {
                codePoint = 0;
            } else if (codePoint == '[' || codePoint == '3') {
                codePoint = 27; // ^[ (Esc)
            } else if (codePoint == '\\' || codePoint == '4') {
                codePoint = 28;
            } else if (codePoint == ']' || codePoint == '5') {
                codePoint = 29;
            } else if (codePoint == '^' || codePoint == '6') {
                codePoint = 30; // control-^
            } else if (codePoint == '_' || codePoint == '7' || codePoint == '/') {
                // "Ctrl-/ 发送 0x1f，相当于 Ctrl-_，自 VT102 时代以来"
                // - http://apple.stackexchange.com/questions/24261/how-do-i-send-c-that-is-control-slash-to-the-terminal
                codePoint = 31;
            } else if (codePoint == '8') {
                codePoint = 127; // DEL
            }
        }

        if (codePoint > -1) {
            // 如果不是虚拟键盘或软键盘。
            if (eventSource > KEY_EVENT_SOURCE_SOFT_KEYBOARD) {
                // 解决蓝牙键盘发送奇怪的 Unicode 字符而不是终端程序期望的更正常的 ASCII 字符的问题 -
                // 输入原始字符的需求应该很低。
                switch (codePoint) {
                    case 0x02DC: // 小波浪号。
                        codePoint = 0x007E; // 波浪号 (~)。
                        break;
                    case 0x02CB: // 修饰字母重音符。
                        codePoint = 0x0060; // 重音符 (`)。
                        break;
                    case 0x02C6: // 修饰字母抑扬符。
                        codePoint = 0x005E; // 抑扬符 (^)。
                        break;
                }
            }

            // 如果是左 Alt 键，则在码点之前发送转义符，以使例如 Alt+B 和 Alt+F 在 readline 中工作：
            mTermSession.writeCodePoint(altDown, codePoint);
        }
    }

    /**
     * 如果适用，输入指定的 keyCode 并返回输入是否已被消费。
     */
    public boolean handleKeyCode(int keyCode, int keyMod) {
        // 确保当按键被按下时（如长按（箭头）键）显示光标
        if (mEmulator != null)
            mEmulator.setCursorBlinkState(true);

        if (handleKeyCodeAction(keyCode, keyMod))
            return true;

        TerminalEmulator term = mTermSession.getEmulator();
        String code = KeyHandler.getCode(keyCode, keyMod, term.isCursorKeysApplicationMode(), term.isKeypadApplicationMode());
        if (code == null) return false;
        mTermSession.write(code);
        return true;
    }

    public boolean handleKeyCodeAction(int keyCode, int keyMod) {
        boolean shiftDown = (keyMod & KeyHandler.KEYMOD_SHIFT) != 0;

        switch (keyCode) {
            case KeyEvent.KEYCODE_PAGE_UP:
            case KeyEvent.KEYCODE_PAGE_DOWN:
                // shift+page_up 和 shift+page_down 应该滚动回滚历史记录，而不是
                // 滚动命令历史记录或更改页面
                if (shiftDown) {
                    long time = SystemClock.uptimeMillis();
                    MotionEvent motionEvent = MotionEvent.obtain(time, time, MotionEvent.ACTION_DOWN, 0, 0, 0);
                    doScroll(motionEvent, keyCode == KeyEvent.KEYCODE_PAGE_UP ? -mEmulator.mRows : mEmulator.mRows);
                    motionEvent.recycle();
                    return true;
                }
        }

        return false;
    }

    /**
     * 当视图中释放按键时调用。
     *
     * @param keyCode 被释放按键的键码。
     * @param event   描述事件的 {@link KeyEvent}。
     * @return 事件是否已处理。
     */
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {

        // 不返回 KEYCODE_BACK 并将其发送给客户端，因为用户可能正在尝试
        // 退出活动。
        if (mEmulator == null && keyCode != KeyEvent.KEYCODE_BACK) return true;

        if (mClient.onKeyUp(keyCode, event)) {
            invalidate();
            return true;
        } else if (event.isSystem()) {
            // 允许系统按键事件通过。
            return super.onKeyUp(keyCode, event);
        }

        return true;
    }

    /**
     * 在布局期间，当此视图的大小发生变化时调用。如果刚刚添加到视图
     * 层次结构中，则使用旧值 0 调用您。
     */
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        updateSize();
    }

    /**
     * 检查终端的行数和列数大小是否应该更新。
     */
    public void updateSize() {
        int viewWidth = getWidth();
        int viewHeight = getHeight();
        if (viewWidth == 0 || viewHeight == 0 || mTermSession == null) return;

        // 如果要启用 vttest，请设置为 80 和 24。
        int newColumns = Math.max(4, (int) (viewWidth / mRenderer.mFontWidth));
        int newRows = Math.max(4, (viewHeight - mRenderer.mFontLineSpacingAndAscent) / mRenderer.mFontLineSpacing);

        if (mEmulator == null || (newColumns != mEmulator.mColumns || newRows != mEmulator.mRows)) {
            mTermSession.updateSize(newColumns, newRows, (int) mRenderer.getFontWidth(), mRenderer.getFontLineSpacing());
            mEmulator = mTermSession.getEmulator();
            mClient.onEmulatorSet();

            // 在会话更改时更新 mTerminalCursorBlinkerRunnable 内部类 mEmulator
            if (mTerminalCursorBlinkerRunnable != null)
                mTerminalCursorBlinkerRunnable.setEmulator(mEmulator);

            mTopRow = 0;
            scrollTo(0, 0);
            invalidate();
        }
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        if (mEmulator == null) {
            canvas.drawColor(0XFF000000);
        } else {
            // 渲染终端视图并突出显示任何选定的文本
            int[] sel = mDefaultSelectors;
            if (mTextSelectionCursorController != null) {
                mTextSelectionCursorController.getSelectors(sel);
            }

            mRenderer.render(mEmulator, canvas, mTopRow, sel[0], sel[1], sel[2], sel[3]);

            // 渲染文本选择手柄
            renderTextSelection();
        }
    }

    public TerminalSession getCurrentSession() {
        return mTermSession;
    }

    private CharSequence getText() {
        return mEmulator.getScreen().getSelectedText(0, mTopRow, mEmulator.mColumns, mTopRow + mEmulator.mRows);
    }

    public int getCursorX(float x) {
        return (int) (x / mRenderer.mFontWidth);
    }

    public int getCursorY(float y) {
        return (int) (((y - 40) / mRenderer.mFontLineSpacing) + mTopRow);
    }

    public int getPointX(int cx) {
        if (cx > mEmulator.mColumns) {
            cx = mEmulator.mColumns;
        }
        return Math.round(cx * mRenderer.mFontWidth);
    }

    public int getPointY(int cy) {
        return (cy - mTopRow) * mRenderer.mFontLineSpacing;
    }

    public int getTopRow() {
        return mTopRow;
    }

    public void setTopRow(int mTopRow) {
        this.mTopRow = mTopRow;
    }


    /**
     * 设置终端光标闪烁速率。它必须介于 {@link #TERMINAL_CURSOR_BLINK_RATE_MIN}
     * 和 {@link #TERMINAL_CURSOR_BLINK_RATE_MAX} 之间，否则将被禁用。
     * <p>
     * 如果不禁用，必须在此之后调用 {@link #setTerminalCursorBlinkerState(boolean, boolean)} 才能使更改生效。
     *
     * @param blinkRate 要设置的值。
     * @return 如果成功设置闪烁速率，则返回 {@code true}，否则返回 {@code false}。
     */
    public synchronized boolean setTerminalCursorBlinkerRate(int blinkRate) {
        boolean result;

        // 如果光标闪烁速率无效
        if (blinkRate != 0 && (blinkRate < TERMINAL_CURSOR_BLINK_RATE_MIN || blinkRate > TERMINAL_CURSOR_BLINK_RATE_MAX)) {
            mTerminalCursorBlinkerRate = 0;
            result = false;
        } else {
            mTerminalCursorBlinkerRate = blinkRate;
            result = true;
        }

        if (mTerminalCursorBlinkerRate == 0) {
            stopTerminalCursorBlinker();
        }

        return result;
    }

    /**
     * 设置是否应启动或停止光标闪烁器。仅当 {@link #mTerminalCursorBlinkerRate} 不等于 0 且介于
     * {@link #TERMINAL_CURSOR_BLINK_RATE_MIN} 和 {@link #TERMINAL_CURSOR_BLINK_RATE_MAX} 之间时，光标闪烁器才会启动。
     * <p>
     * 当持有此活动的视图恢复或停止时应调用此方法，以便在活动不可见时光标闪烁器不会运行。如果在此方法的 onResume() 中调用以启动光标闪烁，
     * 则请确保已设置 {@link #mEmulator}，否则在调用 {@link #attachSession(TerminalSession)} 后等待 {@link TerminalViewClientBase#onEmulatorSet()} 事件，
     * 因为如果未设置 {@link #mEmulator}，例如在双击返回退出活动后再次启动活动，闪烁将不会开始。不要在 {@link #attachSession(TerminalSession)} 之后直接调用此方法，
     * 因为 {@link #updateSize()} 可能会在未设置 {@link #mEmulator} 的情况下返回，因为宽度/高度可能为 0。它会在 {@link #onSizeChanged(int, int, int, int)} 中再次调用。
     * 如果模拟器已设置，则在 onResume() 中调用是必要的，因为在设备显示超时后通过双击而不是电源按钮启动活动后，可能不会调用 onEmulatorSet()。
     * <p>
     * 当光标启用或禁用时，也应在 {@link TerminalSessionClient#onTerminalCursorStateChange(boolean)} 回调中调用此方法，
     * 以便如果不需要显示光标，则禁用闪烁器。在启动闪烁器之前，还应检查活动是否可见。
     * <p>
     * 在使用 {@link TerminalSession#reset()} 重置终端后，也应调用此方法，以防由于调用
     * {@link TerminalSessionClient#onTerminalCursorStateChange(boolean)} 而在重置前禁用了光标闪烁器。
     * <p>
     * 光标闪烁器的工作原理是向应用程序主线程的 Looper 注册一个 {@link Runnable}，当它运行时，
     * 会切换光标闪烁状态，并以 {@link #mTerminalCursorBlinkerRate} 设置的延迟重新注册自身。当需要禁用光标闪烁时，
     * 我们只需取消任何已注册的回调。我们不运行自己的“线程”，而是让主 Looper 的线程为我们完成工作，
     * 因为它的使用也需要更新 UI，因为它还处理基于队列的其他 UI 更新调用。
     * <p>
     * 请注意，在诸如 nano 等文本编辑器中移动光标时，光标状态会快速切换 `-> 关闭 -> 开启`，
     * 这会非常快地顺序调用此方法。因此，如果快速移动光标 2 次或更多次，例如长按箭头键，它将触发
     * `-> 关闭 -> 开启 -> 关闭 -> 开启 -> ...`，并且索引 2 处的“开启”回调会在索引 3 处的下一个“关闭”回调之前自动取消，
     * 从而没有机会运行。我们不会延迟启动闪烁，以便在光标之前不可见时立即显示光标。
     *
     * @param start                    是否应启动或停止光标闪烁器。
     * @param startOnlyIfCursorEnabled 如果设置为 {@code true}，则在启动光标闪烁器之前，还将检查光标是否甚至由 {@link TerminalEmulator} 启用。
     */
    public synchronized void setTerminalCursorBlinkerState(boolean start, boolean startOnlyIfCursorEnabled) {
        // 停止任何现有的光标闪烁器回调
        stopTerminalCursorBlinker();

        if (mEmulator == null) return;

        mEmulator.setCursorBlinkingEnabled(false);

        if (start) {
            // 如果光标闪烁器未启用或无效
            if (mTerminalCursorBlinkerRate < TERMINAL_CURSOR_BLINK_RATE_MIN || mTerminalCursorBlinkerRate > TERMINAL_CURSOR_BLINK_RATE_MAX)
                return;
                // 如果仅当光标启用时才启动光标闪烁器
            else if (startOnlyIfCursorEnabled && !mEmulator.isCursorEnabled()) {
                return;
            }

            // 启动光标闪烁器可运行对象
            if (mTerminalCursorBlinkerHandler == null)
                mTerminalCursorBlinkerHandler = new Handler(Looper.getMainLooper());
            mTerminalCursorBlinkerRunnable = new TerminalCursorBlinkerRunnable(mEmulator, mTerminalCursorBlinkerRate);
            mEmulator.setCursorBlinkingEnabled(true);
            mTerminalCursorBlinkerRunnable.run();
        }
    }

    /**
     * 取消终端光标闪烁器回调
     */
    private void stopTerminalCursorBlinker() {
        if (mTerminalCursorBlinkerHandler != null && mTerminalCursorBlinkerRunnable != null) {
            mTerminalCursorBlinkerHandler.removeCallbacks(mTerminalCursorBlinkerRunnable);
        }
    }

    private class TerminalCursorBlinkerRunnable implements Runnable {

        private TerminalEmulator mEmulator;
        private final int mBlinkRate;

        // 初始化为 false，以便在切换后初始闪烁状态可见
        boolean mCursorVisible = false;

        public TerminalCursorBlinkerRunnable(TerminalEmulator emulator, int blinkRate) {
            mEmulator = emulator;
            mBlinkRate = blinkRate;
        }

        public void setEmulator(TerminalEmulator emulator) {
            mEmulator = emulator;
        }

        public void run() {
            try {
                if (mEmulator != null) {
                    // 切换闪烁状态，然后使视图无效，以便
                    // 调用 onDraw()，然后调用 TerminalRenderer.render()
                    // 它会检查 TerminalEmulator.shouldCursorBeVisible() 以决定是否
                    // 绘制光标
                    mCursorVisible = !mCursorVisible;
                    //mClient.logVerbose(LOG_TAG, "Toggling cursor blink state to " + mCursorVisible); // 切换光标闪烁状态到
                    mEmulator.setCursorBlinkState(mCursorVisible);
                    invalidate();
                }
            } finally {
                // 在 mBlinkRate 毫秒后再次调用 Runnable 以切换闪烁状态
                mTerminalCursorBlinkerHandler.postDelayed(this, mBlinkRate);
            }
        }
    }


    /**
     * 定义文本选择及其手柄所需的函数。
     */
    TextSelectionCursorController getTextSelectionCursorController() {
        if (mTextSelectionCursorController == null) {
            mTextSelectionCursorController = new TextSelectionCursorController(this);

            final ViewTreeObserver observer = getViewTreeObserver();
            if (observer != null) {
                observer.addOnTouchModeChangeListener(mTextSelectionCursorController);
            }
        }

        return mTextSelectionCursorController;
    }

    private void showTextSelectionCursors(MotionEvent event) {
        getTextSelectionCursorController().show(event);
    }

    private boolean hideTextSelectionCursors() {
        return getTextSelectionCursorController().hide();
    }

    private void renderTextSelection() {
        if (mTextSelectionCursorController != null)
            mTextSelectionCursorController.render();
    }

    public boolean isSelectingText() {
        if (mTextSelectionCursorController != null) {
            return mTextSelectionCursorController.isActive();
        }

        return false;

    }

    /**
     * 获取在上下文菜单中按下“更多”按钮之前存储的选定文本。
     */
    @Nullable
    public String getStoredSelectedText() {
        return mTextSelectionCursorController != null ? mTextSelectionCursorController.getStoredSelectedText() : null;
    }

    /**
     * 取消设置在上下文菜单中按下“更多”按钮之前存储的选定文本。
     */
    public void unsetStoredSelectedText() {
        if (mTextSelectionCursorController != null)
            mTextSelectionCursorController.unsetStoredSelectedText();
    }

    private ActionMode getTextSelectionActionMode() {
        if (mTextSelectionCursorController != null) {
            return mTextSelectionCursorController.getActionMode();
        }

        return null;

    }

    public void startTextSelectionMode(MotionEvent event) {
        if (!requestFocus()) {
            return;
        }

        showTextSelectionCursors(event);
        mClient.copyModeChanged(isSelectingText());

        invalidate();
    }

    public void stopTextSelectionMode() {
        if (hideTextSelectionCursors()) {
            mClient.copyModeChanged(isSelectingText());
            invalidate();
        }
    }

    private void decrementYTextSelectionCursors(int decrement) {
        if (mTextSelectionCursorController != null) {
            mTextSelectionCursorController.decrementYTextSelectionCursors(decrement);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        if (mTextSelectionCursorController != null) {
            getViewTreeObserver().addOnTouchModeChangeListener(mTextSelectionCursorController);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        if (mTextSelectionCursorController != null) {
            // 可能会解决以下异常
            // android.view.WindowLeaked: Activity awkoo.terminal.activities.MainActivity has leaked window android.widget.PopupWindow
            stopTextSelectionMode();

            getViewTreeObserver().removeOnTouchModeChangeListener(mTextSelectionCursorController);
        }
    }


    /**
     * 定义长按工具栏所需的函数。
     */
    private final Runnable mShowFloatingToolbar = () -> {
        if (getTextSelectionActionMode() != null) {
            getTextSelectionActionMode().hide(0);  // 隐藏关闭。
        }
    };

    private void showFloatingToolbar() {
        if (getTextSelectionActionMode() != null) {
            int delay = ViewConfiguration.getDoubleTapTimeout();
            postDelayed(mShowFloatingToolbar, delay);
        }
    }

    void hideFloatingToolbar() {
        if (getTextSelectionActionMode() != null) {
            removeCallbacks(mShowFloatingToolbar);
            getTextSelectionActionMode().hide(-1);
        }
    }

    public void updateFloatingToolbarVisibility(MotionEvent event) {
        if (getTextSelectionActionMode() != null) {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_MOVE:
                    hideFloatingToolbar();
                    break;
                case MotionEvent.ACTION_UP:  // 继续执行
                case MotionEvent.ACTION_CANCEL:
                    showFloatingToolbar();
            }
        }
    }

}
