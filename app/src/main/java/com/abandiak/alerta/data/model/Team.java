package com.abandiak.alerta.data.model;

public class Team {
    private String id;
    private String name;
    private String description;
    private String code;
    private String createdBy;
    private long createdAt;

    public Team() {}
    public Team(String id, String name, String description, String code, String createdBy, long createdAt) {
        this.id = id; this.name = name; this.description = description;
        this.code = code; this.createdBy = createdBy; this.createdAt = createdAt;
    }
    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getCode() { return code; }
    public String getCreatedBy() { return createdBy; }
    public long getCreatedAt() { return createdAt; }
    public void setId(String id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setCode(String code) { this.code = code; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
