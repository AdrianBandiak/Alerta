package com.abandiak.alerta.data.repository;

import androidx.annotation.Nullable;

import com.abandiak.alerta.data.model.Incident;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
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
                .addOnFailureListener(e -> {
                    inner[0] = listenWithAudTokens(buildTokens(null), type, regionBucket, region, listener);
                });

        return proxy(inner);
    }


    public ListenerRegistration listenIncidents(
            @Nullable String region,
            @Nullable String type,
            EventListener<QuerySnapshot> listener
    ) {
        return listenVisibleIncidentsForCurrentUser(type, /*regionBucket*/ null, region, listener);
    }

    public Task<DocumentReference> createIncident(Map<String, Object> data) {
        return db.collection("incidents").add(data);
    }

    public Task<DocumentReference> createIncident(Incident incident) {
        return createIncident(incident.toMap());
    }


    private static ListenerRegistration proxy(final ListenerRegistration[] ref) {
        return new ListenerRegistration() {
            @Override
            public void remove() {
                if (ref[0] != null) {
                    ref[0].remove();
                    ref[0] = null;
                }
            }
        };
    }

    private static List<String> extractTeamIds(@Nullable DocumentSnapshot snap) {
        List<String> teamIds = new ArrayList<>();
        if (snap != null && snap.exists()) {
            Object raw = snap.get("teamIds");
            if (raw instanceof List) {
                for (Object o : (List<?>) raw) {
                    if (o != null) teamIds.add(String.valueOf(o));
                }
            }
        }
        return teamIds;
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
            @Nullable String region,
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
        if (region != null && !region.isEmpty()) {
            q = q.whereEqualTo("region", region);
        }

        return q.addSnapshotListener(listener);
    }

    public static String geohashPrefix(@Nullable String geohash, int len) {
        if (geohash == null) return null;
        return geohash.length() <= len ? geohash : geohash.substring(0, len);
    }
}
