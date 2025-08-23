//package com.termux.shared.termux.shell.command.environment;
//
//import android.content.Context;
//import android.content.pm.ApplicationInfo;
//
//import androidx.annotation.NonNull;
//
//import com.termux.shared.android.PackageUtils;
//import com.termux.shared.shell.command.environment.AndroidShellEnvironment;
//import com.termux.shared.termux.TermuxConstants;
//import com.termux.shared.termux.shell.TermuxShellUtils;
//
//import java.util.HashMap;
//
///**
// * Environment for Termux.
// */
//public class TermuxShellEnvironment extends AndroidShellEnvironment {
//
//    public TermuxShellEnvironment() {
//        super();
//        shellCommandShellEnvironment = new HashMap<>();
//    }
//
//    @NonNull
//    @Override
//    public String getDefaultWorkingDirectoryPath() {
//        return TermuxConstants.TERMUX_HOME_DIR_PATH;
//    }
//
//    @NonNull
//    @Override
//    public String getDefaultBinPath() {
//        return TermuxConstants.TERMUX_BIN_PREFIX_DIR_PATH;
//    }
//
//    @NonNull
//    @Override
//    public String[] setupShellCommandArguments(@NonNull String executable, String[] arguments) {
//        return TermuxShellUtils.setupShellCommandArguments(executable, arguments);
//    }
//
//}
