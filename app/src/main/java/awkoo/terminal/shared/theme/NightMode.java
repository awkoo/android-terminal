package awkoo.terminal.shared.theme;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;

/**
 * The modes used by to decide night mode for themes.
 */
public enum NightMode {

    /**
     * Night theme should be enabled.
     */
    TRUE("true", AppCompatDelegate.MODE_NIGHT_YES),

    /**
     * Dark theme should be enabled.
     */
    FALSE("false", AppCompatDelegate.MODE_NIGHT_NO),

    /**
     * Use night or dark theme depending on system night mode.
     * https://developer.android.com/guide/topics/resources/providing-resources#NightQualifier
     */
    SYSTEM("system", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);

    /**
     * The current app wide night mode used by various libraries. Defaults to {@link #SYSTEM}.
     */
    private static NightMode APP_NIGHT_MODE;

    private final String name;
    private final @AppCompatDelegate.NightMode int mode;

    NightMode(final String name, int mode) {
        this.name = name;
        this.mode = mode;
    }

    public String getName() {
        return name;
    }

    public int getMode() {
        return mode;
    }


    /**
     * Get {@link #APP_NIGHT_MODE}.
     */
    @NonNull
    public static NightMode getAppNightMode() {
        if (APP_NIGHT_MODE == null)
            APP_NIGHT_MODE = SYSTEM;

        return APP_NIGHT_MODE;
    }

}
