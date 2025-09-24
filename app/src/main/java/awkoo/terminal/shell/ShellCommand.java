package awkoo.terminal.shell;

import java.util.List;

public class ShellCommand {

    /*
    The {@link State#SUCCESS} and {@link State#FAILED} is defined based on
    successful execution of command without any internal errors or exceptions being raised.
    The shell command {@link #exitCode} being non-zero **does not** mean that execution command failed.
    Only the {@link #errCode} being non-zero means that execution command failed from the Termux app
    perspective.
    */

    /**
     * The {@link Enum} that defines {@link ShellCommand} state.
     */
    public enum State {

        PRE_EXECUTION("Pre-Execution", 0),
        EXECUTING("Executing", 1),
        EXECUTED("Executed", 2),
        SUCCESS("Success", 3),
        FAILED("Failed", 4);

        private final String name;
        private final int value;

        State(final String name, final int value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public int getValue() {
            return value;
        }


    }

    public enum Mode {
        ROOT,
        APP,
        ADB
    }

    /**
     * The optional unique id for the {@link ShellCommand}. This should equal -1 if execution
     * command is not going to be managed by a shell manager.
     */
    public final Integer id;

    /**
     * The current state of the {@link ShellCommand}.
     */
    private State currentState = State.PRE_EXECUTION;


    /**
     * The executable for the {@link ShellCommand}.
     */
    public String executable;
    /**
     * The executable arguments array for the {@link ShellCommand}.
     */
    public String[] arguments;
    /**
     * The stdin string for the {@link ShellCommand}.
     */
    public final String stdin;
    /**
     * The current working directory for the {@link ShellCommand}.
     */
    public String workingDirectory;


    /**
     * The terminal transcript rows for the {@link ShellCommand}.
     */
    public Integer terminalTranscriptRows;


    public final Mode mode;


    /**
     * The shell name of commands.
     */
    public String shellName;


    /**
     * The command label for the {@link ShellCommand}.
     */
    public String commandLabel;


    /**
     * Defines the {@link ResultData} for the {@link ShellCommand} containing information
     * of the result.
     */
    public final ResultData resultData = new ResultData();


    /**
     * Defines if processing results already called for this {@link ShellCommand}.
     */
    public boolean processingResultsAlreadyCalled;

    public ShellCommand(
        Integer id,
        String executable,
        String[] arguments,
        String stdin,
        String workingDirectory,
        Mode mode
    ) {
        this.id = id;
        this.executable = executable;
        this.arguments = arguments;
        this.stdin = stdin;
        this.workingDirectory = workingDirectory;
        this.mode = mode;
    }


    public synchronized boolean setState(State newState) {
        // The state transition cannot go back or change if already at {@link State#SUCCESS}
        if (newState.getValue() < currentState.getValue() || currentState == State.SUCCESS) {
            return false;
        }

        currentState = newState;
        return true;
    }

    public synchronized boolean hasExecuted() {
        return currentState.getValue() >= State.EXECUTED.getValue();
    }


    public synchronized boolean setStateFailed(int code, String message) {
        return setStateFailed(null, code, message, null);
    }

    public synchronized boolean setStateFailed(String type, int code, String message, List<Throwable> throwablesList) {
        this.resultData.setStateFailed(type, code, message, throwablesList);
        return setState(State.FAILED);
    }

    public synchronized boolean shouldNotProcessResults() {
        if (processingResultsAlreadyCalled) {
            return true;
        } else {
            processingResultsAlreadyCalled = true;
            return false;
        }
    }

    public synchronized boolean isStateFailed() {
        if (currentState != State.FAILED)
            return false;

        return resultData.isStateFailed();
    }


    public String getIdLogString() {
        if (id != null)
            return "(" + id + ") ";
        else
            return "";
    }

    public String getCommandLabelLogString() {
        if (commandLabel != null && !commandLabel.isEmpty())
            return commandLabel;
        else
            return "Execution Command";
    }

    public String getCommandIdAndLabelLogString() {
        return getIdLogString() + getCommandLabelLogString();
    }

}
