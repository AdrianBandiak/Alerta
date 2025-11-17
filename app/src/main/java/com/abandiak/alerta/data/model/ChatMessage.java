package com.abandiak.alerta.data.model;

public class ChatMessage {

    private String id;
    private String senderId;
    private String text;
    private long createdAt;

    public ChatMessage() {
    }

    public ChatMessage(String id, String senderId, String text, long createdAt) {
        this.id = id;
        this.senderId = senderId;
        this.text = text;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public String getSenderId() {
        return senderId;
    }

    public String getText() {
        return text;
    }

    public long getCreatedAt() {
        return createdAt;
    }
}
