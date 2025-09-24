package awkoo.terminal.utils.properties;

public abstract class TermuxSharedProperties {


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
        return switch (key) {

//            /* Integer (may be null) */
//            case TermuxPropertyConstants.KEY_SHORTCUT_CREATE_SESSION,
//                 TermuxPropertyConstants.KEY_SHORTCUT_NEXT_SESSION,
//                 TermuxPropertyConstants.KEY_SHORTCUT_PREVIOUS_SESSION,
//                 TermuxPropertyConstants.KEY_SHORTCUT_RENAME_SESSION -> null;

            /* String (may be null) */
            case TermuxPropertyConstants.KEY_BACK_KEY_BEHAVIOUR ->
                TermuxPropertyConstants.DEFAULT_IVALUE_BACK_KEY_BEHAVIOUR;
            case TermuxPropertyConstants.KEY_EXTRA_KEYS ->
                TermuxPropertyConstants.DEFAULT_IVALUE_EXTRA_KEYS;
            case TermuxPropertyConstants.KEY_EXTRA_KEYS_STYLE ->
                TermuxPropertyConstants.DEFAULT_IVALUE_EXTRA_KEYS_STYLE;
            case TermuxPropertyConstants.KEY_SOFT_KEYBOARD_TOGGLE_BEHAVIOUR ->
                TermuxPropertyConstants.DEFAULT_IVALUE_SOFT_KEYBOARD_TOGGLE_BEHAVIOUR;
            case TermuxPropertyConstants.KEY_VOLUME_KEYS_BEHAVIOUR ->
                TermuxPropertyConstants.DEFAULT_IVALUE_VOLUME_KEYS_BEHAVIOUR;
            default -> null;
        };
    }


    public static boolean isBackKeyTheEscapeKey() {
        return TermuxPropertyConstants.IVALUE_BACK_KEY_BEHAVIOUR_ESCAPE.equals(getInternalTermuxPropertyValueFromValue(TermuxPropertyConstants.KEY_BACK_KEY_BEHAVIOUR));
    }

    public static boolean shouldEnableDisableSoftKeyboardOnToggle() {
        return TermuxPropertyConstants.IVALUE_SOFT_KEYBOARD_TOGGLE_BEHAVIOUR_ENABLE_DISABLE.equals(getInternalTermuxPropertyValueFromValue(TermuxPropertyConstants.KEY_SOFT_KEYBOARD_TOGGLE_BEHAVIOUR));
    }

    public static boolean areVirtualVolumeKeysDisabled() {
        return TermuxPropertyConstants.IVALUE_VOLUME_KEY_BEHAVIOUR_VOLUME.equals(getInternalTermuxPropertyValueFromValue(TermuxPropertyConstants.KEY_VOLUME_KEYS_BEHAVIOUR));
    }


}
