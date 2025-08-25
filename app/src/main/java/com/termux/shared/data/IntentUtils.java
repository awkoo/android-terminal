package com.termux.shared.data;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.util.Arrays;

public class IntentUtils {

    private static final String LOG_TAG = "IntentUtils";


    /**
     * Get a {@link String} extra from an {@link Intent} if its not {@code null} or empty.
     *
     * @param intent                 The {@link Intent} to get the extra from.
     * @param key                    The {@link String} key name.
     * @param def                    The default value if extra is not set.
     * @param throwExceptionIfNotSet If set to {@code true}, then an exception will be thrown if extra
     *                               is not set.
     * @return Returns the {@link String} extra if set, otherwise {@code null}.
     */
    public static String getStringExtraIfSet(@NonNull Intent intent, String key, String def, boolean throwExceptionIfNotSet) throws Exception {
        String value = getStringExtraIfSet(intent, key, def);
        if (value == null && throwExceptionIfNotSet)
            throw new Exception("The \"" + key + "\" key string value is null or empty");
        return value;
    }

    /**
     * Get a {@link String} extra from an {@link Intent} if its not {@code null} or empty.
     *
     * @param intent The {@link Intent} to get the extra from.
     * @param key    The {@link String} key name.
     * @param def    The default value if extra is not set.
     * @return Returns the {@link String} extra if set, otherwise {@code null}.
     */
    public static String getStringExtraIfSet(@NonNull Intent intent, String key, String def) {
        String value = intent.getStringExtra(key);
        if (value == null || value.isEmpty()) {
            if (def != null && !def.isEmpty())
                return def;
            else
                return null;
        }
        return value;
    }

    /**
     * Get an {@link Integer} from an {@link Intent} stored as a {@link String} extra if its not
     * {@code null} or empty.
     *
     * @param intent The {@link Intent} to get the extra from.
     * @param key    The {@link String} key name.
     * @param def    The default value if extra is not set.
     * @return Returns the {@link Integer} extra if set, otherwise {@code null}.
     */
    public static Integer getIntegerExtraIfSet(@NonNull Intent intent, String key, Integer def) {
        try {
            String value = intent.getStringExtra(key);
            if (value == null || value.isEmpty()) {
                return def;
            }

            return Integer.parseInt(value);
        } catch (Exception e) {
            return def;
        }
    }


    /**
     * Get a {@link String[]} extra from an {@link Intent} if its not {@code null} or empty.
     *
     * @param intent                 The {@link Intent} to get the extra from.
     * @param key                    The {@link String} key name.
     * @param def                    The default value if extra is not set.
     * @param throwExceptionIfNotSet If set to {@code true}, then an exception will be thrown if extra
     *                               is not set.
     * @return Returns the {@link String[]} extra if set, otherwise {@code null}.
     */
    public static String[] getStringArrayExtraIfSet(@NonNull Intent intent, String key, String[] def, boolean throwExceptionIfNotSet) throws Exception {
        String[] value = getStringArrayExtraIfSet(intent, key, def);
        if (value == null && throwExceptionIfNotSet)
            throw new Exception("The \"" + key + "\" key string array is null or empty");
        return value;
    }

    /**
     * Get a {@link String[]} extra from an {@link Intent} if its not {@code null} or empty.
     *
     * @param intent The {@link Intent} to get the extra from.
     * @param key    The {@link String} key name.
     * @param def    The default value if extra is not set.
     * @return Returns the {@link String[]} extra if set, otherwise {@code null}.
     */
    public static String[] getStringArrayExtraIfSet(Intent intent, String key, String[] def) {
        String[] value = intent.getStringArrayExtra(key);
        if (value == null || value.length == 0) {
            if (def != null && def.length != 0)
                return def;
            else
                return null;
        }
        return value;
    }

    public static String getIntentString(Intent intent) {
        if (intent == null) return null;

        return intent.toString() + "\n" + getBundleString(intent.getExtras());
    }

    public static String getBundleString(Bundle bundle) {
        if (bundle == null || bundle.size() == 0) return "Bundle[]";

        StringBuilder bundleString = new StringBuilder("Bundle[\n");
        boolean first = true;
        for (String key : bundle.keySet()) {
            if (!first)
                bundleString.append("\n");

            bundleString.append(key).append(": `");

            Object value = bundle.get(key);
            switch (value) {
                case int[] ints -> bundleString.append(Arrays.toString(ints));
                case byte[] bytes -> bundleString.append(Arrays.toString(bytes));
                case boolean[] booleans -> bundleString.append(Arrays.toString(booleans));
                case short[] shorts -> bundleString.append(Arrays.toString(shorts));
                case long[] longs -> bundleString.append(Arrays.toString(longs));
                case float[] floats -> bundleString.append(Arrays.toString(floats));
                case double[] doubles -> bundleString.append(Arrays.toString(doubles));
                case String[] strings -> bundleString.append(Arrays.toString(strings));
                case CharSequence[] charSequences ->
                    bundleString.append(Arrays.toString(charSequences));
                case Parcelable[] parcelables -> bundleString.append(Arrays.toString(parcelables));
                case Bundle bundle1 -> bundleString.append(getBundleString(bundle1));
                case null, default -> bundleString.append(value);
            }

            bundleString.append("`");

            first = false;
        }

        bundleString.append("\n]");
        return bundleString.toString();
    }

}
