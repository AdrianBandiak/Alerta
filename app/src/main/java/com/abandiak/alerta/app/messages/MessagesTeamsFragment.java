package com.abandiak.alerta.app.messages;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.abandiak.alerta.R;

import java.util.ArrayList;
import java.util.List;

public class MessagesTeamsFragment extends Fragment {

    private RecyclerView recyclerView;
    private TeamChatsAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_messages_teams, container, false);

        recyclerView = view.findViewById(R.id.recyclerTeamsChats);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new TeamChatsAdapter();
        recyclerView.setAdapter(adapter);

        List<TeamChatEntry> mock = new ArrayList<>();
        mock.add(new TeamChatEntry("t1", "Rescue Team", null, "New assignment posted", System.currentTimeMillis()));
        mock.add(new TeamChatEntry("t2", "Medical Unit", null, "Meeting at 12:00", System.currentTimeMillis()));
        adapter.submit(mock);

        return view;
    }
}



