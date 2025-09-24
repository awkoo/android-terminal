package awkoo.terminal.utils.properties;

import com.google.common.collect.ImmutableBiMap;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import awkoo.terminal.Constants;
import awkoo.terminal.terminal.TerminalEmulator;


public final class TermuxPropertyConstants {

    /* boolean */

    /**
     * Defines the key for whether hardware keyboard shortcuts are enabled.
     */
    public static final String KEY_DISABLE_HARDWARE_KEYBOARD_SHORTCUTS = "disable-hardware-keyboard-shortcuts"; // Default: "disable-hardware-keyboard-shortcuts"


    /**
     * Defines the key for whether a toast will be shown when user changes the terminal session
     */
    public static final String KEY_DISABLE_TERMINAL_SESSION_CHANGE_TOAST = "disable-terminal-session-change-toast"; // Default: "disable-terminal-session-change-toast"


    /**
     * Defines the key for whether to enforce character based input to fix the issue where for some devices like Samsung, the letters might not appear until enter is pressed
     */
    public static final String KEY_ENFORCE_CHAR_BASED_INPUT = "enforce-char-based-input"; // Default: "enforce-char-based-input"


    /**
     * Defines the key for whether text for the extra keys buttons should be all capitalized automatically
     */
    public static final String KEY_EXTRA_KEYS_TEXT_ALL_CAPS = "extra-keys-text-all-caps"; // Default: "extra-keys-text-all-caps"


    /**
     * Defines the key for whether to hide soft keyboard when termux app is started
     */
    public static final String KEY_HIDE_SOFT_KEYBOARD_ON_STARTUP = "hide-soft-keyboard-on-startup"; // Default: "hide-soft-keyboard-on-startup"


    /**
     * Defines the key for whether url links in terminal transcript will automatically open on click or on tap
     */
    public static final String KEY_TERMINAL_ONCLICK_URL_OPEN = "terminal-onclick-url-open"; // Default: "terminal-onclick-url-open"


    /**
     * Defines the key for whether to use ctrl space workaround to fix the issue where ctrl+space does not work on some ROMs
     */
    public static final String KEY_USE_CTRL_SPACE_WORKAROUND = "ctrl-space-workaround"; // Default: "ctrl-space-workaround"


    /**
     * Defines the key for whether to use fullscreen
     */
    public static final String KEY_USE_FULLSCREEN = "fullscreen"; // Default: "fullscreen"


    /**
     * Defines the key for whether to use fullscreen workaround
     */
    public static final String KEY_USE_FULLSCREEN_WORKAROUND = "use-fullscreen-workaround"; // Default: "use-fullscreen-workaround"





    /* int */


    /**
     * Defines the key for the terminal cursor blink rate
     */
    public static final String KEY_TERMINAL_CURSOR_BLINK_RATE = "terminal-cursor-blink-rate"; // Default: "terminal-cursor-blink-rate"
    public static final int DEFAULT_IVALUE_TERMINAL_CURSOR_BLINK_RATE = 0;


    /**
     * Defines the key for the terminal cursor style
     */
    public static final String KEY_TERMINAL_CURSOR_STYLE = "terminal-cursor-style"; // Default: "terminal-cursor-style"

    public static final int DEFAULT_IVALUE_TERMINAL_CURSOR_STYLE = TerminalEmulator.DEFAULT_TERMINAL_CURSOR_STYLE;


    /**
     * Defines the key for the terminal transcript rows
     */
    public static final String KEY_TERMINAL_TRANSCRIPT_ROWS = "terminal-transcript-rows"; // Default: "terminal-transcript-rows"
    public static final int DEFAULT_IVALUE_TERMINAL_TRANSCRIPT_ROWS = TerminalEmulator.DEFAULT_TERMINAL_TRANSCRIPT_ROWS;





    /* float */

    /**
     * Defines the key for the terminal toolbar height
     */
    public static final String KEY_TERMINAL_TOOLBAR_HEIGHT_SCALE_FACTOR = "terminal-toolbar-height"; // Default: "terminal-toolbar-height"
    public static final float DEFAULT_IVALUE_TERMINAL_TOOLBAR_HEIGHT_SCALE_FACTOR = 1;





