package com.abandiak.alerta.data.model;

import androidx.annotation.Nullable;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.ServerTimestamp;
import java.util.*;

public class Incident {

    private String id;
    private String title;
    private String description;
    private String type;
    private double lat;
    private double lng;
    private String region;
    private String createdBy;
    private List<String> aud;
    private int teamColor;
    @Nullable private String teamId;

    @Nullable private String geohash;
    @Nullable private String regionBucket;
    @Nullable private String photoUrl;

    private boolean verified;
    @Nullable private String verifiedBy;
    @Nullable private List<Map<String, Object>> logs;

    @ServerTimestamp
    private Date createdAt;

    @SuppressWarnings("unused")
    public Incident() { }

    public Incident(String title, String description, String type,
                    double lat, double lng, String region, String createdBy) {
        this(title, description, type, null, lat, lng, region, createdBy,
                new ArrayList<>(Collections.singletonList("public")), null);
    }

    public Incident(String title, String description, String type, @Nullable String geohash,
                    double lat, double lng, String region, String createdBy,
                    List<String> aud, @Nullable String photoUrl) {
        this.title = title;
        this.description = description;
        this.type = type;
        this.geohash = geohash;
        this.lat = lat;
        this.lng = lng;
        this.region = region;
        this.createdBy = createdBy;
        this.aud = (aud == null) ? new ArrayList<>() : new ArrayList<>(aud);
        this.photoUrl = photoUrl;
        this.verified = false;
        this.verifiedBy = null;
        this.logs = new ArrayList<>();

        Map<String, Object> logEntry = new HashMap<>();
        logEntry.put("timestamp", System.currentTimeMillis());
        logEntry.put("action", "Incident created");
        logs.add(logEntry);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> m = new HashMap<>();
        m.put("title", title);
        m.put("description", description);
        m.put("type", type);
        m.put("lat", lat);
        m.put("lng", lng);
        m.put("region", region);
        m.put("createdBy", createdBy);
        m.put("aud", (aud == null || aud.isEmpty())
                ? Collections.singletonList("public") : aud);

        if (geohash != null) m.put("geohash", geohash);
        if (regionBucket != null) m.put("regionBucket", regionBucket);
        if (photoUrl != null) m.put("photoUrl", photoUrl);
        if (teamId != null) m.put("teamId", teamId);
        if (teamColor != 0) m.put("teamColor", teamColor);

        m.put("verified", verified);
        m.put("verifiedBy", verifiedBy);
        m.put("logs", logs);
        m.put("createdAt", FieldValue.serverTimestamp());
        return m;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getType() { return type; }
    public double getLat() { return lat; }
    public double getLng() { return lng; }
    public String getRegion() { return region; }
    public String getCreatedBy() { return createdBy; }
    public List<String> getAud() { return aud; }
    @Nullable public String getGeohash() { return geohash; }
    @Nullable public String getRegionBucket() { return regionBucket; }
    @Nullable public String getPhotoUrl() { return photoUrl; }
    public Date getCreatedAt() { return createdAt; }
    public boolean isVerified() { return verified; }
    @Nullable public String getVerifiedBy() { return verifiedBy; }
    @Nullable public List<Map<String, Object>> getLogs() { return logs; }

    public void setVerified(boolean verified) { this.verified = verified; }
    public void setVerifiedBy(@Nullable String verifiedBy) { this.verifiedBy = verifiedBy; }
    public void setLogs(@Nullable List<Map<String, Object>> logs) { this.logs = logs; }
    @Nullable public String getTeamId() { return teamId; }
    public void setTeamId(@Nullable String teamId) { this.teamId = teamId; }

    public int getTeamColor() { return teamColor; }
    public void setTeamColor(int teamColor) { this.teamColor = teamColor; }

}
