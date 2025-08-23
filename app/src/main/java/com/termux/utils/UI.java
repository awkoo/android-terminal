package com.termux.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.termux.R;

public class UI {
    public static void showToast(final Context context, final String toastText, boolean longDuration) {
        new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(context, toastText, longDuration ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT).show());
    }

    public static void setDefaultToolbar(AppCompatActivity activity, Boolean showBackButton) {
        activity.setSupportActionBar(activity.findViewById(R.id.toolbar));
        if (showBackButton) {
            ActionBar actionBar = activity.getSupportActionBar();
            if (actionBar != null) {
                actionBar.setDisplayHomeAsUpEnabled(true);
                actionBar.setDisplayShowHomeEnabled(true);
            }
        }
    }
}
