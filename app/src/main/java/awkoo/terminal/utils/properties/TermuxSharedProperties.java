package awkoo.terminal.utils.properties;

import androidx.annotation.NonNull;

import java.util.Set;

public abstract class TermuxSharedProperties {

    protected final String mLabel;
    protected final Set<String> mPropertiesList;
    protected final SharedPropertiesParser mSharedPropertiesParser;
    protected SharedProperties mSharedProperties;

    public TermuxSharedProperties(@NonNull String label,
                                  @NonNull Set<String> propertiesList,
                                  @NonNull SharedPropertiesParser sharedPropertiesParser) {
        mLabel = label;
        mPropertiesList = propertiesList;
        mSharedPropertiesParser = sharedPropertiesParser;
        loadTermuxPropertiesFromDisk();
    }

    /**
     * Reload the termux properties from disk into an in-memory cache.
     */
    public synchronized void loadTermuxPropertiesFromDisk() {
        // Properties files must be searched everytime since no file may exist when constructor is
        // called or a higher priority file may have been created afterward. Otherwise, if no file
        // was found, then default props would keep loading, since mSharedProperties would be null. #2836
        mSharedProperties = new SharedProperties(mPropertiesList, mSharedPropertiesParser);

        mSharedProperties.loadPropertiesFromDisk();
    }


    public Object getInternalPropertyValue(String key, boolean cached) {
        Object value;
        if (cached) {
            value = mSharedProperties.getInternalProperty(key);
            // If the value is not null since key was found or if the value was null since the
            // object stored for the key was itself null, we detect the later by checking if the key
            // exists in the map.
            if (value != null || mSharedProperties.getInternalProperties().containsKey(key)) {
                return value;
            } else {
                // This should not happen normally unless mMap was modified after the
                // {@link #loadTermuxPropertiesFromDisk()} call
                // A null value can still be returned by
                // {@link #getInternalPropertyValueFromValue(Context,String,String)} for some keys
                return getInternalTermuxPropertyValueFromValue(key);
            }
        } else {
            // We get the property value directly from file and return its internal value
            return getInternalTermuxPropertyValueFromValue(key);
        }
    }


    /**
     * The class that implements the {@link SharedPropertiesParser} interface.
     */
    public static class SharedPropertiesParserClient implements SharedPropertiesParser {

        /**
         * Override the
         * {@link SharedPropertiesParser#getInternalPropertyValueFromValue(String)}
         * interface function.
         */
        @Override
        public Object getInternalPropertyValueFromValue(String key) {
            return getInternalTermuxPropertyValueFromValue(key);
        }
    }


    /**
     * A static function that should return the internal termux {@link Object} for a key/value pair
     * read from properties file.
     *
     * @param key The key for which the internal object is required.
     * @return Returns the internal termux {@link Object} object.
     */
    public static Object getInternalTermuxPropertyValueFromValue(String key) {
        if (key == null) return null;
        /*
          For keys where a MAP_* is checked by respective functions. Note that value to this function
          would actually be the key for the MAP_*:
          - If the value is currently null, then searching MAP_* should also return null and internal default value will be used.
          - If the value is not null and does not exist in MAP_*, then internal default value will be used.
          - If the value is not null and does exist in MAP_*, then internal value returned by map will be used.
         */
        switch (key) {
            /* int */
            case TermuxPropertyConstants.KEY_TERMINAL_CURSOR_BLINK_RATE:
                return getTerminalCursorBlinkRateInternalPropertyValueFromValue();
            case TermuxPropertyConstants.KEY_TERMINAL_CURSOR_STYLE:
                return getTerminalCursorStyleInternalPropertyValueFromValue();
            case TermuxPropertyConstants.KEY_TERMINAL_TRANSCRIPT_ROWS:
                return getTerminalTranscriptRowsInternalPropertyValueFromValue();

            /* float */
            case TermuxPropertyConstants.KEY_TERMINAL_TOOLBAR_HEIGHT_SCALE_FACTOR:
                return getTerminalToolbarHeightScaleFactorInternalPropertyValueFromValue();

            /* Integer (may be null) */
            case TermuxPropertyConstants.KEY_SHORTCUT_CREATE_SESSION:
            case TermuxPropertyConstants.KEY_SHORTCUT_NEXT_SESSION:
            case TermuxPropertyConstants.KEY_SHORTCUT_PREVIOUS_SESSION:
            case TermuxPropertyConstants.KEY_SHORTCUT_RENAME_SESSION:
                return null;

            /* String (may be null) */
            case TermuxPropertyConstants.KEY_BACK_KEY_BEHAVIOUR:
                return getBackKeyBehaviourInternalPropertyValueFromValue();
            case TermuxPropertyConstants.KEY_EXTRA_KEYS:
                return getExtraKeysInternalPropertyValueFromValue();
            case TermuxPropertyConstants.KEY_EXTRA_KEYS_STYLE:
                return getExtraKeysStyleInternalPropertyValueFromValue();
            case TermuxPropertyConstants.KEY_SOFT_KEYBOARD_TOGGLE_BEHAVIOUR:
                return getSoftKeyboardToggleBehaviourInternalPropertyValueFromValue();
            case TermuxPropertyConstants.KEY_VOLUME_KEYS_BEHAVIOUR:
                return getVolumeKeysBehaviourInternalPropertyValueFromValue();

            default:
                // default false boolean behaviour
                if (TermuxPropertyConstants.TERMUX_DEFAULT_FALSE_BOOLEAN_BEHAVIOUR_PROPERTIES_LIST.contains(key))
                    return false;
                // default true boolean behaviour
                if (TermuxPropertyConstants.TERMUX_DEFAULT_TRUE_BOOLEAN_BEHAVIOUR_PROPERTIES_LIST.contains(key))
                    return true;
                else
                    return null;
        }
    }


