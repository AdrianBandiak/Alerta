package com.abandiak.alerta.core.utils;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.Window;

import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.abandiak.alerta.R;

public class SystemBars {

    public static void apply(Activity activity) {

        Window window = activity.getWindow();
        View decorView = window.getDecorView();

        WindowCompat.setDecorFitsSystemWindows(window, false);

        window.setStatusBarColor(
                ContextCompat.getColor(activity, R.color.status_bar_gray)
        );
        window.setNavigationBarColor(
                ContextCompat.getColor(activity, R.color.status_bar_gray)
        );

        WindowInsetsControllerCompat controller =
                new WindowInsetsControllerCompat(window, decorView);
        controller.setAppearanceLightStatusBars(true);
    }

    public static int getStatusBarHeight(Context context) {
        int result = 0;
        int resourceId = context.getResources().getIdentifier(
                "status_bar_height", "dimen", "android"
        );

        if (resourceId > 0) {
            result = context.getResources().getDimensionPixelSize(resourceId);
        }

        return result;
    }
}
