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
import com.abandiak.alerta.app.messages.dm.DMChatsAdapter;
import com.abandiak.alerta.app.messages.dm.DMChatEntry;
import com.abandiak.alerta.data.repository.ChatRepository;
import com.abandiak.alerta.data.repository.UserRepository;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.util.ArrayList;
import java.util.List;

public class MessagesDMFragment extends Fragment {

    private static MessagesDMFragment instance;

    public static void requestRefresh() {
        if (instance != null) {
            Log.e("DM_FRAGMENT", "requestRefresh() CALLED");
            instance.forceReload();
        }
    }

    private RecyclerView recyclerView;
    private DMChatsAdapter adapter;

    private ListenerRegistration dmListener;

    private ChatRepository chatRepository;
    private UserRepository userRepository;

    private String getCurrentUid() {
        return FirebaseAuth.getInstance().getUid();
    }

    public MessagesDMFragment() {}

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        instance = this;

        Log.e("DM_FRAGMENT", "onCreate()");

        chatRepository = new ChatRepository();
        userRepository = new UserRepository();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        Log.e("DM_FRAGMENT", "onCreateView()");

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
        String uid = getCurrentUid();
        if (uid == null) return;

        if (dmListener != null) dmListener.remove();

        Log.e("DM_FRAGMENT", "listenForDmChats() START");

        dmListener = chatRepository.listenForDmChatList((snapshots, error) -> {
            if (error != null) {
                Log.e("DM_FRAGMENT", "Error listening for DM chats: ", error);
                return;
            }

            Log.e("DM_FRAGMENT", "SNAPSHOT RECEIVED count=" + snapshots.size());

            List<DMChatEntry> entries = new ArrayList<>();

            for (DocumentSnapshot doc : snapshots.getDocuments()) {

                Log.e("DM_FRAGMENT", "DOC = " + doc.getId());

                List<String> participants = (List<String>) doc.get("participants");
                if (participants == null || participants.size() != 2) continue;

                String otherUserId = participants.get(0).equals(uid)
                        ? participants.get(1)
                        : participants.get(0);

                String lastMessage = doc.getString("lastMessage");
                Timestamp ts = null;

                try {
                    ts = doc.getTimestamp("lastTimestamp");
                } catch (Exception e) {
                    Log.e("DM_FRAGMENT", "ERROR: lastTimestamp is NOT a Timestamp, using fallback 0");
                }

                long lastTimestamp = (ts != null) ? ts.toDate().getTime() : 0;


                DMChatEntry entry = new DMChatEntry(
                        doc.getId(),
                        otherUserId,
                        "",
                        lastMessage,
                        null,
                        lastTimestamp
                );

                entries.add(entry);

                userRepository.getUserById(otherUserId, userDoc -> {
                    if (userDoc != null && userDoc.exists()) {

                        String first = userDoc.getString("firstName");
                        String last = userDoc.getString("lastName");
                        String avatar = userDoc.getString("photoUrl");

                        entry.setOtherUserName((first + " " + last).trim());
                        entry.setAvatarUrl(avatar);
                    }

                    adapter.submitList(new ArrayList<>(entries));
                });
            }

            adapter.submitList(entries);
        });
    }

    private void forceReload() {
        Log.e("DM_FRAGMENT", "forceReload()");
        listenForDmChats();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.e("DM_FRAGMENT", "onDestroyView()");
        if (dmListener != null) dmListener.remove();

        if (instance == this) instance = null;
    }
}
