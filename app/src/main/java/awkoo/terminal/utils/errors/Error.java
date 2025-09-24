package awkoo.terminal.utils.errors;

import android.content.Context;

import androidx.annotation.NonNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import awkoo.terminal.utils.UI;
import awkoo.terminal.utils.logger.Logger;

public class Error implements Serializable {

    /**
     * The error type.
     */
    private String type;
    /**
     * The error code.
     */
    private int code;
    /**
     * The error message.
     */
    private String message;
    /**
     * The error exceptions.
     */
    private List<Throwable> throwablesList = new ArrayList<>();


    public Error() {
        InitError(null, null, null, null);
    }

    public Error(String type, Integer code, String message, List<Throwable> throwablesList) {
        InitError(type, code, message, throwablesList);
    }

    public Error(String type, Integer code, String message, Throwable throwable) {
        InitError(type, code, message, Collections.singletonList(throwable));
    }

    public Error(String type, Integer code, String message) {
        InitError(type, code, message, null);
    }

    public Error(Integer code, String message, List<Throwable> throwablesList) {
        InitError(null, code, message, throwablesList);
    }

    public Error(Integer code, String message, Throwable throwable) {
        InitError(null, code, message, Collections.singletonList(throwable));
    }

    public Error(Integer code, String message) {
        InitError(null, code, message, null);
    }

    public Error(String message, Throwable throwable) {
        InitError(null, null, message, Collections.singletonList(throwable));
    }

    public Error(String message, List<Throwable> throwablesList) {
        InitError(null, null, message, throwablesList);
    }

    public Error(String message) {
        InitError(null, null, message, null);
    }

    private void InitError(String type, Integer code, String message, List<Throwable> throwablesList) {
        if (type != null && !type.isEmpty())
            this.type = type;
        else
            this.type = Errno.TYPE;

        if (code != null && code > Errno.ERRNO_SUCCESS.getCode())
            this.code = code;
        else
            this.code = Errno.ERRNO_SUCCESS.getCode();

        this.message = message;

        if (throwablesList != null)
            this.throwablesList = throwablesList;
    }


    public String getType() {
        return type;
    }

    public Integer getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }


    public synchronized boolean setStateFailed(String type, int code, String message, List<Throwable> throwablesList) {
        this.message = message;
        this.throwablesList = throwablesList;

        if (type != null && !type.isEmpty())
            this.type = type;

        if (code > Errno.ERRNO_SUCCESS.getCode()) {
            this.code = code;
            return true;
        } else {
            //        logMessage(Log.WARN, tag, message);
            this.code = Errno.ERRNO_FAILED.getCode();
            return false;
        }
    }

    public boolean isStateFailed() {
        return code > Errno.ERRNO_SUCCESS.getCode();
    }


    @NonNull
    @Override
    public String toString() {
        return getErrorLogString(this);
    }


    public void showToast(Context context) {
        UI.showToast(context, getMinimalErrorLogString(), true);
    }


    /**
     * Get a log friendly {@link String} for {@link Error} error parameters.
     *
     * @param error The {@link Error} to convert.
     * @return Returns the log friendly {@link String}.
     */
    public static String getErrorLogString(final Error error) {
        if (error == null) return "null";
        return error.getErrorLogString();
    }

    public String getErrorLogString() {
        StringBuilder logString = new StringBuilder();

        logString.append(getCodeString());
        logString.append("\n").append(getTypeAndMessageLogString());
        if (throwablesList != null && !throwablesList.isEmpty())
            logString.append("\n").append(geStackTracesLogString());

        return logString.toString();
    }

    public String getMinimalErrorLogString() {
        return getCodeString() +
            getTypeAndMessageLogString();
    }


    public String getCodeString() {
        return Logger.getSingleLineLogStringEntry("Error Code", code, "-");
    }

    public String getTypeAndMessageLogString() {
        return Logger.getMultiLineLogStringEntry(Errno.TYPE.equals(type) ? "Error Message" : "Error Message (" + type + ")", message, "-");
    }

    public String geStackTracesLogString() {
        return Logger.getStackTracesString("StackTraces:", Logger.getStackTracesStringArray(throwablesList));
    }

}
