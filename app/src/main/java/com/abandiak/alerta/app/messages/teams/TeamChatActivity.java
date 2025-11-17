package com.abandiak.alerta.app.messages.teams;

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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class TeamChatActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TeamMessageAdapter adapter;
    private EditText inputMessage;
    private ImageButton btnSend;

    private ChatRepository chatRepo;

    private String teamId;
    private String currentUid;

    private ListenerRegistration messageListener;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_team_chat);

        teamId = getIntent().getStringExtra("teamId");
        currentUid = FirebaseAuth.getInstance().getUid();

        chatRepo = new ChatRepository();

        recyclerView = findViewById(R.id.recyclerTeamMessages);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new TeamMessageAdapter(currentUid);
        recyclerView.setAdapter(adapter);

        inputMessage = findViewById(R.id.inputTeamMessage);
        btnSend = findViewById(R.id.btnSendTeamMessage);

        btnSend.setOnClickListener(v -> sendMessage());

        listenForMessages();
    }

    private void sendMessage() {
        String text = inputMessage.getText().toString().trim();
        if (TextUtils.isEmpty(text)) return;

        chatRepo.sendTeamMessage(teamId, text);
        inputMessage.setText("");
    }

    private void listenForMessages() {

        messageListener = chatRepo.listenForTeamMessages(
                teamId,
                (snapshots, error) -> {

                    if (error != null) return;

                    List<ChatMessage> list = new ArrayList<>();

                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        ChatMessage msg = new ChatMessage(
                                doc.getId(),
                                doc.getString("senderId"),
                                doc.getString("text"),
                                doc.contains("createdAt") ?
                                        doc.getTimestamp("createdAt").toDate().getTime() :
                                        0
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
        if (messageListener != null) {
            messageListener.remove();
        }
    }
}
