package awkoo.terminal.utils;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;

import androidx.appcompat.app.AppCompatActivity;

public class ThemeUtils {


    /**
     * Get a values defined by the current heme listed in attrs.
     *
     * @param context The context for operations. It must be an instance of {@link Activity} or
     *                {@link AppCompatActivity} or one with which a theme attribute can be got.
     *                Do no use application context.
     * @param attr    The attr id.
     * @param def     The def value to return.
     * @return Returns the {@code attr} value if found, otherwise {@code def}.
     */
    public static int getSystemAttrColor(Context context, int attr, int def) {
        TypedArray typedArray = context.getTheme().obtainStyledAttributes(new int[]{attr});
        int color = typedArray.getColor(0, def);
        typedArray.recycle();
        return color;
    }

}
