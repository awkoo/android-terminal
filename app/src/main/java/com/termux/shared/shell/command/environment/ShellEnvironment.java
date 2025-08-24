package com.termux.shared.shell.command.environment;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.termux.shared.shell.ShellUtils;
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
public class ShellEnvironment {

    /** Names for common/supported login shell binaries. */
    public static final String[] LOGIN_SHELL_BINARIES = new String[]{"login", "bash", "zsh", "fish", "sh"};

    /** Get shell environment for Android. */
    @NonNull
    public HashMap<String, String> getEnvironment(@NonNull Context currentPackageContext, boolean isFailSafe) {
        HashMap<String, String> environment = new HashMap<>();

        environment.put("HOME", currentPackageContext.getFilesDir().getAbsolutePath());
        environment.put("LANG", "en_US.UTF-8");
        environment.put("PATH", System.getenv("PATH"));
        environment.put("TMPDIR", currentPackageContext.getCacheDir().getAbsolutePath());
        environment.put("COLORTERM", "truecolor");
        environment.put("TERM", "xterm-256color");

        return environment;
    }



    @NonNull
    public String getDefaultWorkingDirectoryPath(Context context) {
        return context.getFilesDir().getAbsolutePath();
    }


    @NonNull
    public String getDefaultBinPath() {
        return "/system/bin";
    }

    @NonNull
    public String[] setupShellCommandArguments(@NonNull String executable, @Nullable String[] arguments) {
        return ShellUtils.setupShellCommandArguments(executable, arguments);
    }

    @NonNull
    public HashMap<String, String> setupShellCommandEnvironment(@NonNull Context currentPackageContext,
                                                                @NonNull ExecutionCommand executionCommand) {
        HashMap<String, String> environment = getEnvironment(currentPackageContext, executionCommand.isFailsafe);

        String workingDirectory = executionCommand.workingDirectory;
        environment.put("PWD",
            workingDirectory != null && !workingDirectory.isEmpty() ? new File(workingDirectory).getAbsolutePath() : // PWD must be absolute path
            getDefaultWorkingDirectoryPath(currentPackageContext));
//        FileUtils.createDirectoryFile(environment.get("HOME"));

        return environment;
    }

}
