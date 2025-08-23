package com.termux.shared.shell.command.environment;

import android.content.Context;

import androidx.annotation.NonNull;

import com.termux.shared.shell.command.ExecutionCommand;

import java.io.File;
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
public class AndroidShellEnvironment extends UnixShellEnvironment {

    protected HashMap<String, String> shellCommandShellEnvironment;

    public AndroidShellEnvironment() {
        shellCommandShellEnvironment = new HashMap<>();
    }

    /** Get shell environment for Android. */
    @NonNull
    @Override
    public HashMap<String, String> getEnvironment(@NonNull Context currentPackageContext, boolean isFailSafe) {
        HashMap<String, String> environment = new HashMap<>();

        environment.put(ENV_HOME, "/");
        environment.put(ENV_LANG, "en_US.UTF-8");
        environment.put(ENV_PATH, System.getenv(ENV_PATH));
        environment.put(ENV_TMPDIR, "/data/local/tmp");

        environment.put(ENV_COLORTERM, "truecolor");
        environment.put(ENV_TERM, "xterm-256color");

        return environment;
    }



    @NonNull
    @Override
    public String getDefaultWorkingDirectoryPath() {
        return "/";
    }


    @NonNull
    @Override
    public String getDefaultBinPath() {
        return "/system/bin";
    }

    @NonNull
    @Override
    public HashMap<String, String> setupShellCommandEnvironment(@NonNull Context currentPackageContext,
                                                                @NonNull ExecutionCommand executionCommand) {
        HashMap<String, String> environment = getEnvironment(currentPackageContext, executionCommand.isFailsafe);

        String workingDirectory = executionCommand.workingDirectory;
        environment.put(ENV_PWD,
            workingDirectory != null && !workingDirectory.isEmpty() ? new File(workingDirectory).getAbsolutePath() : // PWD must be absolute path
            getDefaultWorkingDirectoryPath());
        ShellEnvironmentUtils.createHomeDir(environment);

        return environment;
    }

}
