package com.abandiak.alerta.app.messages;

public class DMChatEntry {

    private String chatId;
    private String userId;
    private String displayName;
    private String avatarUrl;
    private String lastMessage;
    private long lastTimestamp;

    public DMChatEntry() {}

    public DMChatEntry(String chatId, String userId, String displayName,
                       String avatarUrl, String lastMessage, long lastTimestamp) {
        this.chatId = chatId;
        this.userId = userId;
        this.displayName = displayName;
        this.avatarUrl = avatarUrl;
        this.lastMessage = lastMessage;
        this.lastTimestamp = lastTimestamp;
    }

    public String getChatId() { return chatId; }
    public String getUserId() { return userId; }
    public String getDisplayName() { return displayName; }
    public String getAvatarUrl() { return avatarUrl; }
    public String getLastMessage() { return lastMessage; }
    public long getLastTimestamp() { return lastTimestamp; }

    public String getFormattedTime() {
        java.text.SimpleDateFormat sdf =
                new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault());
        return sdf.format(new java.util.Date(lastTimestamp));
    }
}
