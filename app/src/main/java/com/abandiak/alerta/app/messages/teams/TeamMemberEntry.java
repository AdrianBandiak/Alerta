package com.abandiak.alerta.app.messages.teams;

public class TeamMemberEntry {

    private String uid;
    private String role;
    private long joinedAt;

    public TeamMemberEntry(String uid, String role, long joinedAt) {
        this.uid = uid;
        this.role = role;
        this.joinedAt = joinedAt;
    }

    public String getUid() { return uid; }
    public String getRole() { return role; }
    public long getJoinedAt() { return joinedAt; }
}

