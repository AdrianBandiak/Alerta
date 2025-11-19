package com.abandiak.alerta.data.model;

public class ChatMessage {

    private String id;
    private String senderId;
    private String text;
    private long createdAt;

    private String senderName;
    private String senderAvatar;

    public ChatMessage() {}

    public ChatMessage(String id, String senderId, String text, long createdAt) {
        this.id = id;
        this.senderId = senderId;
        this.text = text;
        this.createdAt = createdAt;
    }

    public ChatMessage(String id, String senderId, String text, long createdAt,
                       String senderName, String senderAvatar) {
        this.id = id;
        this.senderId = senderId;
        this.text = text;
        this.createdAt = createdAt;
        this.senderName = senderName;
        this.senderAvatar = senderAvatar;
    }

    public String getId() { return id; }
    public String getSenderId() { return senderId; }
    public String getText() { return text; }
    public long getCreatedAt() { return createdAt; }
    public String getSenderName() { return senderName; }
    public String getSenderAvatar() { return senderAvatar; }

    public void setSenderName(String senderName) { this.senderName = senderName; }
    public void setSenderAvatar(String senderAvatar) { this.senderAvatar = senderAvatar; }
}
