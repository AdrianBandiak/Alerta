package com.abandiak.alerta.app.messages.teams;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.abandiak.alerta.R;
import com.abandiak.alerta.data.model.Team;
import com.abandiak.alerta.data.repository.TeamRepository;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class MessagesTeamsFragment extends Fragment {

    private RecyclerView recyclerView;
    private TeamChatsAdapter adapter;
    private TeamRepository teamRepo;

    private ListenerRegistration listenerReg;

    public MessagesTeamsFragment() {}

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_messages_teams, container, false);

        recyclerView = view.findViewById(R.id.recyclerTeamChats);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new TeamChatsAdapter();
        adapter.setOnTeamChatClickListener(chat -> {
            Intent i = new Intent(getContext(), TeamChatActivity.class);
            i.putExtra("teamId", chat.getTeamId());
            startActivity(i);
        });

        recyclerView.setAdapter(adapter);

        teamRepo = new TeamRepository();
        listenForTeams();

        return view;
    }

    private void listenForTeams() {
        listenerReg = teamRepo.listenMyTeams(new TeamRepository.TeamsListener() {
            @Override
            public void onSuccess(List<Team> list) {

                List<TeamChatEntry> chats = new ArrayList<>();

                for (Team t : list) {

                    long ts = t.getLastTimestamp() > 0
                            ? t.getLastTimestamp()
                            : 0;

                    TeamChatEntry entry = new TeamChatEntry(
                            t.getId(),
                            t.getName(),
                            t.getLastMessage() == null ? "" : t.getLastMessage(),
                            ts,
                            t.getColor()
                    );


                    chats.add(entry);
                }

                adapter.submit(chats);
            }

            @Override
            public void onError(Exception e) {
                Log.e("TeamsChats", "Error loading teams", e);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (listenerReg != null) {
            listenerReg.remove();
        }
    }
}
