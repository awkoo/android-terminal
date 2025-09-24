package awkoo.terminal.utils.properties;

import androidx.annotation.NonNull;

import com.google.common.primitives.Primitives;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * An implementation similar to android's {@link android.content.SharedPreferences} interface for
 * reading and writing to and from ".properties" files which also maintains an in-memory cache for
 * the key/value pairs when an instance object is used. Operations are done under
 * synchronization locks and should be thread safe.
 * <p>
 * If {@link SharedProperties} instance object is used, then two types of in-memory cache maps are
 * maintained, one for the literal {@link String} values found in the file for the keys and an
 * additional one that stores (near) primitive {@link Object} values for internal use by the caller.
 * <p>
 * The {@link SharedProperties} also provides static functions that can be used to read properties
 * from files or individual key values or even their internal values. An automatic mapping to a
 * boolean as internal value can also be done. An in-memory cache is not maintained, nor are locks used.
 * <p>
 * This currently only has read support, write support can/will be added later if needed. Check android's
 * SharedPreferencesImpl class for reference implementation.
 * <p>
 * https://cs.android.com/android/platform/superproject/+/android-11.0.0_r3:frameworks/base/core/java/android/app/SharedPreferencesImpl.java
 */
public class SharedProperties {

    /**
     * The {@link HashMap<>} object that maintains an in-memory cache of internal values for the values
     * loaded from the {@link #mPropertiesFile} file. The key/value pairs are of any keys defined by
     * {@link #mPropertiesList} that are found in the file against their internal {@link Object} values
     * returned by the call to
     * {@link #getInternalPropertyValueFromValue(String, String)} interface.
     */
    private Map<String, Object> mMap;

    private final Set<String> mPropertiesList;
    private final SharedPropertiesParser mSharedPropertiesParser;

    private final Object mLock = new Object();

    /**
     * Constructor for the SharedProperties class.
     *
     * @param propertiesList         The {@link Set<String>} object that defined which properties to load.
     *                               If this is set to {@code null}, then all properties that exist in
     *                               {@code propertiesFile} will be read by {@link #loadPropertiesFromDisk()}
     * @param sharedPropertiesParser The implementation of the {@link SharedPropertiesParser} interface.
     */
    public SharedProperties(Set<String> propertiesList, @NonNull SharedPropertiesParser sharedPropertiesParser) {
        mPropertiesList = propertiesList;
        mSharedPropertiesParser = sharedPropertiesParser;

        mMap = new HashMap<>();
    }

    public void loadPropertiesFromDisk() {
        synchronized (mLock) {
            // Get properties from mPropertiesFile
            Properties properties = new Properties();

            // We still need to load default values into mMap, so we assume no properties defined if
            // reading from mPropertiesFile failed

            HashMap<String, Object> map = new HashMap<>();
            Properties newProperties = new Properties();

            Set<String> propertiesList = mPropertiesList;
            if (propertiesList == null)
                propertiesList = properties.stringPropertyNames();

            Object internalValue;
            for (String key : propertiesList) {

                // Call the {@link SharedPropertiesParser#getInternalPropertyValueFromValue(Context,String,String)}
                // interface method to get the internal value to store in the {@link #mMap}.
                internalValue = mSharedPropertiesParser.getInternalPropertyValueFromValue(key);

                // If the internal value was successfully added to map, then also add value to newProperties
                // We only store values in-memory defined by propertiesList
                if (putToMap(map, key, internalValue)) { // null internalValue will be put into map
                    putToProperties(newProperties, key, null); // null value will **not** be into properties
                }
            }

            mMap = map;
        }
    }

    /**
     * Get the {@link #mMap} object for the {@link #mPropertiesFile}. A call to
     * {@link #loadPropertiesFromDisk()} must be made before this.
     *
     * @return Returns a copy of {@link #mMap} object.
     */
    public Map<String, Object> getInternalProperties() {
        synchronized (mLock) {
            if (mMap == null) mMap = new HashMap<>();
            return getMapCopy(mMap);
        }
    }

    /**
     * Get the internal {@link Object} value for the key passed from the {@link #mPropertiesFile}.
     * The value is returned from the {@link #mMap} in-memory cache, so a call to
     * {@link #loadPropertiesFromDisk()} must be made before this.
     *
     * @param key The key to read from the {@link #mMap} object.
     * @return Returns the {@link Object} object. This will be {@code null} if key is not found or
     * if object was {@code null}. Use {@link HashMap#containsKey(Object)} to detect the later.
     * situation.
     */
    public Object getInternalProperty(String key) {
        synchronized (mLock) {
            // null keys are not allowed to be stored in mMap
            if (key != null)
                return getInternalProperties().get(key);
            else
                return null;
        }
    }


    /**
     * Put a value in a {@link #mMap}.
     * The key cannot be {@code null}.
     * Only {@code null}, primitive or their wrapper classes or String class objects are allowed to be added to
     * the map, although this limitation may be changed.
     *
     * @param map   The {@link Map} object to add value to.
     * @param key   The key for which to add the value to the map.
     * @param value The {@link Object} to add to the map.
     * @return Returns {@code true} if value was successfully added, otherwise {@code false}.
     */
    public static boolean putToMap(HashMap<String, Object> map, String key, Object value) {

        if (map == null) return false;

        // null keys are not allowed to be stored in mMap
        if (key == null) return false;

        boolean put = false;
        if (value != null) {
            Class<?> clazz = value.getClass();
            if (clazz.isPrimitive() || Primitives.isWrapperType(clazz) || value instanceof String) {
                put = true;
            }
        } else {
            put = true;
        }

        if (put) {
            map.put(key, value);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Put a value in a {@link Map}.
     * The key cannot be {@code null}.
     * Passing {@code null} as the value argument is equivalent to removing the key from the
     * properties.
     *
     * @param properties The {@link Properties} object to add value to.
     * @param key        The key for which to add the value to the properties.
     * @param value      The {@link String} to add to the properties.
     */
    public static void putToProperties(Properties properties, String key, String value) {

        if (properties == null) return;

        // null keys are not allowed to be stored in mMap
        if (key == null) return;

        if (value != null) {
            properties.put(key, value);
        } else {
            properties.remove(key);
        }

    }

    public static Map<String, Object> getMapCopy(Map<String, Object> map) {
        if (map == null) return null;
        return new HashMap<>(map);
    }


}
