package com.abandiak.alerta.app.map.cluster;

import android.content.Context;

import androidx.annotation.NonNull;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;
import com.google.maps.android.ui.IconGenerator;

public class IncidentRenderer extends DefaultClusterRenderer<IncidentItem> {

    public IncidentRenderer(Context context, GoogleMap map, ClusterManager<IncidentItem> clusterManager) {
        super(context, map, clusterManager);
    }

    @Override
    protected void onBeforeClusterItemRendered(@NonNull IncidentItem item, @NonNull MarkerOptions markerOptions) {
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

    @Override
    protected boolean shouldRenderAsCluster(@NonNull com.google.maps.android.clustering.Cluster<IncidentItem> cluster) {
        return cluster.getSize() > 3;
    }
}
