package awkoo.terminal.utils.data;

import java.util.LinkedHashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UrlUtils {

    public static Pattern URL_MATCH_REGEX;

    public static Pattern getUrlMatchRegex() {
        if (URL_MATCH_REGEX != null) return URL_MATCH_REGEX;


        String regex_sb = "(" +                       // Begin first matching group.
            "(?:" +                     // Begin scheme group.
            "dav|" +                    // The DAV proto.
            "dict|" +                   // The DICT proto.
            "dns|" +                    // The DNS proto.
            "file|" +                   // File path.
            "finger|" +                 // The Finger proto.
            "ftps?|" +              // The FTP proto.
            "git|" +                    // The Git proto.
            "gemini|" +                 // The Gemini proto.
            "gopher|" +                 // The Gopher proto.
            "https?|" +             // The HTTP proto.
            "imaps?|" +             // The IMAP proto.
            "irc[6s]?|" +           // The IRC proto.
            "ip[fn]s|" +                // The IPFS proto.
            "ldaps?|" +             // The LDAP proto.
            "pop3s?|" +             // The POP3 proto.
            "rediss?|" +            // The Redis proto.
            "rsync|" +                  // The Rsync proto.
            "rtsp[su]?|" +          // The RTSP proto.
            "sftp|" +                   // The SFTP proto.
            "smbs?|" +              // The SAMBA proto.
            "smtps?|" +             // The SMTP proto.
            "svn(?:\\+ssh)?|" +     // The Subversion proto.
            "tcp|" +                    // The TCP proto.
            "telnet|" +                 // The Telnet proto.
            "tftp|" +                   // The TFTP proto.
            "udp|" +                    // The UDP proto.
            "vnc|" +                    // The VNC proto.
            "wss?" +                // The Websocket proto.
            ")://" +                    // End scheme group.
            ")" +                       // End first matching group.


            // Begin second matching group.
            "(" +

            // User name and/or password in format 'user:pass@'.
            "(?:\\S+(?::\\S*)?@)?" +

            // Begin host group.
            "(?:" +

            // IP address (from http://www.regular-expressions.info/examples.html).
            "(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)|" +

            // Host name or domain.
            "(?:[a-z\\u00a1-\\uffff0-9]-*)*[a-z\\u00a1-\\uffff0-9]+(?:(?:\\.(?:[a-z\\u00a1-\\uffff0-9]-*)*[a-z\\u00a1-\\uffff0-9]+)*\\.(?:[a-z\\u00a1-\\uffff0-9]-*)+[a-z\\u00a1-\\uffff0-9]+)?|" +

            // Just path. Used in case of 'file://' scheme.
            "/(?:[a-z\\u00a1-\\uffff0-9]-*)*[a-z\\u00a1-\\uffff0-9]+" +

            // End host group.
            ")" +

            // Port number.
            "(?::\\d{1,5})?" +

            // Resource path with optional query string.
            "(?:/[a-zA-Z0-9:@%\\-._~!$&()*+,;=?/]*)?" +

            // Fragment.
            "(?:#[a-zA-Z0-9:@%\\-._~!$&()*+,;=?/]*)?" +

            // End second matching group.
            ")";

        URL_MATCH_REGEX = Pattern.compile(
            regex_sb,
            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);

        return URL_MATCH_REGEX;
    }

    public static LinkedHashSet<CharSequence> extractUrls(String text) {
        LinkedHashSet<CharSequence> urlSet = new LinkedHashSet<>();
        Matcher matcher = getUrlMatchRegex().matcher(text);

        while (matcher.find()) {
            int matchStart = matcher.start(1);
            int matchEnd = matcher.end();
            String url = text.substring(matchStart, matchEnd);
            urlSet.add(url);
        }

        return urlSet;
    }

}
