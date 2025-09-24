package awkoo.terminal;

import android.content.Intent;

public class Application extends android.app.Application {

    public void onCreate() {
        super.onCreate();

        Intent serviceIntent = new Intent(this, TerminalService.class);
        startService(serviceIntent);
    }
}
