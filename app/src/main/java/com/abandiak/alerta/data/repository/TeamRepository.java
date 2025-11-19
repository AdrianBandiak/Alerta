package com.abandiak.alerta.data.repository;

import android.util.Log;

import androidx.annotation.Nullable;

import com.abandiak.alerta.app.messages.teams.TeamMemberEntry;
import com.abandiak.alerta.data.model.Team;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.util.*;

public class TeamRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final String uid = FirebaseAuth.getInstance().getUid();

    public interface TeamsListener {
        void onSuccess(List<Team> list);
        void onError(Exception e);
    }

    public ListenerRegistration listenMyTeams(TeamsListener listener) {
        if (uid == null) return null;

        return db.collection("teams")
                .whereArrayContains("membersIndex", uid)
                .addSnapshotListener((snap, err) -> {

                    if (err != null) {
                        listener.onError(err);
                        return;
                    }
                    if (snap == null) return;

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

    public interface CreateTeamCallback {
        void onComplete(boolean success, String teamIdOrError);
    }

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
        data.put("createdByName", "");
        data.put("createdAt", Timestamp.now());

        data.put("lastMessage", "");
        data.put("lastTimestamp", null);

        data.put("membersIndex", Collections.singletonList(uid));

        teamRef.set(data)
                .continueWithTask(task -> {

                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }

                    Map<String, Object> member = new HashMap<>();
                    member.put("role", "owner");
                    member.put("joinedAt", Timestamp.now());

                    return teamRef.collection("members")
                            .document(uid)
                            .set(member);
                })
                .continueWithTask(task -> {

                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }

                    Map<String, Object> codeMap = new HashMap<>();
                    codeMap.put("teamId", teamId);

                    return db.collection("team_codes")
                            .document(code)
                            .set(codeMap);
                })
                .addOnSuccessListener(aVoid -> callback.onComplete(true, teamId))
                .addOnFailureListener(err -> callback.onComplete(false, err.getMessage()));
    }

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

    public interface UpdateTeamCallback {
        void onComplete(boolean ok, String msg);
    }

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
                .addOnSuccessListener(aVoid -> callback.onComplete(true, "OK"))
                .addOnFailureListener(e -> callback.onComplete(false, e.getMessage()));
    }

    public interface DeleteTeamCallback {
        void onComplete(boolean success);
    }

    public void deleteTeam(String teamId, DeleteTeamCallback cb) {

        db.collection("teams")
                .document(teamId)
                .delete()
                .addOnSuccessListener(v -> cb.onComplete(true))
                .addOnFailureListener(e -> cb.onComplete(false));
    }

    public interface JoinCallback {
        void onComplete(boolean ok, String msg);
    }

    public void joinByCode(String code, JoinCallback callback) {

        db.collection("team_codes")
                .document(code)
                .get()
                .addOnSuccessListener(doc -> {

                    if (!doc.exists()) {
                        callback.onComplete(false, "Invalid code.");
                        return;
                    }

                    String teamId = doc.getString("teamId");
                    if (teamId == null) {
                        callback.onComplete(false, "Invalid mapping.");
                        return;
                    }

                    DocumentReference teamRef = db.collection("teams").document(teamId);

                    teamRef.update("membersIndex", FieldValue.arrayUnion(uid))
                            .addOnSuccessListener(aVoid -> {

                                teamRef.collection("members")
                                        .document(uid)
                                        .set(Collections.singletonMap("joinedAt", Timestamp.now()));

                                callback.onComplete(true, "OK");
                            })
                            .addOnFailureListener(err -> callback.onComplete(false, err.getMessage()));
                })
                .addOnFailureListener(err -> callback.onComplete(false, err.getMessage()));
    }

    public void getFullTeamMembers(String teamId, OnMembersLoaded callback) {

        Log.e("MEMBERS_DEBUG", "------------------------------------------");
        Log.e("MEMBERS_DEBUG", "getFullTeamMembers() CALLED");
        Log.e("MEMBERS_DEBUG", "TEAM ID = " + teamId);
        Log.e("MEMBERS_DEBUG", "Firestore instance = " + db);

        db.collection("teams")
                .document(teamId)
                .collection("members")
                .get()
                .addOnSuccessListener(memberSnap -> {

                    Log.e("MEMBERS_DEBUG", "Members found: " + memberSnap.size());

                    if (memberSnap.isEmpty()) {
                        callback.onLoaded(new ArrayList<>());
                        return;
                    }

                    List<TeamMemberEntry> result = new ArrayList<>();
                    int total = memberSnap.size();

                    for (DocumentSnapshot doc : memberSnap) {

                        String userId = doc.getId();
                        Timestamp ts = doc.getTimestamp("joinedAt");
                        long joinedAt = ts != null ? ts.toDate().getTime() : 0;

                        Log.e("MEMBERS_DEBUG", "------------------------------------------");
                        Log.e("MEMBERS_DEBUG", "Member UID = " + userId);
                        Log.e("MEMBERS_DEBUG", "Fetching from: /users/" + userId);

                        db.collection("users")
                                .document(userId)
                                .get()
                                .addOnSuccessListener(userDoc -> {

                                    if (!userDoc.exists()) {
                                        Log.e("MEMBERS_DEBUG", "!!! USER DOC DOES NOT EXIST !!!");
                                        result.add(new TeamMemberEntry(
                                                userId,
                                                userId.substring(0, 6) + "...",
                                                "",
                                                joinedAt
                                        ));
                                    } else {

                                        Log.e("MEMBERS_DEBUG", "UserDoc EXISTS");

                                        String first = userDoc.getString("firstName");
                                        String last = userDoc.getString("lastName");
                                        String avatar = userDoc.getString("photoUrl");

                                        Log.e("MEMBERS_DEBUG", "firstName = " + first);
                                        Log.e("MEMBERS_DEBUG", "lastName  = " + last);
                                        Log.e("MEMBERS_DEBUG", "avatar    = " + avatar);

                                        String fullName = "";

                                        if ((first != null && !first.isEmpty()) ||
                                                (last != null && !last.isEmpty())) {
                                            fullName = ((first != null ? first : "") + " " +
                                                    (last != null ? last : "")).trim();
                                        } else {
                                            fullName = userId.substring(0, 6) + "...";
                                        }

                                        result.add(new TeamMemberEntry(
                                                userId,
                                                fullName,
                                                avatar,
                                                joinedAt
                                        ));
                                    }

                                    if (result.size() == total) {
                                        result.sort(Comparator.comparingLong(TeamMemberEntry::getJoinedAt));
                                        Log.e("MEMBERS_DEBUG", "ALL LOADED → CALLBACK");
                                        callback.onLoaded(result);
                                    }
                                })

                                .addOnFailureListener(e -> {
                                    Log.e("MEMBERS_DEBUG", "ERROR fetching user " + userId + ": " + e);

                                    result.add(new TeamMemberEntry(
                                            userId,
                                            userId.substring(0, 6) + "...",
                                            "",
                                            joinedAt
                                    ));

                                    if (result.size() == total) {
                                        result.sort(Comparator.comparingLong(TeamMemberEntry::getJoinedAt));
                                        callback.onLoaded(result);
                                    }
                                });
                    }
                })

                .addOnFailureListener(e -> {
                    Log.e("MEMBERS_DEBUG", "ERROR → Cannot read members subcollection: ", e);
                    callback.onLoaded(new ArrayList<>());
                });
    }


    public interface OnMembersLoaded {
        void onLoaded(List<TeamMemberEntry> list);
    }

    private String generateTeamCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        Random r = new Random();
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < 6; i++) {
            sb.append(chars.charAt(r.nextInt(chars.length())));
        }
        return sb.toString();
    }

    public DocumentReference getTeamRef(String teamId) {
        return db.collection("teams").document(teamId);
    }
}
