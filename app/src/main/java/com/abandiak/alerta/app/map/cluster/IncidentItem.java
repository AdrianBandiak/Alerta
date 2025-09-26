package com.abandiak.alerta.app.map.cluster;

import androidx.annotation.Nullable;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;

public class IncidentItem implements ClusterItem {
    private final LatLng position;
    private final String title;
    private final String snippet;
    private final String id;
    private final String type;
    @Nullable
    private final Float zIndex;

    public IncidentItem(String id, String title, String snippet, double lat, double lng, String type) {
        this.id = id;
        this.title = title;
        this.snippet = snippet;
        this.position = new LatLng(lat, lng);
        this.type = type;

        if ("CRITICAL".equals(type)) {
            this.zIndex = 2f;
        } else if ("HAZARD".equals(type)) {
            this.zIndex = 1f;
        } else {
            this.zIndex = 0f;
        }
    }

    @Override
    public LatLng getPosition() {
        return position;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public String getSnippet() {
        return snippet;
    }

    @Override
    @Nullable
    public Float getZIndex() {
        return zIndex;
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }
}
