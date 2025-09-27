package awkoo.terminal.shell;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import awkoo.terminal.utils.errors.Error;

public class ResultData implements Serializable {

    /**
     * The stdout of command.
     */
    public final StringBuilder stdout = new StringBuilder();
    /**
     * The exit code of command.
     */
    public Integer exitCode;

    /**
     * The internal errors list of command.
     */
    public List<Error> errorsList = new ArrayList<>();

    public synchronized void setStateFailed(int code, String message) {
        errorsList.add(new Error().setStateFailed(code, message));
    }

    public boolean isStateFailed() {
        if (errorsList != null) {
            for (Error error : errorsList)
                if (error.isStateFailed())
                    return true;
        }

        return false;
    }


}
