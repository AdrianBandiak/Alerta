package com.abandiak.alerta.core.utils;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.abandiak.alerta.R;

public final class ToastUtils {
    private static Toast current;

    private ToastUtils() { }

    public static void show(Context context, String message) {
        View layout = LayoutInflater.from(context).inflate(R.layout.toast_custom, null, false);

        TextView text = layout.findViewById(R.id.toast_text);
        ImageView icon = layout.findViewById(R.id.toast_icon);

        text.setText(message);
        icon.setImageResource(R.drawable.logo_alerta);

        if (current != null) {
            current.cancel();
        }
        current = new Toast(context.getApplicationContext());
        current.setDuration(Toast.LENGTH_SHORT);
        current.setView(layout);
        current.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 100);
        current.show();
    }
}
