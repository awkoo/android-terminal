package com.termux.app;

import android.content.Context;

import com.termux.shared.termux.settings.properties.TermuxAppSharedProperties;

public class Application extends android.app.Application {

    public void onCreate() {
        super.onCreate();

        Context context = getApplicationContext();

        // Init app wide SharedProperties loaded from termux.properties
        TermuxAppSharedProperties.init(context);
    }
}
