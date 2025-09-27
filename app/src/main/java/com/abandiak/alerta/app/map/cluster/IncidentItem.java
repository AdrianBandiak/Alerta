package com.abandiak.alerta.app.map.cluster;

import androidx.annotation.Nullable;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;

public class IncidentItem implements ClusterItem {

    private final String id;
    private final String title;
    private final String snippet;
    private final LatLng position;
    private final String type;
    @Nullable private final String photoUrl;

    public IncidentItem(String id,
                        String title,
                        String snippet,
                        double lat,
                        double lng,
                        String type,
                        @Nullable String photoUrl) {
        this.id = id;
        this.title = title;
        this.snippet = snippet;
        this.position = new LatLng(lat, lng);
        this.type = type;
        this.photoUrl = photoUrl;
    }

    public IncidentItem(String id,
                        String title,
                        String snippet,
                        double lat,
                        double lng,
                        String type) {
        this(id, title, snippet, lat, lng, type, null);
    }

    @Override public LatLng getPosition() { return position; }
    @Override public String getTitle()    { return title; }
    @Override public String getSnippet()  { return snippet; }
    @Override public Float getZIndex()    { return 0f; }

    public String getId()       { return id; }
    public String getType()     { return type; }
    @Nullable
    public String getPhotoUrl() { return photoUrl; }
}
