package com.abandiak.alerta.data.repository;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;
import com.google.firebase.firestore.EventListener;

import java.util.*;

public class ChatRepository {

    private final FirebaseFirestore db;

    public ChatRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    public ChatRepository(FirebaseFirestore firestore) {
        this.db = firestore;
    }

    private String getCurrentUid() {
        return FirebaseAuth.getInstance().getUid();
    }

    private CollectionReference dmChats() {
        return db.collection("dm_chats");
    }

    private CollectionReference teams() {
        return db.collection("teams");
    }

    public String dmChatIdFor(String userA, String userB) {
        List<String> sorted = Arrays.asList(userA, userB);
        Collections.sort(sorted);
        return sorted.get(0) + "_" + sorted.get(1);
    }

    public void sendDirectMessage(String otherUserId, String text, OnDmCreated callback) {

        String currentUid = getCurrentUid();
        Log.e("DM_DEBUG", "sendDirectMessage() START");
        Log.e("DM_DEBUG", "currentUid=" + currentUid + "  otherUserId=" + otherUserId);

        if (currentUid == null) {
            Log.e("DM_DEBUG", "ERROR: currentUid is NULL");
            return;
        }
        if (otherUserId == null) {
            Log.e("DM_DEBUG", "ERROR: otherUserId is NULL");
            return;
        }

        String chatId = dmChatIdFor(currentUid, otherUserId);
        Log.e("DM_DEBUG", "chatId=" + chatId);

        DocumentReference chatRef = dmChats().document(chatId);

        Log.e("DM_DEBUG", "Calling chatRef.get()...");

        chatRef.get()
                .addOnSuccessListener(doc -> {

                    Log.e("DM_DEBUG", "chatRef.get() SUCCESS doc.exists=" + doc.exists());

                    boolean existedBefore = doc.exists();

                    Map<String, Object> meta = new HashMap<>();
                    meta.put("participants", Arrays.asList(currentUid, otherUserId));
                    meta.put("lastMessage", text);
                    meta.put("lastTimestamp", FieldValue.serverTimestamp());

                    Log.e("DM_DEBUG", "META = " + meta);

                    Log.e("DM_DEBUG", "Calling chatRef.set(meta)...");

                    chatRef.set(meta)
                            .addOnSuccessListener(unused -> {
                                Log.e("DM_DEBUG", "chatRef.set() SUCCESS");

                                Map<String, Object> message = new HashMap<>();
                                message.put("senderId", currentUid);
                                message.put("text", text);
                                message.put("createdAt", FieldValue.serverTimestamp());

                                Log.e("DM_DEBUG", "Adding message to messages/...");

                                chatRef.collection("messages")
                                        .add(message)
                                        .addOnSuccessListener(msg -> Log.e("DM_DEBUG", "MESSAGE ADDED SUCCESS: id=" + msg.getId()))
                                        .addOnFailureListener(e -> Log.e("DM_DEBUG", "ERROR adding message: " + e.getMessage(), e));

                                if (callback != null) callback.onCreated(existedBefore);
                            })
                            .addOnFailureListener(e -> Log.e("DM_DEBUG", "ERROR chatRef.set(): " + e.getMessage(), e));

                })
                .addOnFailureListener(e -> Log.e("DM_DEBUG", "ERROR chatRef.get(): " + e.getMessage(), e));
    }

    public interface OnDmCreated {
        void onCreated(boolean existedBefore);
    }

    public void sendTeamMessage(String teamId, String text,
                                String senderId, String senderName, String senderAvatar) {

        Map<String, Object> msg = new HashMap<>();
        msg.put("senderId", senderId);
        msg.put("senderName", senderName);
        msg.put("senderAvatar", senderAvatar);
        msg.put("text", text);
        msg.put("createdAt", FieldValue.serverTimestamp());

        db.collection("teams")
                .document(teamId)
                .collection("messages")
                .add(msg);

        db.collection("teams")
                .document(teamId)
                .update(
                        "lastMessage", text,
                        "lastTimestamp", FieldValue.serverTimestamp()
                );
    }

    public ListenerRegistration listenForDmMessages(
            String otherUserId,
            EventListener<QuerySnapshot> listener
    ) {
        String currentUid = getCurrentUid();
        if (currentUid == null) return null;

        String chatId = dmChatIdFor(currentUid, otherUserId);

        return dmChats()
                .document(chatId)
                .collection("messages")
                .orderBy("createdAt")
                .addSnapshotListener(listener);
    }

    public ListenerRegistration listenForDmChatList(
            EventListener<QuerySnapshot> listener
    ) {
        String currentUid = getCurrentUid();
        if (currentUid == null) return null;

        return dmChats()
                .whereArrayContains("participants", currentUid)
                .orderBy("lastTimestamp", Query.Direction.DESCENDING)
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

    public ListenerRegistration listenForTeamChatList(
            List<String> userTeams,
            EventListener<QuerySnapshot> listener
    ) {
        return teams()
                .whereIn(FieldPath.documentId(), userTeams)
                .addSnapshotListener(listener);
    }
}