    public static int getTerminalCursorBlinkRateInternalPropertyValueFromValue() {
        return TermuxPropertyConstants.DEFAULT_IVALUE_TERMINAL_CURSOR_BLINK_RATE;
    }

    public static int getTerminalCursorStyleInternalPropertyValueFromValue() {
        return TermuxPropertyConstants.DEFAULT_IVALUE_TERMINAL_CURSOR_STYLE;
    }

    public static int getTerminalTranscriptRowsInternalPropertyValueFromValue() {
        return TermuxPropertyConstants.DEFAULT_IVALUE_TERMINAL_TRANSCRIPT_ROWS;
    }

    public static float getTerminalToolbarHeightScaleFactorInternalPropertyValueFromValue() {
        return TermuxPropertyConstants.DEFAULT_IVALUE_TERMINAL_TOOLBAR_HEIGHT_SCALE_FACTOR;
    }

    public static String getBackKeyBehaviourInternalPropertyValueFromValue() {
        return TermuxPropertyConstants.DEFAULT_IVALUE_BACK_KEY_BEHAVIOUR;
    }

    public static String getExtraKeysInternalPropertyValueFromValue() {
        return TermuxPropertyConstants.DEFAULT_IVALUE_EXTRA_KEYS;
    }

    public static String getExtraKeysStyleInternalPropertyValueFromValue() {
        return TermuxPropertyConstants.DEFAULT_IVALUE_EXTRA_KEYS_STYLE;
    }

    public static String getSoftKeyboardToggleBehaviourInternalPropertyValueFromValue() {
        return TermuxPropertyConstants.DEFAULT_IVALUE_SOFT_KEYBOARD_TOGGLE_BEHAVIOUR;
    }

    public static String getVolumeKeysBehaviourInternalPropertyValueFromValue() {
        return TermuxPropertyConstants.DEFAULT_IVALUE_VOLUME_KEYS_BEHAVIOUR;
    }


    public boolean areHardwareKeyboardShortcutsDisabled() {
        return (boolean) getInternalPropertyValue(TermuxPropertyConstants.KEY_DISABLE_HARDWARE_KEYBOARD_SHORTCUTS, true);
    }

    public boolean areTerminalSessionChangeToastsDisabled() {
        return (boolean) getInternalPropertyValue(TermuxPropertyConstants.KEY_DISABLE_TERMINAL_SESSION_CHANGE_TOAST, true);
    }

    public boolean isEnforcingCharBasedInput() {
        return (boolean) getInternalPropertyValue(TermuxPropertyConstants.KEY_ENFORCE_CHAR_BASED_INPUT, true);
    }

    public boolean shouldExtraKeysTextBeAllCaps() {
        return (boolean) getInternalPropertyValue(TermuxPropertyConstants.KEY_EXTRA_KEYS_TEXT_ALL_CAPS, true);
    }

    public boolean shouldSoftKeyboardBeHiddenOnStartup() {
        return (boolean) getInternalPropertyValue(TermuxPropertyConstants.KEY_HIDE_SOFT_KEYBOARD_ON_STARTUP, true);
    }

    public boolean shouldOpenTerminalTranscriptURLOnClick() {
        return (boolean) getInternalPropertyValue(TermuxPropertyConstants.KEY_TERMINAL_ONCLICK_URL_OPEN, true);
    }

    public boolean isUsingCtrlSpaceWorkaround() {
        return (boolean) getInternalPropertyValue(TermuxPropertyConstants.KEY_USE_CTRL_SPACE_WORKAROUND, true);
    }

    public int getTerminalCursorBlinkRate() {
        return (int) getInternalPropertyValue(TermuxPropertyConstants.KEY_TERMINAL_CURSOR_BLINK_RATE, true);
    }

    public int getTerminalCursorStyle() {
        return (int) getInternalPropertyValue(TermuxPropertyConstants.KEY_TERMINAL_CURSOR_STYLE, true);
    }

    public int getTerminalTranscriptRows() {
        return (int) getInternalPropertyValue(TermuxPropertyConstants.KEY_TERMINAL_TRANSCRIPT_ROWS, true);
    }

    public float getTerminalToolbarHeightScaleFactor() {
        return (float) getInternalPropertyValue(TermuxPropertyConstants.KEY_TERMINAL_TOOLBAR_HEIGHT_SCALE_FACTOR, true);
    }

    public boolean isBackKeyTheEscapeKey() {
        return TermuxPropertyConstants.IVALUE_BACK_KEY_BEHAVIOUR_ESCAPE.equals(getInternalPropertyValue(TermuxPropertyConstants.KEY_BACK_KEY_BEHAVIOUR, true));
    }

    public boolean shouldEnableDisableSoftKeyboardOnToggle() {
        return TermuxPropertyConstants.IVALUE_SOFT_KEYBOARD_TOGGLE_BEHAVIOUR_ENABLE_DISABLE.equals(getInternalPropertyValue(TermuxPropertyConstants.KEY_SOFT_KEYBOARD_TOGGLE_BEHAVIOUR, true));
    }

    public boolean areVirtualVolumeKeysDisabled() {
        return TermuxPropertyConstants.IVALUE_VOLUME_KEY_BEHAVIOUR_VOLUME.equals(getInternalPropertyValue(TermuxPropertyConstants.KEY_VOLUME_KEYS_BEHAVIOUR, true));
    }


}
