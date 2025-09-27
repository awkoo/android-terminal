package awkoo.terminal.utils;

import awkoo.terminal.terminal.TerminalBuffer;
import awkoo.terminal.terminal.TerminalEmulator;
import awkoo.terminal.terminal.TerminalSession;

public class ShellUtils {


    /**
     * Get transcript for {@link TerminalSession}.
     */
    public static String getTerminalSessionTranscriptText(TerminalSession terminalSession, boolean linesJoined, boolean trim) {
        if (terminalSession == null) return null;

        TerminalEmulator terminalEmulator = terminalSession.getEmulator();
        if (terminalEmulator == null) return null;

        TerminalBuffer terminalBuffer = terminalEmulator.getScreen();
        if (terminalBuffer == null) return null;

        String transcriptText;

        if (linesJoined)
            transcriptText = terminalBuffer.getTranscriptTextWithFullLinesJoined();
        else
            transcriptText = terminalBuffer.getTranscriptTextWithoutJoinedLines();

        if (trim)
            transcriptText = transcriptText.trim();

        return transcriptText;
    }

}
