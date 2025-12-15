package com.abandiak.alerta.data.repository;

import android.util.Log;

import androidx.annotation.Nullable;

import com.abandiak.alerta.data.model.Incident;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IncidentRepository {

    private final FirebaseFirestore db;

    public IncidentRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    public IncidentRepository(FirebaseFirestore db) {
        this.db = db;
    }

    public Task<DocumentReference> createIncident(Map<String, Object> data) {
        return db.collection("incidents").add(data);
    }

    public Task<DocumentReference> createIncident(Incident incident) {
        return createIncident(incident.toMap());
    }

    public Task<Void> updateIncidentVerification(String id, boolean verified) {

        String userId = (FirebaseAuth.getInstance().getCurrentUser() != null)
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : "unknown";

        Map<String, Object> update = new HashMap<>();
        update.put("verified", verified);
        update.put("verifiedBy", verified ? userId : null);

        Map<String, Object> log = new HashMap<>();
        log.put("timestamp", System.currentTimeMillis());
        log.put("action", verified ? "Verified by authorities" : "Verification removed");

        DocumentReference doc = db.collection("incidents").document(id);

        return doc.update(update)
                .continueWithTask(t -> {
                    if (!t.isSuccessful()) {
                        return t;
                    }
                    return doc.update("logs", FieldValue.arrayUnion(log));
                });
    }

    public Task<Void> addIncidentLog(String id, String action) {
        Map<String, Object> log = new HashMap<>();
        log.put("timestamp", System.currentTimeMillis());
        log.put("action", action);

        DocumentReference doc = db.collection("incidents").document(id);
        return doc.update("logs", FieldValue.arrayUnion(log));
    }

    public Task<Void> deleteIncident(String id) {
        return db.collection("incidents").document(id).delete();
    }

    public ListenerRegistration listenVisibleIncidentsForCurrentUser(
            @Nullable String type,
            @Nullable String regionBucket,
            @Nullable String region,
            EventListener<QuerySnapshot> listener
    ) {
        final ListenerRegistration[] holder = new ListenerRegistration[1];
        String uid = FirebaseAuth.getInstance().getUid();

        if (uid == null) {
            Log.w("MAP_DEBUG", "User not signed in, using public only.");
            holder[0] = listenWithAudTokens(buildTokens(null, null), type, regionBucket, region, listener);
            return proxy(holder);
        }

        db.collection("teams")
                .whereArrayContains("membersIndex", uid)
                .get()
                .addOnSuccessListener(snap -> {

                    List<String> teamIds = new ArrayList<>();
                    for (DocumentSnapshot d : snap.getDocuments()) {
                        teamIds.add(d.getId());
                    }

                    List<String> tokens = buildTokens(uid, teamIds);
                    holder[0] = listenWithAudTokens(tokens, type, regionBucket, region, listener);

                })
                .addOnFailureListener(e -> {

                    Log.e("MAP_DEBUG", "Failed to load team IDs: " + e.getMessage());
                    List<String> tokens = buildTokens(uid, null);
                    holder[0] = listenWithAudTokens(tokens, type, regionBucket, region, listener);

                });

        return proxy(holder);
    }

    private static ListenerRegistration proxy(final ListenerRegistration[] ref) {
        return () -> {
            if (ref[0] != null) {
                ref[0].remove();
                ref[0] = null;
            }
        };
    }

    private List<String> buildTokens(@Nullable String uid, @Nullable List<String> teamIds) {
        List<String> tokens = new ArrayList<>();
        tokens.add("public");
        if (uid != null) tokens.add(uid);
        if (teamIds != null) tokens.addAll(teamIds);

        return tokens.size() > 10
                ? new ArrayList<>(tokens.subList(0, 10))
                : tokens;
    }

    private ListenerRegistration listenWithAudTokens(
            List<String> tokens,
            @Nullable String type,
            @Nullable String regionBucket,
            @Nullable String region,
            EventListener<QuerySnapshot> listener
    ) {
        Log.d("MAP_DEBUG", "Listening with tokens: " + tokens);

        Query q = db.collection("incidents")
                .whereArrayContainsAny("aud", tokens)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(200);

        if (type != null && !"ALL".equalsIgnoreCase(type))
            q = q.whereEqualTo("type", type);

        if (regionBucket != null)
            q = q.whereEqualTo("regionBucket", regionBucket);

        if (region != null)
            q = q.whereEqualTo("region", region);

        return q.addSnapshotListener(listener);
    }
}
