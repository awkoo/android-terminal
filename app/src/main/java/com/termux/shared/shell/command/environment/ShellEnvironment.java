package com.termux.shared.shell.command.environment;

import android.content.Context;

import java.util.HashMap;

/**
 * Environment for Android.
 *
 * https://cs.android.com/android/platform/superproject/+/android-12.0.0_r32:frameworks/base/core/java/android/os/Environment.java
 * https://cs.android.com/android/platform/superproject/+/android-12.0.0_r32:system/core/rootdir/init.environ.rc.in
 * https://cs.android.com/android/platform/superproject/+/android-5.0.0_r1.0.1:system/core/rootdir/init.environ.rc.in
 * https://cs.android.com/android/_/android/platform/system/core/+/refs/tags/android-12.0.0_r32:rootdir/init.rc;l=910
 * https://cs.android.com/android/platform/superproject/+/android-12.0.0_r32:packages/modules/SdkExtensions/derive_classpath/derive_classpath.cpp;l=96
 */
public class ShellEnvironment {
    /** Names for common/supported login shell binaries. */
    public static final String[] LOGIN_SHELL_BINARIES = new String[]{"login", "bash", "zsh", "fish", "sh"};

    public static final String ENVNAME_PWD = "PWD";
    public static final String ENVNAME_HOME = "HOME";
    public static final String ENVNAME_PATH = "PATH";
    public static final String ENVNAME_TMPDIR = "TMPDIR";

    public static String getDefaultWorkingPath(Context context) {
        return context.getFilesDir().getAbsolutePath();
    }

    public static String getDefaultBinPath() {
        return "/system/bin";
    }

    public static String getDefaultTempPath(Context context) {
        return context.getCacheDir().getAbsolutePath();
    }

    public static HashMap<String, String> getDefaultEnvironment(Context context) {
        HashMap<String, String> environment = new HashMap<>();
        environment.put(ENVNAME_PWD, getDefaultWorkingPath(context));
        environment.put(ENVNAME_HOME, getDefaultWorkingPath(context));
        environment.put(ENVNAME_PATH, System.getenv("PATH"));
        environment.put(ENVNAME_TMPDIR, getDefaultTempPath(context));
        environment.put("COLORTERM", "truecolor");
        environment.put("TERM", "xterm-256color");
        return environment;
    }

}
