package com.abandiak.alerta.app.messages;

public class TeamChatEntry {

    private String teamId;
    private String teamName;
    private String avatarUrl;
    private String lastMessage;
    private long lastTimestamp;

    public TeamChatEntry() { }

    public TeamChatEntry(String teamId, String teamName, String avatarUrl,
                         String lastMessage, long lastTimestamp) {
        this.teamId = teamId;
        this.teamName = teamName;
        this.avatarUrl = avatarUrl;
        this.lastMessage = lastMessage;
        this.lastTimestamp = lastTimestamp;
    }

    public String getTeamId() { return teamId; }
    public String getTeamName() { return teamName; }
    public String getAvatarUrl() { return avatarUrl; }
    public String getLastMessage() { return lastMessage; }
    public long getLastTimestamp() { return lastTimestamp; }

    public String getFormattedTime() {
        java.text.SimpleDateFormat sdf =
                new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault());
        return sdf.format(new java.util.Date(lastTimestamp));
    }
}
