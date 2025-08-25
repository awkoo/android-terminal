package com.termux.shared.shell;

import androidx.annotation.Nullable;

import com.termux.shared.file.FileUtils;
import com.termux.terminal.TerminalBuffer;
import com.termux.terminal.TerminalEmulator;
import com.termux.terminal.TerminalSession;

public class ShellUtils {

    /**
     * Get basename for executable.
     */
    @Nullable
    public static String getExecutableBasename(@Nullable String executable) {
        return FileUtils.getFileBasename(executable);
    }


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
