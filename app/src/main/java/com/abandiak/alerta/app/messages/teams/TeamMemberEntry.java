package com.abandiak.alerta.app.messages.teams;

public class TeamMemberEntry {

    private final String uid;
    private final String fullName;
    private final String avatarUrl;
    private final long joinedAt;

    public TeamMemberEntry(String uid, String fullName, String avatarUrl, long joinedAt) {
        this.uid = uid;
        this.fullName = fullName;
        this.avatarUrl = avatarUrl;
        this.joinedAt = joinedAt;
    }

    public String getUid() { return uid; }
    public String getFullName() { return fullName; }
    public String getAvatarUrl() { return avatarUrl; }
    public long getJoinedAt() { return joinedAt; }
}
