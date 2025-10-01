package awkoo.terminal.shell;

public class ShellInfo {

    /*
    The {@link State#SUCCESS} and {@link State#FAILED} is defined based on
    successful execution of command without any internal errors or exceptions being raised.
    The shell command {@link #exitCode} being non-zero **does not** mean that execution command failed.
    Only the {@link #errCode} being non-zero means that execution command failed from the Termux app
    perspective.
    */

    /**
     * The {@link Enum} that defines {@link ShellInfo} state.
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
        APP
    }

    /**
     * The optional unique id for the {@link ShellInfo}. This should equal -1 if execution
     * command is not going to be managed by a shell manager.
     */
    public final int id;

    public int exitCode = -1;

    /**
     * The current state of the {@link ShellInfo}.
     */
    private State currentState = State.PRE_EXECUTION;


    /**
     * The executable for the {@link ShellInfo}.
     */
    public String executable;
    /**
     * The executable arguments array for the {@link ShellInfo}.
     */
    public String[] arguments;
    /**
     * The stdin string for the {@link ShellInfo}.
     */
    public final String stdin;
    /**
     * The current working directory for the {@link ShellInfo}.
     */
    public String workingDirectory;


    /**
     * The terminal transcript rows for the {@link ShellInfo}.
     */
    public Integer terminalTranscriptRows;


    public final Mode mode;


    /**
     * The shell name of commands.
     */
    public String shellName;


    /**
     * The command label for the {@link ShellInfo}.
     */
    public String commandLabel;

    public final ShellEnvironment environment = new ShellEnvironment();


    /**
     * Defines if processing results already called for this {@link ShellInfo}.
     */
    public boolean processingResultsAlreadyCalled;

    public ShellInfo(
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


    public synchronized boolean setStateFailed() {
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
        return currentState == State.FAILED;
    }


}
