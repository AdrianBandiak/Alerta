package com.abandiak.alerta.data.repository;

import com.abandiak.alerta.app.messages.teams.TeamMemberEntry;
import com.abandiak.alerta.data.model.Team;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.List;

public interface TeamRepositoryInterface {

    ListenerRegistration listenMyTeams(TeamsListener listener);

    void createTeam(String name, String desc, int color, String region, CreateTeamCallback cb);

    void updateTeam(Team team, UpdateTeamCallback cb);

    void deleteTeam(String teamId, DeleteTeamCallback cb);

    void joinByCode(String code, JoinCallback cb);

    void getFullTeamMembers(String teamId, OnMembersLoaded cb);

    void initializeTeamChat(String teamId);
    DocumentReference getTeamRef(String id);


    String getCurrentUserId();

    interface TeamsListener {
        void onSuccess(List<Team> list);
        void onError(Exception e);
    }

    interface CreateTeamCallback {
        void onComplete(boolean success, String result);
    }

    interface UpdateTeamCallback {
        void onComplete(boolean ok, String msg);
    }

    interface DeleteTeamCallback {
        void onComplete(boolean success);
    }

    interface JoinCallback {
        void onComplete(boolean ok, String msg);
    }

    interface OnMembersLoaded {
        void onLoaded(List<TeamMemberEntry> list);
    }
}

