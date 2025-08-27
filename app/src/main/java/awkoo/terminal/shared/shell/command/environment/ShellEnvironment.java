package awkoo.terminal.shared.shell.command.environment;

import java.util.HashMap;

/**
 * Environment for Android.
 * <p>
 * https://cs.android.com/android/platform/superproject/+/android-12.0.0_r32:frameworks/base/core/java/android/os/Environment.java
 * https://cs.android.com/android/platform/superproject/+/android-12.0.0_r32:system/core/rootdir/init.environ.rc.in
 * https://cs.android.com/android/platform/superproject/+/android-5.0.0_r1.0.1:system/core/rootdir/init.environ.rc.in
 * https://cs.android.com/android/_/android/platform/system/core/+/refs/tags/android-12.0.0_r32:rootdir/init.rc;l=910
 * https://cs.android.com/android/platform/superproject/+/android-12.0.0_r32:packages/modules/SdkExtensions/derive_classpath/derive_classpath.cpp;l=96
 */
public class ShellEnvironment extends HashMap<String, String> {
    /**
     * Names for common/supported login shell binaries.
     */
    public static final String LOGIN_SHELL_NAME = "sh";

    public static final String ENVNAME_PWD = "PWD";
    public static final String ENVNAME_HOME = "HOME";
    public static final String ENVNAME_PATH = "PATH";
    public static final String ENVNAME_TMPDIR = "TMPDIR";

    public static String getDefaultWorkingPath() {
        return "/";
    }

    public static String getDefaultTempPath() {
        return "/";
    }

    public ShellEnvironment() {
        super();
        this.put(ENVNAME_PWD, getDefaultWorkingPath());
        this.put(ENVNAME_HOME, getDefaultWorkingPath());
        this.put(ENVNAME_PATH, System.getenv("PATH"));
        this.put(ENVNAME_TMPDIR, getDefaultTempPath());
        this.put("COLORTERM", "truecolor");
        this.put("TERM", "xterm-256color");
    }

}
