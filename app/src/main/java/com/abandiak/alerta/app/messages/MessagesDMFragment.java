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

public class MessagesDMFragment extends Fragment {

    private RecyclerView recyclerView;
    private DMChatsAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_messages_dm, container, false);

        recyclerView = view.findViewById(R.id.recyclerDM);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new DMChatsAdapter();
        recyclerView.setAdapter(adapter);

        List<DMChatEntry> mock = new ArrayList<>();
        mock.add(new DMChatEntry("chat1", "uid1", "John Doe", null, "Hey, all good?", System.currentTimeMillis()));
        mock.add(new DMChatEntry("chat2", "uid2", "Alice", null, "Are you safe?", System.currentTimeMillis()));
        adapter.submit(mock);

        return view;

    }
}
