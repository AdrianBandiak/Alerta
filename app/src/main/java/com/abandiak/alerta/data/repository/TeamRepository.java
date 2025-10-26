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
    private DocumentReference teamDoc(String id){ return teams().document(id); }

    private DocumentReference codeDoc(String code){ return db.collection("team_codes").document(code); }

    public interface TeamsListener { void onSuccess(List<Team> list); void onError(Exception e); }
    public interface SimpleCallback { void onResult(boolean ok, String message); }

    public ListenerRegistration listenMyTeams(TeamsListener l){
        return teams()
                .whereArrayContains("membersIndex", uid)
                .addSnapshotListener((snap, e)->{
                    if(e!=null){ l.onError(e); return; }
                    l.onSuccess(snap==null? Collections.emptyList() : snap.toObjects(Team.class));
                });
    }

    public void getMyTeams(TeamsListener l){
        teams().whereArrayContains("membersIndex", uid)
                .get().addOnSuccessListener(s -> l.onSuccess(s.toObjects(Team.class)))
                .addOnFailureListener(l::onError);
    }

    public void createTeam(String name, String desc, SimpleCallback cb){
        if(uid==null){ cb.onResult(false,"Not signed in"); return; }
        String id = UUID.randomUUID().toString();
        String code = genCode();
        long now = System.currentTimeMillis();
        Team t = new Team(id, name, desc, code, uid, now);

        WriteBatch batch = db.batch();
        DocumentReference team = teamDoc(id);
        batch.set(team, t);
        batch.update(team, "membersIndex", Arrays.asList(uid));

        batch.set(team.collection("members").document(uid), new HashMap<String,Object>(){{
            put("role","owner"); put("joinedAt", now);
        }});

        batch.set(codeDoc(code), new HashMap<String, Object>(){{ put("teamId", id); }});

        batch.commit()
                .addOnSuccessListener(v-> cb.onResult(true,"Team created"))
                .addOnFailureListener(e-> { Log.e("TEAM_CREATE","",e); cb.onResult(false,"Create failed"); });
    }

    public void joinByCode(String rawCode, SimpleCallback cb){
        if(uid==null){ cb.onResult(false,"Not signed in"); return; }
        String code = rawCode.trim().toUpperCase(Locale.ROOT);
        codeDoc(code).get().addOnSuccessListener(doc -> {
            if(!doc.exists()){ cb.onResult(false,"Invalid code"); return; }
            String teamId = doc.getString("teamId");
            if(teamId==null){ cb.onResult(false,"Invalid code"); return; }

            DocumentReference team = teamDoc(teamId);
            long now = System.currentTimeMillis();

            db.runTransaction(tr -> {
                        tr.set(team.collection("members").document(uid),
                                new HashMap<String,Object>(){{ put("role","member"); put("joinedAt", now); }}, SetOptions.merge());
                        List<String> idx = (List<String>) tr.get(team).get("membersIndex");
                        if(idx==null) idx = new ArrayList<>();
                        if(!idx.contains(uid)) { idx.add(uid); tr.update(team, "membersIndex", idx); }
                        return null;
                    }).addOnSuccessListener(v-> cb.onResult(true,"Joined"))
                    .addOnFailureListener(e-> cb.onResult(false,"Join failed"));
        }).addOnFailureListener(e-> cb.onResult(false,"Join failed"));
    }

    private String genCode(){
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder sb = new StringBuilder(6);
        Random r = ThreadLocalRandom.current();
        for(int i=0;i<6;i++) sb.append(chars.charAt(r.nextInt(chars.length())));
        return sb.toString();
    }

    public String currentUid(){ return uid; }
}
