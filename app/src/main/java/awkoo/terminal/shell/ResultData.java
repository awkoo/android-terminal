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
     * The stderr of command.
     */
    public final StringBuilder stderr = new StringBuilder();
    /**
     * The exit code of command.
     */
    public Integer exitCode;

    /**
     * The internal errors list of command.
     */
    public List<Error> errorsList = new ArrayList<>();

    public synchronized void setStateFailed(String type, int code, String message, List<Throwable> throwablesList) {
        if (errorsList == null)
            errorsList = new ArrayList<>();

        Error error = new Error();
        errorsList.add(error);

        error.setStateFailed(type, code, message, throwablesList);
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
