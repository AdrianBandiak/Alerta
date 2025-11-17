package com.abandiak.alerta.app.messages.dm;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.abandiak.alerta.R;
import com.abandiak.alerta.data.model.ChatMessage;
import com.abandiak.alerta.data.repository.ChatRepository;
import com.abandiak.alerta.data.repository.UserRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.util.ArrayList;
import java.util.List;

public class DMChatActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private DMMessageAdapter adapter;
    private EditText inputMessage;
    private ImageButton btnSend;

    private ChatRepository chatRepository;
    private UserRepository userRepository;

    private ListenerRegistration messageListener;

    private String otherUserId;
    private String currentUid;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dm_chat);

        otherUserId = getIntent().getStringExtra("otherUserId");

        chatRepository = new ChatRepository();
        userRepository = new UserRepository();
        currentUid = FirebaseAuth.getInstance().getUid();

        recyclerView = findViewById(R.id.recyclerMessages);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new DMMessageAdapter(currentUid);
        recyclerView.setAdapter(adapter);

        inputMessage = findViewById(R.id.inputMessage);
        btnSend = findViewById(R.id.btnSend);

        btnSend.setOnClickListener(v -> sendMessage());

        loadOtherUserProfile();
        listenForMessages();
    }


    private void loadOtherUserProfile() {
        userRepository.getUserById(otherUserId, doc -> {
            if (doc != null && doc.exists()) {
                String name = doc.getString("displayName");
                setTitle(name);
            }
        });
    }


    private void sendMessage() {
        String text = inputMessage.getText().toString().trim();
        if (TextUtils.isEmpty(text)) return;

        chatRepository.sendDirectMessage(otherUserId, text);
        inputMessage.setText("");
    }


    private void listenForMessages() {
        messageListener = chatRepository.listenForDmMessages(otherUserId, (snapshots, error) -> {
            if (error != null) return;

            List<ChatMessage> list = new ArrayList<>();

            for (DocumentSnapshot doc : snapshots.getDocuments()) {
                ChatMessage msg = new ChatMessage(
                        doc.getId(),
                        doc.getString("senderId"),
                        doc.getString("text"),
                        doc.contains("createdAt")
                                ? doc.getTimestamp("createdAt").toDate().getTime()
                                : 0
                );
                list.add(msg);
            }

            adapter.submitList(list);
            recyclerView.scrollToPosition(list.size() - 1);
        });
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (messageListener != null) messageListener.remove();
    }
}
