package awkoo.terminal.terminal;

import android.app.Service;

import androidx.annotation.NonNull;

import awkoo.terminal.TerminalService;
import awkoo.terminal.shell.TerminalShell;

/**
 * The {@link TerminalSessionClient} implementation that may require a {@link Service} for its interface methods.
 */
public class TerminalSessionServiceClient extends TerminalSessionClientBase {

    private final TerminalService mService;

    public TerminalSessionServiceClient(TerminalService service) {
        this.mService = service;
    }

    @Override
    public void setTerminalShellPid(@NonNull TerminalSession terminalSession, int pid) {
        TerminalShell terminalShell = mService.getTermuxSessionForTerminalSession(terminalSession);
        if (terminalShell != null)
            terminalShell.getExecutionCommand().mPid = pid;
    }

}
