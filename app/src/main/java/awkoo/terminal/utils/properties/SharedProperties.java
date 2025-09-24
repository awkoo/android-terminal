package awkoo.terminal.utils.properties;

import androidx.annotation.NonNull;

import com.google.common.primitives.Primitives;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 这是一个类似于 Android 的 {@link android.content.SharedPreferences} 接口的实现，用于读写 ".properties" 文件，
 * 当使用实例对象时，它还维护一个键/值对的内存缓存。操作在同步锁下进行，应该是线程安全的。
 * <p>
 * 如果使用 {@link SharedProperties} 实例对象，则维护两种内存缓存映射：
 * 一种用于文件中找到的键的字面量 {@link String} 值，
 * 另一种用于存储供调用者内部使用的（接近）原始 {@link Object} 值。
 * <p>
 * {@link SharedProperties} 还提供了静态函数，可用于从文件或单个键值甚至其内部值中读取属性。
 * 还可以自动将布尔值映射为内部值。不维护内存缓存，也不使用锁。
 * <p>
 * 目前只支持读取，如果需要，以后会添加写入支持。请参考 Android 的 SharedPreferencesImpl 类获取参考实现。
 * <p>
 * https://cs.android.com/android/platform/superproject/+/android-11.0.0_r3:frameworks/base/core/java/android/app/SharedPreferencesImpl.java
 */
public class SharedProperties {

    /**
     * {@link HashMap<>} 对象，维护从 {@link #mPropertiesFile} 文件加载的值的内部值的内存缓存。
     * 键/值对是 {@link #mPropertiesList} 定义的任何键，这些键在文件中找到，并对应于通过调用
     * {@link #getInternalPropertyValueFromValue(String, String)} 接口返回的内部 {@link Object} 值。
     */
    private Map<String, Object> mMap;

    private final Set<String> mPropertiesList;
    private final SharedPropertiesParser mSharedPropertiesParser;

    private final Object mLock = new Object();

    /**
     * SharedProperties 类的构造函数。
     *
     * @param propertiesList         定义要加载哪些属性的 {@link Set<String>} 对象。
     *                               如果设置为 {@code null}，则 {@code propertiesFile} 中存在的所有属性都将由
     *                               {@link #loadPropertiesFromDisk()} 读取。
     * @param sharedPropertiesParser {@link SharedPropertiesParser} 接口的实现。
     */
    public SharedProperties(Set<String> propertiesList, @NonNull SharedPropertiesParser sharedPropertiesParser) {
        mPropertiesList = propertiesList;
        mSharedPropertiesParser = sharedPropertiesParser;

        mMap = new HashMap<>();
    }

    /**
     * 将属性加载到内存缓存中。由于属性文件系统已移除，
     * 此方法现在根据 {@link #mPropertiesList} 和 {@link SharedPropertiesParser} 初始化 {@link #mMap}。
     */
    public void loadPropertiesFromDisk() {
        synchronized (mLock) {
            HashMap<String, Object> map = new HashMap<>();

            Set<String> propertiesList = (mPropertiesList != null) ? mPropertiesList : new HashSet<>();

            Object internalValue;
            for (String key : propertiesList) {

                // 调用 {@link SharedPropertiesParser#getInternalPropertyValueFromValue(String)}
                // 接口方法以获取要存储在 {@link #mMap} 中的内部值。
                internalValue = mSharedPropertiesParser.getInternalPropertyValueFromValue(key);

                // 如果内部值成功添加到映射中，则也将值添加到 newProperties
                // 我们只存储 propertiesList 定义的内存值
                putToMap(map, key, internalValue); // null internalValue 将被放入映射
            }

            mMap = map;
        }
    }

    /**
     * 获取 {@link #mPropertiesFile} 的 {@link #mMap} 对象。在此之前必须调用
     * {@link #loadPropertiesFromDisk()}。
     *
     * @return 返回 {@link #mMap} 对象的副本。
     */
    public Map<String, Object> getInternalProperties() {
        synchronized (mLock) {
            if (mMap == null) mMap = new HashMap<>();
            return getMapCopy(mMap);
        }
    }

    /**
     * 从 {@link #mPropertiesFile} 获取传入键的内部 {@link Object} 值。
     * 该值从 {@link #mMap} 内存缓存中返回，因此在此之前必须调用
     * {@link #loadPropertiesFromDisk()}。
     *
     * @param key 要从 {@link #mMap} 对象中读取的键。
     * @return 返回 {@link Object} 对象。如果未找到键或对象为 {@code null}，则返回 {@code null}。
     * 使用 {@link HashMap#containsKey(Object)} 来检测后一种情况。
     */
    public Object getInternalProperty(String key) {
        synchronized (mLock) {
            // 不允许在 mMap 中存储 null 键
            if (key != null)
                return getInternalProperties().get(key);
            else
                return null;
        }
    }


    /**
     * 将值放入 {@link #mMap} 中。
     * 键不能为 {@code null}。
     * 只允许将 {@code null}、原始类型或其包装类或 String 类对象添加到映射中，尽管此限制可能会更改。
     *
     * @param map   要添加值的 {@link Map} 对象。
     * @param key   要为其添加值到映射的键。
     * @param value 要添加到映射的 {@link Object}。
     */
    public static void putToMap(HashMap<String, Object> map, String key, Object value) {

        if (map == null) return;

        // 不允许在 mMap 中存储 null 键
        if (key == null) return;

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
        }
    }

    public static Map<String, Object> getMapCopy(Map<String, Object> map) {
        if (map == null) return null;
        return new HashMap<>(map);
    }


}
