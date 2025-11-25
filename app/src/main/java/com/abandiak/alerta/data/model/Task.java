package com.abandiak.alerta.data.model;

public class Task {

    private String id;
    private String title;
    private String createdBy;
    private String time;
    private boolean completed;
    private String date;

    private String description;
    private String priority;
    private String endDate;

    private String type = "NORMAL";
    private String teamId;
    private Integer teamColor;

    public Task() {
    }

    public Task(String id, String title, String createdBy, String time,
                boolean completed, String date) {

        this.id = id;
        this.title = title;
        this.createdBy = createdBy;
        this.time = time;
        this.completed = completed;
        this.date = date;
        this.type = "NORMAL";
    }


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTeamId() {
        return teamId;
    }

    public void setTeamId(String teamId) {
        this.teamId = teamId;
    }

    public Integer getTeamColor() {
        return teamColor;
    }

    public void setTeamColor(Integer teamColor) {
        this.teamColor = teamColor;
    }
}
