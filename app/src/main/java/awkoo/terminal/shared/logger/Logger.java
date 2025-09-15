package awkoo.terminal.shared.logger;

import android.util.Log;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Objects;

public class Logger {


    public static String getMessageAndStackTraceString(String message, Throwable throwable) {
        if (message == null && throwable == null)
            return null;
        else if (message != null && throwable != null)
            return message + ":\n" + getStackTraceString(throwable);
        else if (throwable == null)
            return message;
        else
            return getStackTraceString(throwable);
    }


    public static String getStackTraceString(Throwable throwable) {
        if (throwable == null) return null;

        String stackTraceString = null;

        try {
            StringWriter errors = new StringWriter();
            PrintWriter pw = new PrintWriter(errors);
            throwable.printStackTrace(pw);
            pw.close();
            stackTraceString = errors.toString();
            errors.close();
        } catch (IOException e) {
            Log.e("", Objects.requireNonNull(e.getMessage()));
        }

        return stackTraceString;
    }


    public static String[] getStackTracesStringArray(List<Throwable> throwablesList) {
        if (throwablesList == null) return null;
        final String[] stackTraceStringArray = new String[throwablesList.size()];
        for (int i = 0; i < throwablesList.size(); i++) {
            stackTraceStringArray[i] = getStackTraceString(throwablesList.get(i));
        }
        return stackTraceStringArray;
    }


    public static String getStackTracesString(String label, String[] stackTraceStringArray) {
        if (label == null) label = "StackTraces:";
        StringBuilder stackTracesString = new StringBuilder(label);

        if (stackTraceStringArray == null || stackTraceStringArray.length == 0) {
            stackTracesString.append(" -");
        } else {
            for (int i = 0; i != stackTraceStringArray.length; i++) {
                if (stackTraceStringArray.length > 1)
                    stackTracesString.append("\n\nStacktrace ").append(i + 1);

                stackTracesString.append("\n```\n").append(stackTraceStringArray[i]).append("\n```\n");
            }
        }

        return stackTracesString.toString();
    }

    public static String getSingleLineLogStringEntry(String label, Object object, String def) {
        if (object != null)
            return label + ": `" + object + "`";
        else
            return label + ": " + def;
    }

    public static String getMultiLineLogStringEntry(String label, Object object, String def) {
        if (object != null)
            return label + ":\n```\n" + object + "\n```\n";
        else
            return label + ": " + def;
    }

}
