package com.abandiak.alerta.data.repository;

import android.util.Log;

import com.abandiak.alerta.data.model.Team;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class TeamRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final String uid = FirebaseAuth.getInstance().getUid();

    private CollectionReference teams() { return db.collection("teams"); }
    private DocumentReference teamDoc(String id) { return teams().document(id); }
    private DocumentReference codeDoc(String code) { return db.collection("team_codes").document(code); }

    public interface TeamsListener { void onSuccess(List<Team> list); void onError(Exception e); }
    public interface SimpleCallback { void onResult(boolean ok, String messageOrId); }

    public ListenerRegistration listenMyTeams(TeamsListener l) {
        assert uid != null;
        return teams()
                .whereArrayContains("membersIndex", uid)
                .addSnapshotListener((snap, e) -> {
                    if (e != null) {
                        l.onError(e);
                        return;
                    }
                    l.onSuccess(
                            snap == null
                                    ? Collections.emptyList()
                                    : snap.toObjects(Team.class)
                    );
                });
    }

    public void getMyTeams(TeamsListener l) {
        assert uid != null;
        teams()
                .whereArrayContains("membersIndex", uid)
                .get()
                .addOnSuccessListener(s -> l.onSuccess(s.toObjects(Team.class)))
                .addOnFailureListener(l::onError);
    }

    public void initializeTeamChat(String teamId) {

        DocumentReference ref = teamDoc(teamId);

        Map<String, Object> fields = new HashMap<>();
        fields.put("lastMessage", "");
        fields.put("lastTimestamp", 0);

        ref.set(fields, SetOptions.merge());

        ref.collection("messages")
                .document("init")
                .set(new HashMap<String, Object>() {{
                    put("system", "Chat initialized");
                    put("createdAt", FieldValue.serverTimestamp());
                }});
    }

    public void createTeam(String name, String desc, int color, String region, SimpleCallback cb) {
        if (uid == null) {
            cb.onResult(false, "Not signed in");
            return;
        }

        String id = UUID.randomUUID().toString();
        String code = genCode();
        long now = System.currentTimeMillis();

        db.collection("users").document(uid)
                .get()
                .addOnSuccessListener(userDoc -> {

                    String firstName = userDoc.getString("firstName");
                    String lastName = userDoc.getString("lastName");
                    String fullName = ((firstName != null ? firstName : "") + " " +
                            (lastName != null ? lastName : "")).trim();

                    Team t = new Team(id, name, desc, code, uid, fullName, now, color, region);

                    WriteBatch batch = db.batch();
                    DocumentReference team = teamDoc(id);

                    batch.set(team, t);

                    batch.update(team, "membersIndex", Arrays.asList(uid));

                    batch.set(team.collection("members").document(uid),
                            new HashMap<String, Object>() {{
                                put("role", "owner");
                                put("joinedAt", now);
                            }});

                    batch.set(codeDoc(code),
                            new HashMap<String, Object>() {{
                                put("teamId", id);
                            }});

                    batch.commit()
                            .addOnSuccessListener(v -> {
                                cb.onResult(true, id);
                            })
                            .addOnFailureListener(e -> {
                                Log.e("TEAM_CREATE", "", e);
                                cb.onResult(false, "Create failed");
                            });

                })
                .addOnFailureListener(e -> {
                    Log.e("TEAM_CREATE", "Failed to get user profile", e);
                    cb.onResult(false, "Failed to get user profile");
                });
    }

    public void joinByCode(String rawCode, SimpleCallback cb) {
        if (uid == null) {
            cb.onResult(false, "Not signed in");
            return;
        }

        String code = rawCode.trim().toUpperCase(Locale.ROOT);

        codeDoc(code).get().addOnSuccessListener(doc -> {
            if (!doc.exists()) {
                cb.onResult(false, "Invalid code");
                return;
            }

            String teamId = doc.getString("teamId");
            if (teamId == null) {
                cb.onResult(false, "Invalid code");
                return;
            }

            DocumentReference team = teamDoc(teamId);
            long now = System.currentTimeMillis();

            db.runTransaction(tr -> {
                        DocumentSnapshot snapshot = tr.get(team);
                        if (!snapshot.exists())
                            throw new FirebaseFirestoreException("Team not found",
                                    FirebaseFirestoreException.Code.NOT_FOUND);

                        List<String> idx = (List<String>) snapshot.get("membersIndex");
                        if (idx == null) idx = new ArrayList<>();

                        if (idx.contains(uid))
                            throw new FirebaseFirestoreException("Already in team",
                                    FirebaseFirestoreException.Code.ABORTED);

                        tr.set(team.collection("members").document(uid),
                                new HashMap<String, Object>() {{
                                    put("role", "member");
                                    put("joinedAt", now);
                                }}, SetOptions.merge());

                        idx.add(uid);
                        tr.update(team, "membersIndex", idx);

                        return null;
                    })
                    .addOnSuccessListener(v -> cb.onResult(true, teamId))
                    .addOnFailureListener(e -> {
                        if (e.getMessage() != null && e.getMessage().contains("Already"))
                            cb.onResult(false, "You are already in this team");
                        else
                            cb.onResult(false, "Join failed");
                    });

        }).addOnFailureListener(e -> cb.onResult(false, "Join failed"));
    }

    private String genCode() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder sb = new StringBuilder(6);
        Random r = ThreadLocalRandom.current();
        for (int i = 0; i < 6; i++)
            sb.append(chars.charAt(r.nextInt(chars.length())));
        return sb.toString();
    }

    public void updateTeam(Team team, SimpleCallback cb) {
        teamDoc(team.getId())
                .set(team, SetOptions.merge())
                .addOnSuccessListener(v -> cb.onResult(true, "Updated"))
                .addOnFailureListener(e -> cb.onResult(false, "Update failed"));
    }

    public void deleteTeam(String teamId, java.util.function.Consumer<Boolean> cb) {
        teamDoc(teamId)
                .delete()
                .addOnSuccessListener(v -> cb.accept(true))
                .addOnFailureListener(e -> cb.accept(false));
    }

    public String currentUid() {
        return uid;
    }
}
