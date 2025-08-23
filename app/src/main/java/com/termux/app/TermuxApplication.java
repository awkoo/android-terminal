package com.termux.app;

import android.app.Application;
import android.content.Context;

import com.termux.shared.termux.settings.properties.TermuxAppSharedProperties;
import com.termux.shared.termux.shell.TermuxShellManager;

public class TermuxApplication extends Application {

    public void onCreate() {
        super.onCreate();

        Context context = getApplicationContext();

        // Init app wide SharedProperties loaded from termux.properties
        TermuxAppSharedProperties.init(context);

        // Init app wide shell manager
        TermuxShellManager.init(context);
    }
}
