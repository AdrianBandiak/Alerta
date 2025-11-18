package com.abandiak.alerta.app.messages;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.abandiak.alerta.R;
import com.abandiak.alerta.app.messages.teams.TeamChatActivity;
import com.abandiak.alerta.app.messages.teams.TeamChatEntry;
import com.abandiak.alerta.app.messages.teams.TeamChatsAdapter;
import com.abandiak.alerta.data.model.Team;
import com.abandiak.alerta.data.repository.TeamRepository;
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
        recyclerView.setAdapter(adapter);

        adapter.setOnTeamChatClickListener(chat -> {
            Intent i = new Intent(getContext(), TeamChatActivity.class);
            i.putExtra("teamId", chat.getTeamId());
            startActivity(i);
        });

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

                    long lastTs = t.getLastTimestampMillis();
                    long createdTs = t.getCreatedAtMillis();

                    long finalTs = lastTs > 0 ? lastTs : createdTs;

                    String lastMsg = t.getLastMessage();
                    if (lastMsg == null || lastMsg.trim().isEmpty()) {
                        lastMsg = "No messages yet";
                    }

                    chats.add(new TeamChatEntry(
                            t.getId(),
                            t.getName(),
                            lastMsg,
                            finalTs,
                            t.getColor()
                    ));
                }

                chats.sort((a, b) -> Long.compare(b.getLastTimestamp(), a.getLastTimestamp()));

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
