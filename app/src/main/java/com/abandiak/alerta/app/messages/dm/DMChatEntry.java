package com.abandiak.alerta.app.messages.dm;

public class DMChatEntry {

    private String chatId;
    private String otherUserId;
    private String otherUserName;
    private String lastMessage;
    private String avatarUrl;
    private long lastTimestamp;

    public DMChatEntry() {}

    public DMChatEntry(String chatId,
                       String otherUserId,
                       String otherUserName,
                       String lastMessage,
                       String avatarUrl,
                       long lastTimestamp) {

        this.chatId = chatId;
        this.otherUserId = otherUserId;
        this.otherUserName = otherUserName;
        this.lastMessage = lastMessage;
        this.avatarUrl = avatarUrl;
        this.lastTimestamp = lastTimestamp;
    }

    public String getChatId() {
        return chatId;
    }

    public String getOtherUserId() {
        return otherUserId;
    }

    public String getOtherUserName() {
        return otherUserName;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public long getLastTimestamp() {
        return lastTimestamp;
    }

    public void setOtherUserName(String name) {
        this.otherUserName = name;
    }

    public void setAvatarUrl(String url) {
        this.avatarUrl = url;
    }
}
