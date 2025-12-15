package com.abandiak.alerta.models;

import com.abandiak.alerta.data.model.ChatMessage;

import org.junit.Test;
import static org.junit.Assert.*;

public class ChatMessageTest {

    @Test
    public void testEmptyConstructor() {
        ChatMessage msg = new ChatMessage();

        assertNull(msg.getId());
        assertNull(msg.getSenderId());
        assertNull(msg.getText());
        assertEquals(0, msg.getCreatedAt());
        assertNull(msg.getSenderName());
        assertNull(msg.getSenderAvatar());
    }

    @Test
    public void testConstructorBasic() {
        ChatMessage msg = new ChatMessage(
                "msg1",
                "user123",
                "Hello!",
                1700000000L
        );

        assertEquals("msg1", msg.getId());
        assertEquals("user123", msg.getSenderId());
        assertEquals("Hello!", msg.getText());
        assertEquals(1700000000L, msg.getCreatedAt());

        assertNull(msg.getSenderName());
        assertNull(msg.getSenderAvatar());
    }

    @Test
    public void testConstructorWithSenderDetails() {
        ChatMessage msg = new ChatMessage(
                "msg2",
                "userABC",
                "Hi!",
                1700001234L,
                "Alice",
                "avatar_url"
        );

        assertEquals("msg2", msg.getId());
        assertEquals("userABC", msg.getSenderId());
        assertEquals("Hi!", msg.getText());
        assertEquals(1700001234L, msg.getCreatedAt());
        assertEquals("Alice", msg.getSenderName());
        assertEquals("avatar_url", msg.getSenderAvatar());
    }

    @Test
    public void testSetters() {
        ChatMessage msg = new ChatMessage();

        msg.setSenderName("Bob");
        msg.setSenderAvatar("avatar_bob");

        assertEquals("Bob", msg.getSenderName());
        assertEquals("avatar_bob", msg.getSenderAvatar());
    }
}
