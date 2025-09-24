package awkoo.terminal.utils.properties;

import java.util.HashMap;

/**
 * An interface that must be defined by the caller of the {@link SharedProperties} class.
 */
public interface SharedPropertiesParser {

    /**
     * A function that should return the internal {@link Object} to be stored for a key/value pair
     * read from properties file in the {@link HashMap <>} in-memory cache.
     *
     * @param key     The key for which the internal object is required.
     * @return Returns the {@link Object} object to store in the {@link HashMap <>} in-memory cache.
     */
    Object getInternalPropertyValueFromValue(String key);

}
