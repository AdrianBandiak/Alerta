package com.abandiak.alerta.core.notifications;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.abandiak.alerta.R;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class AlertaMessagingService extends FirebaseMessagingService {

    private static final String CH_INCIDENTS = "incidents_channel";

    @Override
    public void onCreate() {
        super.onCreate();
        ensureChannel();
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        Log.d("AlertaFCM", "From: " + remoteMessage.getFrom());

        String title = getString(R.string.app_name);
        String body = "New alert";

        if (remoteMessage.getNotification() != null) {
            if (remoteMessage.getNotification().getTitle() != null) {
                title = remoteMessage.getNotification().getTitle();
            }
            if (remoteMessage.getNotification().getBody() != null) {
                body = remoteMessage.getNotification().getBody();
            }
        }

        NotificationCompat.Builder b = new NotificationCompat.Builder(this, CH_INCIDENTS)
                .setSmallIcon(R.drawable.ic_alert_triangle_cutout)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        NotificationManagerCompat.from(this)
                .notify((int) System.currentTimeMillis(), b.build());
    }

    @Override
    public void onNewToken(@NonNull String token) {
        Log.d("AlertaFCM", "Refreshed token: " + token);
    }

    private void ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    CH_INCIDENTS,
                    "Incidents",
                    NotificationManager.IMPORTANCE_HIGH
            );
            ch.setDescription("Notifications for incidents and alerts");
            ch.enableLights(true);
            ch.setLightColor(Color.RED);
            ch.enableVibration(true);
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(ch);
        }
    }
}
