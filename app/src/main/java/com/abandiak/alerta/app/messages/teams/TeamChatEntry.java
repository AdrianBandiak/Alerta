package com.abandiak.alerta.app.messages.teams;

public class TeamChatEntry {

    private String teamId;
    private String teamName;
    private String lastMessage;
    private long lastTimestamp;
    private int teamColor;

    public TeamChatEntry() {}

    public TeamChatEntry(String teamId, String teamName, String lastMessage, long lastTimestamp, int teamColor) {
        this.teamId = teamId;
        this.teamName = teamName;
        this.lastMessage = lastMessage;
        this.lastTimestamp = lastTimestamp;
        this.teamColor = teamColor;
    }

    public String getTeamId() { return teamId; }
    public String getTeamName() { return teamName; }
    public String getLastMessage() { return lastMessage; }
    public long getLastTimestamp() { return lastTimestamp; }
    public int getTeamColor() { return teamColor; }

    public String getFormattedTime() {
        if (lastTimestamp <= 0) return "";
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm");
        return sdf.format(new java.util.Date(lastTimestamp));
    }

    public void setTeamId(String teamId) { this.teamId = teamId; }
    public void setTeamName(String teamName) { this.teamName = teamName; }
    public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; }
    public void setLastTimestamp(long lastTimestamp) { this.lastTimestamp = lastTimestamp; }
    public void setTeamColor(int teamColor) { this.teamColor = teamColor; }
}
