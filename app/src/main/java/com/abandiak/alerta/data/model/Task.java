package com.abandiak.alerta.data.model;

public class Task {
    private String id;
    private String title;
    private String createdBy;
    private String time;
    private boolean completed;
    private String date;

    public Task() {}

    public Task(String id, String title, String createdBy, String time, boolean completed, String date) {
        this.id = id;
        this.title = title;
        this.createdBy = createdBy;
        this.time = time;
        this.completed = completed;
        this.date = date;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getCreatedBy() { return createdBy; }
    public String getTime() { return time; }
    public boolean isCompleted() { return completed; }
    public String getDate() { return date; }

    public void setCompleted(boolean completed) { this.completed = completed; }
}
