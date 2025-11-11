package com.abandiak.alerta.app.map.cluster;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;

public class IncidentItem implements ClusterItem {

    private final String id;
    private final String title;
    private final String snippet;
    private final LatLng position;
    private final String type;
    private int teamColor;
    private String teamId;

    @Nullable
    private final String photoUrl;
    @Nullable
    private final String createdBy;

    private final boolean verified;

    public IncidentItem(String id,
                        String title,
                        String snippet,
                        double lat,
                        double lng,
                        String type,
                        @Nullable String photoUrl) {
        this(id, title, snippet, lat, lng, type, photoUrl, false, null);
    }

    public IncidentItem(String id,
                        String title,
                        String snippet,
                        double lat,
                        double lng,
                        String type) {
        this(id, title, snippet, lat, lng, type, null, false, null);
    }

    public IncidentItem(String id,
                        String title,
                        String snippet,
                        double lat,
                        double lng,
                        String type,
                        @Nullable String photoUrl,
                        boolean verified,
                        @Nullable String createdBy) {
        this.id = id;
        this.title = title;
        this.snippet = snippet;
        this.position = new LatLng(lat, lng);
        this.type = type;
        this.photoUrl = photoUrl;
        this.verified = verified;
        this.createdBy = createdBy;
    }

    @NonNull
    @Override public LatLng getPosition() { return position; }
    @Override public String getTitle()    { return title; }
    @Override public String getSnippet()  { return snippet; }
    @Override public Float getZIndex()    { return 0f; }

    public String getId()        { return id; }
    public String getType()      { return type; }

    @Nullable
    public String getPhotoUrl()  { return photoUrl; }

    @Nullable
    public String getCreatedBy() { return createdBy; }

    public boolean isVerified()  { return verified; }
    public int getTeamColor() { return teamColor; }
    public void setTeamColor(int teamColor) { this.teamColor = teamColor; }
    public String getTeamId() { return teamId; }
    public void setTeamId(String teamId) { this.teamId = teamId; }
}
