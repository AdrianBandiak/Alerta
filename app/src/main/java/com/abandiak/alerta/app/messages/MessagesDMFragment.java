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
import com.abandiak.alerta.app.messages.dm.DMChatActivity;
import com.abandiak.alerta.data.repository.ChatRepository;
import com.abandiak.alerta.data.repository.UserRepository;
import com.abandiak.alerta.app.messages.dm.DMChatsAdapter;
import com.abandiak.alerta.app.messages.dm.DMChatEntry;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.util.ArrayList;
import java.util.List;

public class MessagesDMFragment extends Fragment {

    private RecyclerView recyclerView;
    private DMChatsAdapter adapter;

    private ListenerRegistration dmListener;

    private ChatRepository chatRepository;
    private UserRepository userRepository;

    private String currentUid;

    public MessagesDMFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        chatRepository = new ChatRepository();
        userRepository = new UserRepository();
        currentUid = FirebaseAuth.getInstance().getUid();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_messages_dm, container, false);

        recyclerView = view.findViewById(R.id.recyclerViewDm);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new DMChatsAdapter(chat -> {
            Intent intent = new Intent(getContext(), DMChatActivity.class);
            intent.putExtra("otherUserId", chat.getOtherUserId());
            startActivity(intent);
        });

        recyclerView.setAdapter(adapter);

        listenForDmChats();

        return view;
    }


    private void listenForDmChats() {
        if (currentUid == null) return;

        dmListener = chatRepository.listenForDmChatList((snapshots, error) -> {
            if (error != null) {
                Log.e("DMFragment", "Error listening for DM chats: ", error);
                return;
            }

            List<DMChatEntry> entries = new ArrayList<>();

            for (DocumentSnapshot doc : snapshots.getDocuments()) {
                List<String> participants = (List<String>) doc.get("participants");
                if (participants == null || participants.size() != 2) continue;

                String otherUserId = participants.get(0).equals(currentUid)
                        ? participants.get(1)
                        : participants.get(0);

                String lastMessage = doc.getString("lastMessage");
                long lastTimestamp = 0;

                Timestamp ts = doc.getTimestamp("lastTimestamp");
                if (ts != null) lastTimestamp = ts.toDate().getTime();

                DMChatEntry entry = new DMChatEntry(
                        doc.getId(),
                        otherUserId,
                        "Loading...",
                        lastMessage,
                        null,
                        lastTimestamp
                );

                entries.add(entry);

                userRepository.getUserById(otherUserId, userDoc -> {
                    if (userDoc != null && userDoc.exists()) {
                        String name = userDoc.getString("displayName");
                        String avatarUrl = userDoc.getString("avatarUrl");

                        entry.setOtherUserName(name);
                        entry.setAvatarUrl(avatarUrl);

                        adapter.notifyDataSetChanged();
                    }
                });
            }

            adapter.submitList(entries);
        });
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (dmListener != null) dmListener.remove();
    }
}
