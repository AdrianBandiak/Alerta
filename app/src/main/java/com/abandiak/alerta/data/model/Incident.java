package com.abandiak.alerta.data.model;

import androidx.annotation.Nullable;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    @Nullable private String geohash;
    @Nullable private String regionBucket;

    @ServerTimestamp
    private Date createdAt;


    public Incident(String title,
                    String description,
                    String type,
                    double lat,
                    double lng,
                    String region,
                    String createdBy) {
        this(title, description, type, null, lat, lng, region, createdBy,
                new ArrayList<>(Collections.singletonList("public")));
    }

    public Incident(String title,
                    String description,
                    String type,
                    @Nullable String geohash,
                    double lat,
                    double lng,
                    String region,
                    String createdBy,
                    List<String> aud) {
        this.title = title;
        this.description = description;
        this.type = type;
        this.geohash = geohash;
        this.lat = lat;
        this.lng = lng;
        this.region = region;
        this.createdBy = createdBy;
        this.aud = (aud == null) ? new ArrayList<>() : new ArrayList<>(aud);
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
                ? Collections.singletonList("public")
                : aud);
        if (geohash != null)     m.put("geohash", geohash);
        if (regionBucket != null) m.put("regionBucket", regionBucket);

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
    public Date getCreatedAt() { return createdAt; }

    public void setAud(List<String> aud) { this.aud = aud; }
    public void setGeohash(@Nullable String geohash) { this.geohash = geohash; }
    public void setRegionBucket(@Nullable String regionBucket) { this.regionBucket = regionBucket; }
}
