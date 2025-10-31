package com.abandiak.alerta.data.model;

public class Team {
    private String id;
    private String name;
    private String description;
    private String code;
    private String createdBy;
    private String createdByName;
    private long createdAt;
    private int color;

    public Team() {}

    public Team(String id, String name, String description, String code, String createdBy, String createdByName, long createdAt, int color) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.code = code;
        this.createdBy = createdBy;
        this.createdByName = createdByName;
        this.createdAt = createdAt;
        this.color = color;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getCode() { return code; }
    public String getCreatedBy() { return createdBy; }
    public String getCreatedByName() { return createdByName; }
    public long getCreatedAt() { return createdAt; }
    public int getColor() { return color; }

    public void setId(String id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setCode(String code) { this.code = code; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public void setCreatedByName(String createdByName) { this.createdByName = createdByName; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public void setColor(int color) { this.color = color; }
}
