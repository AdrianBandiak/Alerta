package com.abandiak.alerta.data.repository;

import android.util.Log;

import com.abandiak.alerta.app.messages.teams.TeamMemberEntry;
import com.abandiak.alerta.data.model.Team;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.*;

import java.util.*;

public class TeamRepository implements TeamRepositoryInterface {

    private final FirebaseFirestore db;
    private final String uid;

    public TeamRepository() {
        this(FirebaseFirestore.getInstance(), FirebaseAuth.getInstance().getUid());
    }

    public TeamRepository(FirebaseFirestore db, String uid) {
        this.db = db;
        this.uid = uid;
    }

    @Override
    public ListenerRegistration listenMyTeams(TeamsListener listener) {
        if (uid == null) return null;

        return db.collection("teams")
                .whereArrayContains("membersIndex", uid)
                .addSnapshotListener((snap, err) -> {

                    if (err != null) {
                        listener.onError(err);
                        return;
                    }

                    if (snap == null) {
                        listener.onSuccess(new ArrayList<>());
                        return;
                    }

                    List<Team> list = new ArrayList<>();

                    for (DocumentSnapshot d : snap.getDocuments()) {
                        Team t = d.toObject(Team.class);
                        if (t != null) {
                            t.setId(d.getId());
                            list.add(t);
                        }
                    }
                    listener.onSuccess(list);
                });
    }

    @Override
    public void createTeam(String name, String desc, int color, String region,
                           CreateTeamCallback callback) {

        if (uid == null) {
            callback.onComplete(false, "Not logged in.");
            return;
        }

        String code = generateTeamCode();
        DocumentReference teamRef = db.collection("teams").document();
        String teamId = teamRef.getId();

        Map<String, Object> data = new HashMap<>();
        data.put("name", name);
        data.put("description", desc);
        data.put("color", color);
        data.put("code", code);
        data.put("region", region);
        data.put("createdBy", uid);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        data.put("createdByName", user != null ? user.getDisplayName() : "");
        data.put("createdAt", Timestamp.now());
        data.put("lastMessage", "");
        data.put("lastTimestamp", null);
        data.put("membersIndex", Collections.singletonList(uid));

        teamRef.set(data)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) throw task.getException();

                    Map<String, Object> member = new HashMap<>();
                    member.put("role", "owner");
                    member.put("joinedAt", Timestamp.now());

