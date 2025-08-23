package com.termux.shared.logger;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

public class Logger {

    /**
     * The maximum size of the log entry payload that can be written to the logger. An attempt to
     * write more than this amount will result in a truncated log entry.
     *
     * The limit is 4068 but this includes log tag and log level prefix "D/" before log tag and ": "
     * suffix after it.
     *
     * #define LOGGER_ENTRY_MAX_PAYLOAD 4068
     * https://cs.android.com/android/_/android/platform/system/core/+/android10-release:liblog/include/log/log_read.h;l=127
     */
    public static final int LOGGER_ENTRY_MAX_PAYLOAD = 4068; // 4068 bytes

    /**
     * The maximum safe size of the log entry payload that can be written to the logger, based on
     * {@link #LOGGER_ENTRY_MAX_PAYLOAD}. Using 4000 as a safe limit to give log tag and its
     * prefix/suffix max 68 characters for itself. Use "log*Extended()" functions to use max possible
     * limit if tag is already known.
     */
    public static final int LOGGER_ENTRY_MAX_SAFE_PAYLOAD = 4000; // 4000 bytes

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
            e.printStackTrace();
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

    public static String getStackTracesMarkdownString(String label, String[] stackTraceStringArray) {
        if (label == null) label = "StackTraces";
        StringBuilder stackTracesString = new StringBuilder("### " + label);

        if (stackTraceStringArray == null || stackTraceStringArray.length == 0) {
            stackTracesString.append("\n\n`-`");
        } else {
            for (int i = 0; i != stackTraceStringArray.length; i++) {
                if (stackTraceStringArray.length > 1)
                    stackTracesString.append("\n\n\n#### Stacktrace ").append(i + 1);

                stackTracesString.append("\n\n```\n").append(stackTraceStringArray[i]).append("\n```");
            }
        }

        stackTracesString.append("\n##\n");

        return stackTracesString.toString();
    }

    public static String getSingleLineLogStringEntry(String label, Object object, String def) {
        if (object != null)
            return label + ": `" + object + "`";
        else
            return  label + ": "  +  def;
    }

    public static String getMultiLineLogStringEntry(String label, Object object, String def) {
        if (object != null)
            return label + ":\n```\n" + object + "\n```\n";
        else
            return  label + ": "  +  def;
    }

}
