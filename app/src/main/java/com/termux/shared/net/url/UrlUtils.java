package com.termux.shared.net.url;

import androidx.annotation.Nullable;

import com.termux.shared.data.DataUtils;

import java.net.MalformedURLException;
import java.net.URL;

public class UrlUtils {

    /**
     * The parts of a {@link URL}.
     */
    public enum UrlPart {
        AUTHORITY,
        FILE,
        HOST,
        REF,
        FRAGMENT,
        PATH,
        PORT,
        PROTOCOL,
        QUERY,
        USER_INFO
    }

    private static final String LOG_TAG = "UrlUtils";

    /**
     * Join a url base and destination.
     *
     * @param base        The base url to open.
     * @param destination The destination url to open.
     * @param logError    If an error message should be logged.
     * @return Returns the joined {@link String} Url, otherwise {@code null}.
     */
    @Nullable
    public static String joinUrl(final String base, String destination, boolean logError) {
        if (DataUtils.isNullOrEmpty(base)) return null;
        try {
            return new URL(new URL(base), destination).toString();
        } catch (MalformedURLException e) {
            //        logMessage(Log.ERROR, tag, message);
            if (logError)
                e.getMessage();
            return null;
        }
    }

    /**
     * Get {@link URL} from url string.
     *
     * @param urlString The urlString string.
     * @return Returns the {@link URL} if a valid urlString, otherwise {@code null}.
     */
    @Nullable
    public static URL getUrl(String urlString) {
        if (DataUtils.isNullOrEmpty(urlString)) return null;
        try {
            return new URL(urlString);
        } catch (MalformedURLException e) {
            return null;
        }
    }

    /**
     * Get a {@link URL} part from url string.
     *
     * @param urlString The urlString string.
     * @param urlPart   The part to get.
     * @return Returns the {@link URL} part if a valid urlString and part, otherwise {@code null}.
     */
    @Nullable
    public static String getUrlPart(String urlString, UrlPart urlPart) {
        URL url = getUrl(urlString);
        if (url == null) return null;
        return switch (urlPart) {
            case AUTHORITY -> url.getAuthority();
            case FILE -> url.getFile();
            case HOST -> url.getHost();
            case REF, FRAGMENT -> url.getRef();
            case PATH -> url.getPath();
            case PORT -> String.valueOf(url.getPort());
            case PROTOCOL -> url.getProtocol();
            case QUERY -> url.getQuery();
            case USER_INFO -> url.getUserInfo();
            default -> null;
        };
    }

    /**
     * Remove "https://www.", "https://", "www.", etc
     */
    public static String removeProtocol(String urlString) {
        if (urlString == null) return null;
        return urlString.replaceFirst("^(http[s]?://www\\.|http[s]?://|www\\.)", "");
    }

    public static boolean areUrlsEqual(String url1, String url2) {
        if (url1 == null && url2 == null) return true;
        if (url1 == null || url2 == null) return false;
        return UrlUtils.removeProtocol(url1).replaceAll("/+$", "").equals(UrlUtils.removeProtocol(url2).replaceAll("/+$", ""));
    }

}
