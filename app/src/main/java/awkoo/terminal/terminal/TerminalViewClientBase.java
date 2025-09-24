package awkoo.terminal.terminal;

import android.view.KeyEvent;
import android.view.MotionEvent;

public class TerminalViewClientBase {

    public float onScale(float scale) {
        return 1.0f;
    }

    public void onSingleTapUp(MotionEvent e) {
    }

    public boolean shouldBackButtonBeMappedToEscape() {
        return false;
    }

    public boolean shouldEnforceCharBasedInput() {
        return false;
    }

    public boolean shouldUseCtrlSpaceWorkaround() {
        return false;
    }

    public boolean isTerminalViewSelected() {
        return true;
    }

    public void copyModeChanged(boolean copyMode) {
    }

    public boolean onKeyDown(int keyCode, KeyEvent e, TerminalSession session) {
        return false;
    }

    public boolean onKeyUp(int keyCode, KeyEvent e) {
        return false;
    }

    public boolean onLongPress(MotionEvent event) {
        return false;
    }

    public boolean readControlKey() {
        return false;
    }

    public boolean readAltKey() {
        return false;
    }

    public boolean readShiftKey() {
        return false;
    }

    public boolean readFnKey() {
        return false;
    }

    public boolean onCodePoint(int codePoint, boolean ctrlDown, TerminalSession session) {
        return false;
    }

    public void onEmulatorSet() {
    }

}
