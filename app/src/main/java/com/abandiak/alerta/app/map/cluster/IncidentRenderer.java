package com.abandiak.alerta.app.map.cluster;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;

import androidx.annotation.NonNull;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;

public class IncidentRenderer extends DefaultClusterRenderer<IncidentItem> {

    public IncidentRenderer(Context context, GoogleMap map, ClusterManager<IncidentItem> clusterManager) {
        super(context, map, clusterManager);
    }

    @Override
    protected void onBeforeClusterItemRendered(@NonNull IncidentItem item, @NonNull MarkerOptions markerOptions) {
        if ("TEAM".equals(item.getType())) {
            int color = item.getTeamColor() != 0
                    ? item.getTeamColor()
                    : android.graphics.Color.parseColor("#1976D2");

            Bitmap shieldBitmap = createTeamShieldMarker(color);
            markerOptions.icon(BitmapDescriptorFactory.fromBitmap(shieldBitmap))
                    .title(item.getTitle())
                    .snippet(item.getSnippet());
        } else {
            float hue;
            switch (item.getType()) {
                case "CRITICAL":
                    hue = BitmapDescriptorFactory.HUE_RED;
                    break;
                case "HAZARD":
                    hue = BitmapDescriptorFactory.HUE_YELLOW;
                    break;
                case "INFO":
                default:
                    hue = BitmapDescriptorFactory.HUE_AZURE;
                    break;
            }
            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(hue))
                    .title(item.getTitle())
                    .snippet(item.getSnippet());
        }
    }


    private Bitmap createTeamShieldMarker(int color) {
        int width = 130;
        int height = 160;
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        paint.setColor(color);
        paint.setStyle(Paint.Style.FILL);

        Path path = new Path();
        float cx = width / 2f;
        float top = 20f;
        float bottom = height - 20f;

        path.moveTo(cx, bottom);
        path.lineTo(cx + width * 0.35f, top + height * 0.35f);
        path.quadTo(cx, top, cx - width * 0.35f, top + height * 0.35f);
        path.close();

        canvas.drawPath(path, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4);
        paint.setColor(Color.BLACK);
        canvas.drawPath(path, paint);

        Paint gloss = new Paint(Paint.ANTI_ALIAS_FLAG);
        gloss.setShader(new LinearGradient(0, top, 0, bottom / 2f,
                Color.argb(80, 255, 255, 255),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP));
        canvas.drawPath(path, gloss);

        return bitmap;
    }


    private float colorToHue(int color) {
        float[] hsv = new float[3];
        android.graphics.Color.colorToHSV(color, hsv);
        return hsv[0];
    }


    @Override
    protected boolean shouldRenderAsCluster(@NonNull com.google.maps.android.clustering.Cluster<IncidentItem> cluster) {
        return cluster.getSize() > 3;
    }
}
