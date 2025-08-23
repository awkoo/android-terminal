package com.termux.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

public class UI {
    public static void showToast(final Context context, final String toastText, boolean longDuration) {
        new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(context, toastText, longDuration ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT).show());
    }
}
