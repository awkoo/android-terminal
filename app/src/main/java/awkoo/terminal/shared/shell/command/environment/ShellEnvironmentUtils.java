package awkoo.terminal.shared.shell.command.environment;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ShellEnvironmentUtils {

    /**
     * Convert environment {@link HashMap} to `environ` {@link List <String>}.
     * <p>
     * The items in the environ will have the format `name=value`.
     * <p>
     * Check {@link #isValidEnvironmentVariableName(String)} and {@link #isValidEnvironmentVariableValue(String)}
     * for valid variable names and values.
     * <p>
     * https://manpages.debian.org/testing/manpages/environ.7.en.html
     * https://pubs.opengroup.org/onlinepubs/9699919799/basedefs/V1_chap08.html
     */
    @NonNull
    public static List<String> convertEnvironmentToEnviron(@NonNull HashMap<String, String> environmentMap) {
        List<String> environmentList = new ArrayList<>(environmentMap.size());
        String value;
        for (String name : environmentMap.keySet()) {
            value = environmentMap.get(name);
            if (isValidEnvironmentVariableNameValuePair(name, value, true))
                environmentList.add(name + "=" + environmentMap.get(name));
        }
        return environmentList;
    }

    /**
     * Check if environment variable name and value pair is valid. Errors will be logged if
     * {@code logErrors} is {@code true}.
     * <p>
     * Check {@link #isValidEnvironmentVariableName(String)} and {@link #isValidEnvironmentVariableValue(String)}
     * for valid variable names and values.
     */
    public static boolean isValidEnvironmentVariableNameValuePair(@Nullable String name, @Nullable String value, boolean logErrors) {
        return isValidEnvironmentVariableName(name) && isValidEnvironmentVariableValue(value);
    }

    /**
     * Check if environment variable name is valid. It must not be {@code null} and must not contain
     * the null byte ('\0') and must only contain alphanumeric and underscore characters and must not
     * start with a digit.
     */
    public static boolean isValidEnvironmentVariableName(@Nullable String name) {
        return name != null && !name.contains("\0") && name.matches("[a-zA-Z_][a-zA-Z0-9_]*");
    }

    /**
     * Check if environment variable value is valid. It must not be {@code null} and must not contain
     * the null byte ('\0').
     */
    public static boolean isValidEnvironmentVariableValue(@Nullable String value) {
        return value != null && !value.contains("\0");
    }
}
