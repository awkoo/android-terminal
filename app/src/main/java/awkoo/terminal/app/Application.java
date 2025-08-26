package awkoo.terminal.app;

import android.content.Context;

import awkoo.terminal.shared.termux.settings.properties.TermuxAppSharedProperties;

public class Application extends android.app.Application {

    public void onCreate() {
        super.onCreate();

        Context context = getApplicationContext();

        // Init app wide SharedProperties loaded from termux.properties
        TermuxAppSharedProperties.init(context);
    }
}
