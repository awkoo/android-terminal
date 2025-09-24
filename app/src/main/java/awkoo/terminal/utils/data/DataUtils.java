package awkoo.terminal.utils.data;

public class DataUtils {

    /**
     * Max safe limit of data size to prevent TransactionTooLargeException when transferring data
     * inside or to other apps via transactions.
     */
    public static final int TRANSACTION_SIZE_LIMIT_IN_BYTES = 100 * 1024; // 100KB

    public static String getTruncatedCommandOutput(String text, int maxLength, boolean fromEnd, boolean onNewline, boolean addPrefix) {
        if (text == null) return null;

        String prefix = "(truncated) ";

        if (addPrefix)
            maxLength = maxLength - prefix.length();

        if (maxLength < 0 || text.length() < maxLength) return text;

        if (fromEnd) {
            text = text.substring(0, maxLength);
        } else {
            int cutOffIndex = text.length() - maxLength;

            if (onNewline) {
                int nextNewlineIndex = text.indexOf('\n', cutOffIndex);
                if (nextNewlineIndex != -1 && nextNewlineIndex != text.length() - 1) {
                    cutOffIndex = nextNewlineIndex + 1;
                }
            }
            text = text.substring(cutOffIndex);
        }

        if (addPrefix)
            text = prefix + text;

        return text;
    }


    /**
     * If value is not in the range [min, max], set it to either min or max.
     */
    public static int clamp(int value, int min, int max) {
        return Math.min(Math.max(value, min), max);
    }


    /**
     * Check if a string is null or empty.
     */
    public static boolean isNullOrEmpty(String string) {
        return string == null || string.isEmpty();
    }


}
