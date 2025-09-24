package awkoo.terminal;

import android.content.Context;
import android.content.Intent;

import awkoo.terminal.utils.properties.TermuxAppSharedProperties;

public class Application extends android.app.Application {

    public void onCreate() {
        super.onCreate();

        Context context = getApplicationContext();

        Intent serviceIntent = new Intent(this, TerminalService.class);
        startService(serviceIntent);

        // Init app wide SharedProperties loaded from termux.properties
        TermuxAppSharedProperties.init();
    }
}
