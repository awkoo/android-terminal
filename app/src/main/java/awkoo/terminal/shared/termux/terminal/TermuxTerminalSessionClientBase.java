package awkoo.terminal.shared.termux.terminal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import awkoo.terminal.terminal.TerminalSession;
import awkoo.terminal.terminal.TerminalSessionClient;

public class TermuxTerminalSessionClientBase implements TerminalSessionClient {


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
    public void onCopyTextToClipboard(@NonNull TerminalSession session, String text) {
    }

    @Override
    public void onPasteTextFromClipboard(@Nullable TerminalSession session) {
    }

    @Override
    public void onBell(@NonNull TerminalSession session) {
    }

    @Override
    public void onColorsChanged(@NonNull TerminalSession session) {
    }

    @Override
    public void onTerminalCursorStateChange(boolean state) {
    }

    @Override
    public void setTerminalShellPid(@NonNull TerminalSession session, int pid) {
    }

    @Override
    public Integer getTerminalCursorStyle() {
        return null;
    }

}