    /* Integer */

    /**
     * Defines the key for create session shortcut
     */
    public static final String KEY_SHORTCUT_CREATE_SESSION = "shortcut.create-session"; // Default: "shortcut.create-session"
    /**
     * Defines the key for next session shortcut
     */
    public static final String KEY_SHORTCUT_NEXT_SESSION = "shortcut.next-session"; // Default: "shortcut.next-session"
    /**
     * Defines the key for previous session shortcut
     */
    public static final String KEY_SHORTCUT_PREVIOUS_SESSION = "shortcut.previous-session"; // Default: "shortcut.previous-session"
    /**
     * Defines the key for rename session shortcut
     */
    public static final String KEY_SHORTCUT_RENAME_SESSION = "shortcut.rename-session"; // Default: "shortcut.rename-session"

    public static final int ACTION_SHORTCUT_CREATE_SESSION = 1;
    public static final int ACTION_SHORTCUT_NEXT_SESSION = 2;
    public static final int ACTION_SHORTCUT_PREVIOUS_SESSION = 3;
    public static final int ACTION_SHORTCUT_RENAME_SESSION = 4;

    /**
     * Defines the bidirectional map for session shortcut values and their internal actions
     */
    public static final ImmutableBiMap<String, Integer> MAP_SESSION_SHORTCUTS =
        new ImmutableBiMap.Builder<String, Integer>()
            .put(KEY_SHORTCUT_CREATE_SESSION, ACTION_SHORTCUT_CREATE_SESSION)
            .put(KEY_SHORTCUT_NEXT_SESSION, ACTION_SHORTCUT_NEXT_SESSION)
            .put(KEY_SHORTCUT_PREVIOUS_SESSION, ACTION_SHORTCUT_PREVIOUS_SESSION)
            .put(KEY_SHORTCUT_RENAME_SESSION, ACTION_SHORTCUT_RENAME_SESSION)
            .build();





    /* String */

    /**
     * Defines the key for whether back key will behave as escape key or literal back key
     */
    public static final String KEY_BACK_KEY_BEHAVIOUR = "back-key"; // Default: "back-key"

    public static final String IVALUE_BACK_KEY_BEHAVIOUR_BACK = "back";
    public static final String IVALUE_BACK_KEY_BEHAVIOUR_ESCAPE = "escape";
    public static final String DEFAULT_IVALUE_BACK_KEY_BEHAVIOUR = IVALUE_BACK_KEY_BEHAVIOUR_BACK;


    /**
     * Defines the key for extra keys
     */
    public static final String KEY_EXTRA_KEYS = "extra-keys"; // Default: "extra-keys"
    //public static final String DEFAULT_IVALUE_EXTRA_KEYS = "[[ESC, TAB, CTRL, ALT, {key: '-', popup: '|'}, DOWN, UP]]"; // Single row
    public static final String DEFAULT_IVALUE_EXTRA_KEYS = "[['ESC',{key: 'DRAWER', popup: 'PASTE'},'SCROLL','HOME','UP','END','PGUP'], ['TAB','CTRL','ALT','LEFT','DOWN','RIGHT','PGDN']]"; // Double row

    /**
     * Defines the key for extra keys style
     */
    public static final String KEY_EXTRA_KEYS_STYLE = "extra-keys-style"; // Default: "extra-keys-style"
    public static final String DEFAULT_IVALUE_EXTRA_KEYS_STYLE = "default";


    /**
     * Defines the key for whether toggle soft keyboard request will show/hide or enable/disable keyboard
     */
    public static final String KEY_SOFT_KEYBOARD_TOGGLE_BEHAVIOUR = "soft-keyboard-toggle-behaviour"; // Default: "soft-keyboard-toggle-behaviour"

    public static final String IVALUE_SOFT_KEYBOARD_TOGGLE_BEHAVIOUR_SHOW_HIDE = "show/hide";
    public static final String IVALUE_SOFT_KEYBOARD_TOGGLE_BEHAVIOUR_ENABLE_DISABLE = "enable/disable";
    public static final String DEFAULT_IVALUE_SOFT_KEYBOARD_TOGGLE_BEHAVIOUR = IVALUE_SOFT_KEYBOARD_TOGGLE_BEHAVIOUR_SHOW_HIDE;


