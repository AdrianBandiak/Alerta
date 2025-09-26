package com.abandiak.alerta.data.repository;

import androidx.annotation.Nullable;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class IncidentRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public ListenerRegistration listenVisibleIncidentsForCurrentUser(
            @Nullable String type,
            @Nullable String regionBucket,
            EventListener<QuerySnapshot> listener
    ) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) {
            return listenWithAudTokens(buildTokens(null), type, regionBucket, listener);
        }

        Task<DocumentSnapshot> t = db.collection("users").document(uid).get();
        t.addOnSuccessListener(snap -> {
            List<String> teamIds = new ArrayList<>();
            if (snap != null && snap.exists()) {
                Object raw = snap.get("teamIds");
                if (raw instanceof List) {
                    for (Object o : (List<?>) raw) {
                        if (o != null) teamIds.add(String.valueOf(o));
                    }
                }
            }
            listenWithAudTokens(buildTokens(teamIds), type, regionBucket, listener);
        }).addOnFailureListener(e -> {
            listenWithAudTokens(buildTokens(null), type, regionBucket, listener);
        });

        return null;
    }

    public ListenerRegistration listenIncidents(
            @Nullable String region,
            @Nullable String type,
            EventListener<QuerySnapshot> listener
    ) {
        return listenVisibleIncidentsForCurrentUser(type, null, listener);
    }

    private List<String> buildTokens(@Nullable List<String> userTeamTokens) {
        List<String> tokens = new ArrayList<>();
        tokens.add("public");
        if (userTeamTokens != null) tokens.addAll(userTeamTokens);
        if (tokens.size() > 10) {
            return new ArrayList<>(tokens.subList(0, 10));
        }
        return tokens;
    }

    private ListenerRegistration listenWithAudTokens(
            List<String> tokens,
            @Nullable String type,
            @Nullable String regionBucket,
            EventListener<QuerySnapshot> listener
    ) {
        Query q = db.collection("incidents")
                .whereArrayContainsAny("aud", tokens)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(200);

        if (type != null && !type.isEmpty() && !"ALL".equalsIgnoreCase(type)) {
            q = q.whereEqualTo("type", type);
        }
        if (regionBucket != null && !regionBucket.isEmpty()) {
            q = q.whereEqualTo("regionBucket", regionBucket);
        }

        return q.addSnapshotListener(listener);
    }

    public static String geohashPrefix(String geohash, int len) {
        if (geohash == null) return null;
        return geohash.length() <= len ? geohash : geohash.substring(0, len);
    }

    public Task<com.google.firebase.firestore.DocumentReference> createIncident(Map<String, Object> data) {
        return db.collection("incidents").add(data);
    }
}

