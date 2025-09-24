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


    public Object getInternalPropertyValue(String key) {
        Object value = mSharedProperties.getInternalProperty(key);
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
        return switch (key) {

            /* Integer (may be null) */
            case TermuxPropertyConstants.KEY_SHORTCUT_CREATE_SESSION,
                 TermuxPropertyConstants.KEY_SHORTCUT_NEXT_SESSION,
                 TermuxPropertyConstants.KEY_SHORTCUT_PREVIOUS_SESSION,
                 TermuxPropertyConstants.KEY_SHORTCUT_RENAME_SESSION -> null;

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


    public boolean isBackKeyTheEscapeKey() {
        return TermuxPropertyConstants.IVALUE_BACK_KEY_BEHAVIOUR_ESCAPE.equals(getInternalPropertyValue(TermuxPropertyConstants.KEY_BACK_KEY_BEHAVIOUR));
    }

    public boolean shouldEnableDisableSoftKeyboardOnToggle() {
        return TermuxPropertyConstants.IVALUE_SOFT_KEYBOARD_TOGGLE_BEHAVIOUR_ENABLE_DISABLE.equals(getInternalPropertyValue(TermuxPropertyConstants.KEY_SOFT_KEYBOARD_TOGGLE_BEHAVIOUR));
    }

    public boolean areVirtualVolumeKeysDisabled() {
        return TermuxPropertyConstants.IVALUE_VOLUME_KEY_BEHAVIOUR_VOLUME.equals(getInternalPropertyValue(TermuxPropertyConstants.KEY_VOLUME_KEYS_BEHAVIOUR));
    }


}
