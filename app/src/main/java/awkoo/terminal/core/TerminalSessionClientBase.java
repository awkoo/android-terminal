package awkoo.terminal.core;

import androidx.annotation.NonNull;

public class TerminalSessionClientBase implements TerminalSessionClient {


    @Override
    public void onTextChanged(@NonNull TerminalSession changedSession) {
    }

    @Override
    public void onTitleChanged(@NonNull TerminalSession changedSession) {
    }

    @Override
    public void onSessionFinished(@NonNull TerminalSession finishedSession) {
    }

    @Override
    public void onCopyTextToClipboard(String text) {
    }

    @Override
    public void onPasteTextFromClipboard() {
    }

    @Override
    public void onColorsChanged(@NonNull TerminalSession session) {
    }

    @Override
    public void onTerminalCursorStateChange(boolean state) {
    }

    @Override
    public Integer getTerminalCursorStyle() {
        return null;
    }

}
