package awkoo.terminal.terminal;

import androidx.annotation.NonNull;

/**
 * The interface for communication between {@link TerminalSession} and its client. It is used to
 * send callbacks to the client when {@link TerminalSession} changes or for sending other
 * back data to the client like logs.
 */
public interface TerminalSessionClient {

    void onTextChanged(@NonNull TerminalSession changedSession);

    void onTitleChanged(@NonNull TerminalSession changedSession);

    void onSessionFinished(@NonNull TerminalSession finishedSession);

    void onCopyTextToClipboard(String text);

    void onPasteTextFromClipboard();

    void onColorsChanged(@NonNull TerminalSession session);

    void onTerminalCursorStateChange(boolean state);


    Integer getTerminalCursorStyle();

}
