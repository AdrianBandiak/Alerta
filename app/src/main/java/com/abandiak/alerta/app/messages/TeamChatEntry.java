package com.abandiak.alerta.app.messages;

public class TeamChatEntry {

    private String teamId;
    private String teamName;
    private int teamColor;
    private String lastMessage;
    private long lastTimestamp;

    public TeamChatEntry() {}

    public TeamChatEntry(String teamId, String teamName, int teamColor,
                         String lastMessage, long lastTimestamp) {
        this.teamId = teamId;
        this.teamName = teamName;
        this.teamColor = teamColor;
        this.lastMessage = lastMessage;
        this.lastTimestamp = lastTimestamp;
    }

    public String getTeamId() { return teamId; }
    public String getTeamName() { return teamName; }
    public int getTeamColor() { return teamColor; }
    public String getLastMessage() { return lastMessage; }
    public long getLastTimestamp() { return lastTimestamp; }

    public String getFormattedTime() {
        java.text.SimpleDateFormat sdf =
                new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault());
        return sdf.format(new java.util.Date(lastTimestamp));
    }
}
