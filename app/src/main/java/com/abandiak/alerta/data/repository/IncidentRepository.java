package com.abandiak.alerta.data.repository;

import androidx.annotation.Nullable;
import com.abandiak.alerta.data.model.Incident;
import com.google.firebase.firestore.EventListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;
import java.util.*;

public class IncidentRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public Task<DocumentReference> createIncident(Map<String, Object> data) {
        return db.collection("incidents").add(data);
    }

    public ListenerRegistration listenIncidents(
            @Nullable String region,
            @Nullable String type,
            EventListener<QuerySnapshot> listener
    ) {
        return listenVisibleIncidentsForCurrentUser(
                type,
                /* regionBucket */ null,
                region,
                listener
        );
    }

    public Task<DocumentReference> createIncident(Incident incident) {
        return createIncident(incident.toMap());
    }

    public Task<Void> updateIncidentVerification(String id, boolean verified) {
        String userId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "unknown";
        Map<String, Object> update = new HashMap<>();
        update.put("verified", verified);
        update.put("verifiedBy", verified ? userId : null);

        Map<String, Object> log = new HashMap<>();
        log.put("timestamp", System.currentTimeMillis());
        log.put("action", verified ? "Verified by authorities" : "Verification removed");

        DocumentReference doc = db.collection("incidents").document(id);
        return doc.update(update)
                .continueWithTask(t -> doc.update("logs", FieldValue.arrayUnion(log)));
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
        final ListenerRegistration[] inner = new ListenerRegistration[1];
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) {
            inner[0] = listenWithAudTokens(buildTokens(null), type, regionBucket, region, listener);
            return proxy(inner);
        }
        db.collection("users").document(uid).get()
                .addOnSuccessListener(snap -> {
                    List<String> teamIds = extractTeamIds(snap);
                    inner[0] = listenWithAudTokens(buildTokens(teamIds), type, regionBucket, region, listener);
                })
                .addOnFailureListener(e ->
                        inner[0] = listenWithAudTokens(buildTokens(null), type, regionBucket, region, listener));
        return proxy(inner);
    }

    private static ListenerRegistration proxy(final ListenerRegistration[] ref) {
        return new ListenerRegistration() {
            @Override public void remove() {
                if (ref[0] != null) { ref[0].remove(); ref[0] = null; }
            }
        };
    }

    private static List<String> extractTeamIds(@Nullable DocumentSnapshot snap) {
        List<String> teamIds = new ArrayList<>();
        if (snap != null && snap.exists()) {
            Object raw = snap.get("teamIds");
            if (raw instanceof List) {
                for (Object o : (List<?>) raw) if (o != null) teamIds.add(String.valueOf(o));
            }
        }
        return teamIds;
    }

    private List<String> buildTokens(@Nullable List<String> userTeamTokens) {
        List<String> tokens = new ArrayList<>();
        tokens.add("public");
        if (userTeamTokens != null) tokens.addAll(userTeamTokens);
        return tokens.size() > 10 ? new ArrayList<>(tokens.subList(0, 10)) : tokens;
    }

    private ListenerRegistration listenWithAudTokens(
            List<String> tokens, @Nullable String type,
            @Nullable String regionBucket, @Nullable String region,
            EventListener<QuerySnapshot> listener) {

        Query q = db.collection("incidents")
                .whereArrayContainsAny("aud", tokens)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(200);

        if (type != null && !"ALL".equalsIgnoreCase(type)) q = q.whereEqualTo("type", type);
        if (regionBucket != null) q = q.whereEqualTo("regionBucket", regionBucket);
        if (region != null) q = q.whereEqualTo("region", region);
        return q.addSnapshotListener(listener);
    }

}
