package awkoo.terminal.app.terminal;

import android.app.Service;

import androidx.annotation.NonNull;

import awkoo.terminal.app.TerminalService;
import awkoo.terminal.shared.termux.shell.TerminalShell;
import awkoo.terminal.shared.termux.terminal.TermuxTerminalSessionClientBase;
import awkoo.terminal.terminal.TerminalSession;
import awkoo.terminal.terminal.TerminalSessionClient;

/**
 * The {@link TerminalSessionClient} implementation that may require a {@link Service} for its interface methods.
 */
public class TermuxTerminalSessionServiceClient extends TermuxTerminalSessionClientBase {

    private final TerminalService mService;

    public TermuxTerminalSessionServiceClient(TerminalService service) {
        this.mService = service;
    }

    @Override
    public void setTerminalShellPid(@NonNull TerminalSession terminalSession, int pid) {
        TerminalShell terminalShell = mService.getTermuxSessionForTerminalSession(terminalSession);
        if (terminalShell != null)
            terminalShell.getExecutionCommand().mPid = pid;
    }

}
