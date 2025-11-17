package com.abandiak.alerta.data.repository;

import com.abandiak.alerta.data.model.ChatMessage;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;
import com.google.firebase.firestore.EventListener;

import java.util.*;

public class ChatRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final String currentUid = FirebaseAuth.getInstance().getUid();

    private CollectionReference dmChats() {
        return db.collection("dm_chats");
    }

    private CollectionReference teams() {
        return db.collection("teams");
    }

    public String dmChatIdFor(String userA, String userB) {
        return (userA.compareTo(userB) < 0)
                ? userA + "_" + userB
                : userB + "_" + userA;
    }

    public void sendDirectMessage(String otherUserId, String text) {
        if (currentUid == null) return;

        String chatId = dmChatIdFor(currentUid, otherUserId);
        DocumentReference chatRef = dmChats().document(chatId);

        Map<String, Object> message = new HashMap<>();
        message.put("senderId", currentUid);
        message.put("text", text);
        message.put("createdAt", FieldValue.serverTimestamp());

        chatRef.collection("messages").add(message);

        Map<String, Object> meta = new HashMap<>();
        meta.put("participants", Arrays.asList(currentUid, otherUserId));
        meta.put("lastMessage", text);
        meta.put("lastTimestamp", FieldValue.serverTimestamp());

        chatRef.set(meta, SetOptions.merge());
    }

    public void sendTeamMessage(String teamId, String text) {
        if (currentUid == null) return;

        DocumentReference teamRef = teams().document(teamId);

        Map<String, Object> message = new HashMap<>();
        message.put("senderId", currentUid);
        message.put("text", text);
        message.put("createdAt", FieldValue.serverTimestamp());

        teamRef.collection("messages").add(message);

        Map<String, Object> meta = new HashMap<>();
        meta.put("lastMessage", text);
        meta.put("lastTimestamp", FieldValue.serverTimestamp());

        teamRef.set(meta, SetOptions.merge());
    }


    public ListenerRegistration listenForDmMessages(
            String otherUserId,
            EventListener<QuerySnapshot> listener
    ) {
        String chatId = dmChatIdFor(currentUid, otherUserId);

        return dmChats()
                .document(chatId)
                .collection("messages")
                .orderBy("createdAt")
                .addSnapshotListener(listener);
    }


    public ListenerRegistration listenForTeamMessages(
            String teamId,
            EventListener<QuerySnapshot> listener
    ) {
        return teams()
                .document(teamId)
                .collection("messages")
                .orderBy("createdAt")
                .addSnapshotListener(listener);
    }


    public ListenerRegistration listenForDmChatList(
            EventListener<QuerySnapshot> listener
    ) {
        return dmChats()
                .whereArrayContains("participants", currentUid)
                .orderBy("lastTimestamp", Query.Direction.DESCENDING)
                .addSnapshotListener(listener);
    }


    public ListenerRegistration listenForTeamChatList(
            List<String> userTeams,
            EventListener<QuerySnapshot> listener
    ) {
        return teams()
                .whereIn(FieldPath.documentId(), userTeams)
                .addSnapshotListener(listener);
    }
}