                    return teamRef.collection("members")
                            .document(uid)
                            .set(member);
                })
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) throw task.getException();

                    Map<String, Object> map = new HashMap<>();
                    map.put("teamId", teamId);

                    return db.collection("team_codes")
                            .document(code)
                            .set(map);
                })
                .addOnSuccessListener(v -> callback.onComplete(true, teamId))
                .addOnFailureListener(err -> {
                    String msg = (err != null && err.getMessage() != null)
                            ? err.getMessage()
                            : "error";
                    callback.onComplete(false, msg);
                });
    }

    @Override
    public void initializeTeamChat(String teamId) {
        Map<String, Object> msg = new HashMap<>();
        msg.put("text", "Chat created");
        msg.put("senderId", uid);
        msg.put("createdAt", Timestamp.now());

        db.collection("teams")
                .document(teamId)
                .collection("messages")
                .add(msg);
    }

    @Override
    public void updateTeam(Team team, UpdateTeamCallback callback) {

        if (team.getId() == null) {
            callback.onComplete(false, "Missing ID.");
            return;
        }

        Map<String, Object> update = new HashMap<>();
        update.put("name", team.getName());
        update.put("description", team.getDescription());
        update.put("region", team.getRegion());
        update.put("color", team.getColor());

        db.collection("teams")
                .document(team.getId())
                .update(update)
                .addOnSuccessListener(v -> callback.onComplete(true, "OK"))
                .addOnFailureListener(err -> {
                    String msg = (err != null && err.getMessage() != null)
                            ? err.getMessage()
                            : "error";
                    callback.onComplete(false, msg);
                });
    }

    @Override
    public void deleteTeam(String teamId, DeleteTeamCallback cb) {
        db.collection("teams")
                .document(teamId)
                .delete()
                .addOnSuccessListener(v -> cb.onComplete(true))
                .addOnFailureListener(e -> cb.onComplete(false));
    }

    @Override
    public void joinByCode(String code, JoinCallback callback) {

        db.collection("team_codes")
                .document(code)
                .get()
                .addOnSuccessListener(doc -> {

                    if (doc == null || !doc.exists()) {
                        callback.onComplete(false, "Invalid code.");
                        return;
                    }

                    String teamId = doc.getString("teamId");
                    if (teamId == null) {
                        callback.onComplete(false, "Invalid mapping.");
                        return;
                    }

                    DocumentReference teamRef = db.collection("teams")
                            .document(teamId);

                    teamRef.update("membersIndex", FieldValue.arrayUnion(uid))
                            .addOnSuccessListener(v -> {

                                teamRef.collection("members")
                                        .document(uid)
                                        .set(Collections.singletonMap("joinedAt", Timestamp.now()));

                                callback.onComplete(true, "OK");
                            })
                            .addOnFailureListener(err -> {
                                String msg = (err != null && err.getMessage() != null)
                                        ? err.getMessage()
                                        : "error";
                                callback.onComplete(false, msg);
                            });

                })
                .addOnFailureListener(err -> {
                    String msg = (err != null && err.getMessage() != null)
                            ? err.getMessage()
                            : "error";
                    callback.onComplete(false, msg);
                });
    }

    @Override
    public void getFullTeamMembers(String teamId, OnMembersLoaded callback) {

        db.collection("teams")
                .document(teamId)
                .collection("members")
                .get()
                .continueWithTask(task -> {

                    if (!task.isSuccessful() ||
                            task.getResult() == null ||
                            task.getResult().isEmpty()) {

                        callback.onLoaded(new ArrayList<>());
                        return Tasks.forResult(null);
                    }

                    QuerySnapshot snap = task.getResult();

                    List<TeamMemberEntry> result = new ArrayList<>();
                    List<TeamMemberEntry>[] box = new List[]{result};

                    List<Task<Void>> userTasks = new ArrayList<>();

                    for (QueryDocumentSnapshot d : snap) {

                        String userId = d.getId();
                        Timestamp ts = d.getTimestamp("joinedAt");
                        long joinedAt = ts != null ? ts.toDate().getTime() : 0;

                        Task<Void> userTask =
                                db.collection("users")
                                        .document(userId)
                                        .get()
                                        .continueWith(userSnap -> {

                                            DocumentSnapshot userDoc =
                                                    userSnap.isSuccessful()
                                                            ? userSnap.getResult()
                                                            : null;

                                            if (userDoc == null || !userDoc.exists()) {

                                                String safe = userId.length() >= 6
                                                        ? userId.substring(0, 6) + "..."
                                                        : userId + "...";

                                                box[0].add(new TeamMemberEntry(
                                                        userId, safe, "", joinedAt
                                                ));

                                            } else {

                                                String first = userDoc.getString("firstName");
                                                String last = userDoc.getString("lastName");
                                                String avatar = userDoc.getString("photoUrl");

                                                String full = ((first != null ? first : "") + " " +
                                                        (last != null ? last : "")).trim();

                                                if (full.isEmpty()) {
                                                    full = userId.length() >= 6
                                                            ? userId.substring(0, 6) + "..."
                                                            : userId + "...";
                                                }

                                                box[0].add(new TeamMemberEntry(
                                                        userId, full, avatar, joinedAt
                                                ));
                                            }

                                            return null;
                                        });

                        userTasks.add(userTask);
                    }

                    return Tasks.whenAll(userTasks)
                            .continueWith(t -> box[0]);
                })
                .addOnSuccessListener(list -> {

                    if (list == null) return;

                    list.sort(Comparator.comparingLong(TeamMemberEntry::getJoinedAt));
                    callback.onLoaded(list);
                })
                .addOnFailureListener(e -> callback.onLoaded(new ArrayList<>()));
    }

    private String generateTeamCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        Random r = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 6; i++) sb.append(chars.charAt(r.nextInt(chars.length())));
        return sb.toString();
    }

    @Override
    public DocumentReference getTeamRef(String id) {
        return db.collection("teams").document(id);
    }

    @Override
    public String getCurrentUserId() {
        return uid;
    }
}

