package com.abandiak.alerta.data.model;

import com.google.firebase.Timestamp;
import java.util.List;

public class Team {

    private String id;
    private String name;
    private String description;
    private String code;
    private String createdBy;
    private String region;

    private Timestamp createdAt;
    private int color;

    private String lastMessage;
    private Timestamp lastTimestamp;

    private List<String> membersIndex;

    public Team() {}

    public Team(String id, String name, String description, String code,
                String createdBy, String createdByName, Timestamp createdAt,
                int color, String region) {

        this.id = id;
        this.name = name;
        this.description = description;
        this.code = code;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.color = color;
        this.region = region;

        this.lastMessage = "";
        this.lastTimestamp = null;
    }

    public void setId(String id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setRegion(String region) { this.region = region; }
    public void setColor(int color) { this.color = color; }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
    public void setCreatedAt(Timestamp ts) { this.createdAt = ts; }

    public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; }
    public void setLastTimestamp(Timestamp lastTimestamp) { this.lastTimestamp = lastTimestamp; }

    public void setMembersIndex(List<String> membersIndex) { this.membersIndex = membersIndex; }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getCode() { return code; }
    public String getCreatedBy() { return createdBy; }
    public Timestamp getCreatedAt() { return createdAt; }
    public int getColor() { return color; }
    public String getRegion() { return region; }

    public String getLastMessage() { return lastMessage; }
    public Timestamp getLastTimestamp() { return lastTimestamp; }

    public List<String> getMembersIndex() { return membersIndex; }

    public long getLastTimestampMillis() {
        return lastTimestamp != null ? lastTimestamp.toDate().getTime() : 0;
    }

    public long getCreatedAtMillis() {
        return createdAt != null ? createdAt.toDate().getTime() : 0;
    }
}