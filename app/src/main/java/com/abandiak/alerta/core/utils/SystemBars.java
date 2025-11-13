package com.abandiak.alerta.core.utils;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
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

        new WindowInsetsControllerCompat(window, decorView)
                .setAppearanceLightStatusBars(true);

        View statusBar = new View(activity);
        int height = activity.getResources().getDimensionPixelSize(
                activity.getResources().getIdentifier("status_bar_height", "dimen", "android")
        );
        statusBar.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                height
        ));
        statusBar.setBackgroundColor(ContextCompat.getColor(activity, R.color.status_bar_gray));
        ((ViewGroup) decorView).addView(statusBar);
    }
}
