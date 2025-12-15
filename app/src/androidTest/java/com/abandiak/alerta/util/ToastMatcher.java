package com.abandiak.alerta.util;

import android.os.IBinder;
import android.view.WindowManager;

import androidx.test.espresso.Root;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

public class ToastMatcher extends TypeSafeMatcher<Root> {

    @Override
    public void describeTo(Description description) {
        description.appendText("is toast");
    }

    @Override
    public boolean matchesSafely(Root root) {

        int type = root.getWindowLayoutParams().get().type;

        if (type == WindowManager.LayoutParams.TYPE_TOAST) {
            IBinder windowToken = root.getDecorView().getWindowToken();
            IBinder appToken = root.getDecorView().getApplicationWindowToken();
            return windowToken == appToken;
        }

        if (type == WindowManager.LayoutParams.TYPE_APPLICATION_PANEL
                || type == WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG
                || type == WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY) {

            CharSequence title = root.getWindowLayoutParams().get().getTitle();
            return title != null && title.toString().toLowerCase().contains("toast");
        }

        return false;
    }
}
