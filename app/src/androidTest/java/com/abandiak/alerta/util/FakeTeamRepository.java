package com.abandiak.alerta.util;

import com.abandiak.alerta.app.messages.teams.TeamMemberEntry;
import com.abandiak.alerta.data.model.Team;
import com.abandiak.alerta.data.repository.TeamRepositoryInterface;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.*;

public class FakeTeamRepository implements TeamRepositoryInterface {

    private static List<Team> teams = new ArrayList<>();
    private static Map<String, List<TeamMemberEntry>> members = new HashMap<>();
    private static Map<String, String> codeMap = new HashMap<>();

    private TeamsListener activeListener = null;


    public static void reset() {
        teams.clear();
        members.clear();
        codeMap.clear();
    }

    public static void setTeams(List<Team> list) {
        teams = new ArrayList<>();

        for (Team t : list) {
            if (t.getId() == null || t.getId().isEmpty()) {
                t.setId(UUID.randomUUID().toString());
            }
            teams.add(t);
        }
    }


    public static void setMembers(String teamId, List<TeamMemberEntry> list) {
        members.put(teamId, new ArrayList<>(list));
    }

    public static void setCodeMapping(String code, String teamId) {
        codeMap.put(code, teamId);
    }


    @Override
    public ListenerRegistration listenMyTeams(TeamsListener listener) {

        this.activeListener = listener;

        listener.onSuccess(new ArrayList<>(teams));

        return () -> activeListener = null;
    }

    private void notifyChange() {
        if (activeListener != null) {
            activeListener.onSuccess(new ArrayList<>(teams));
        }
    }

    @Override
    public void createTeam(String name, String desc, int color, String region, CreateTeamCallback cb) {

        String id = UUID.randomUUID().toString();

        Team t = new Team();
        t.setId(id);
        t.setName(name);
        t.setDescription(desc);
        t.setRegion(region);
        t.setColor(color);
        t.setCreatedAt(Timestamp.now());
        t.setCreatedBy("TEST_USER");
        t.setMembersIndex(new ArrayList<>(Collections.singletonList("TEST_USER")));

        teams.add(t);

        codeMap.put("ABCDEF", id);

        notifyChange();

        cb.onComplete(true, id);
    }


    @Override
    public void updateTeam(Team team, UpdateTeamCallback cb) {
        cb.onComplete(true, "OK");
        notifyChange();
    }

    @Override
    public void deleteTeam(String teamId, DeleteTeamCallback cb) {
        teams.removeIf(t -> t.getId().equals(teamId));
        notifyChange();
        cb.onComplete(true);
    }

    @Override
    public void joinByCode(String code, JoinCallback cb) {
        String teamId = codeMap.get(code);

        if (teamId == null) {
            cb.onComplete(false, "Invalid code.");
            return;
        }

        for (Team t : teams) {
            if (t.getId().equals(teamId)) {

                if (t.getMembersIndex() == null) {
                    t.setMembersIndex(new ArrayList<>());
                }

                List<String> list = t.getMembersIndex();
                if (!list.contains("TEST_USER")) list.add("TEST_USER");
            }
        }
        cb.onComplete(true, "OK");
    }

    @Override
    public void getFullTeamMembers(String teamId, OnMembersLoaded cb) {
        cb.onLoaded(members.getOrDefault(teamId, new ArrayList<>()));
    }

    @Override
    public String getCurrentUserId() {
        return "TEST_USER";
    }

    @Override
    public void initializeTeamChat(String teamId) {
    }

    @Override
    public DocumentReference getTeamRef(String id) {
        return null;
    }
}