    /**
     * Defines the key for whether volume keys will behave as virtual or literal volume keys
     */
    public static final String KEY_VOLUME_KEYS_BEHAVIOUR = "volume-keys"; // Default: "volume-keys"

    public static final String IVALUE_VOLUME_KEY_BEHAVIOUR_VIRTUAL = "virtual";
    public static final String IVALUE_VOLUME_KEY_BEHAVIOUR_VOLUME = "volume";
    public static final String DEFAULT_IVALUE_VOLUME_KEYS_BEHAVIOUR = IVALUE_VOLUME_KEY_BEHAVIOUR_VIRTUAL;


    /**
     * Defines the set for keys loaded by termux
     * Setting this to {@code null} will make {@link SharedProperties} throw an exception.
     */
    public static final Set<String> TERMUX_APP_PROPERTIES_LIST = new HashSet<>(Arrays.asList(
        /* boolean */
        KEY_DISABLE_HARDWARE_KEYBOARD_SHORTCUTS,
        KEY_DISABLE_TERMINAL_SESSION_CHANGE_TOAST,
        KEY_ENFORCE_CHAR_BASED_INPUT,
        KEY_EXTRA_KEYS_TEXT_ALL_CAPS,
        KEY_HIDE_SOFT_KEYBOARD_ON_STARTUP,
        KEY_TERMINAL_ONCLICK_URL_OPEN,
        KEY_USE_CTRL_SPACE_WORKAROUND,
        KEY_USE_FULLSCREEN,
        KEY_USE_FULLSCREEN_WORKAROUND,
        Constants.PROP_ALLOW_EXTERNAL_APPS,

        /* int */
        KEY_TERMINAL_CURSOR_BLINK_RATE,
        KEY_TERMINAL_CURSOR_STYLE,
        KEY_TERMINAL_TRANSCRIPT_ROWS,

        /* float */
        KEY_TERMINAL_TOOLBAR_HEIGHT_SCALE_FACTOR,

        /* Integer */
        KEY_SHORTCUT_CREATE_SESSION,
        KEY_SHORTCUT_NEXT_SESSION,
        KEY_SHORTCUT_PREVIOUS_SESSION,
        KEY_SHORTCUT_RENAME_SESSION,

        /* String */
        KEY_BACK_KEY_BEHAVIOUR,
        KEY_EXTRA_KEYS,
        KEY_EXTRA_KEYS_STYLE,
        KEY_SOFT_KEYBOARD_TOGGLE_BEHAVIOUR,
        KEY_VOLUME_KEYS_BEHAVIOUR
    ));

    /**
     * Defines the set for keys loaded by termux that have default boolean behaviour with false as default.
     * "true" -> true
     * "false" -> false
     * default: false
     */
    public static final Set<String> TERMUX_DEFAULT_FALSE_BOOLEAN_BEHAVIOUR_PROPERTIES_LIST = new HashSet<>(Arrays.asList(
        KEY_DISABLE_HARDWARE_KEYBOARD_SHORTCUTS,
        KEY_DISABLE_TERMINAL_SESSION_CHANGE_TOAST,
        KEY_ENFORCE_CHAR_BASED_INPUT,
        KEY_HIDE_SOFT_KEYBOARD_ON_STARTUP,
        KEY_TERMINAL_ONCLICK_URL_OPEN,
        KEY_USE_CTRL_SPACE_WORKAROUND,
        KEY_USE_FULLSCREEN,
        KEY_USE_FULLSCREEN_WORKAROUND,
        Constants.PROP_ALLOW_EXTERNAL_APPS
    ));

    /**
     * Defines the set for keys loaded by termux that have default boolean behaviour with true as default.
     * "true" -> true
     * "false" -> false
     * default: true
     */
    public static final Set<String> TERMUX_DEFAULT_TRUE_BOOLEAN_BEHAVIOUR_PROPERTIES_LIST = new HashSet<>(Arrays.asList(
        KEY_EXTRA_KEYS_TEXT_ALL_CAPS
    ));

}
