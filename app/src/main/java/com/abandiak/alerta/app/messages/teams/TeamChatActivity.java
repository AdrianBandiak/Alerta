package com.abandiak.alerta.app.messages.teams;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.abandiak.alerta.R;
import com.abandiak.alerta.core.utils.BaseActivity;
import com.abandiak.alerta.core.utils.SystemBars;
import com.abandiak.alerta.data.model.ChatMessage;
import com.abandiak.alerta.data.repository.ChatRepository;
import com.abandiak.alerta.data.repository.TeamRepository;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class TeamChatActivity extends BaseActivity {

    private RecyclerView recyclerView;
    private TeamMessageAdapter adapter;

    private EditText inputMessage;

    private String teamId;
    private String currentUid;
    private ListenerRegistration msgListener;

    private ChatRepository chatRepo;
    private TeamRepository teamRepo;
    private FirebaseFirestore db;

    private String currentUserName = "";
    private String currentUserAvatar = "";

    private final List<ChatMessage> lastMessages = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SystemBars.apply(this);
        setContentView(R.layout.activity_team_chat);

        AppBarLayout appBar = findViewById(R.id.appBar);
        ViewCompat.setOnApplyWindowInsetsListener(appBar, (v, insets) -> {
            v.setPadding(0, insets.getInsets(WindowInsetsCompat.Type.statusBars()).top, 0, 0);
            return insets;
        });

        View chatContainer = findViewById(R.id.chatInputContainer);
        ViewCompat.setOnApplyWindowInsetsListener(chatContainer, (v, insets) -> {
            int bottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;
            int px24 = (int) (24 * getResources().getDisplayMetrics().density);
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(),
                    v.getPaddingRight(), bottom > 0 ? bottom : px24);
            return insets;
        });

        db = FirebaseFirestore.getInstance();
        teamId = getIntent().getStringExtra("teamId");
        currentUid = FirebaseAuth.getInstance().getUid();

        chatRepo = new ChatRepository();
        teamRepo = new TeamRepository();

        setupUI();
        loadCurrentUser();
        setupToolbar();
        listenForMessages();
    }

    private void loadCurrentUser() {
        if (currentUid == null) return;

        db.collection("users")
                .document(currentUid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String first = doc.getString("firstName");
                        String last = doc.getString("lastName");

                        currentUserName = (first + " " + last).trim();
                        currentUserAvatar = doc.getString("photoUrl");

                        adapter.setCurrentUserAvatar(currentUserAvatar);
                        adapter.setCurrentUserName(currentUserName);
                    }
                });
    }

    private void setupToolbar() {
        MaterialToolbar topBar = findViewById(R.id.topBar);
        topBar.setNavigationOnClickListener(v -> finishWithAnimation());

        teamRepo.getTeamRef(teamId).get().addOnSuccessListener(doc -> {
            String name = doc.getString("name");
            if (name != null) topBar.setTitle(name);
        });

        topBar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_members) {
                Intent i = new Intent(this, TeamMembersActivity.class);
                i.putExtra("teamId", teamId);
                startActivity(i);
                return true;
            }
            return false;
        });
    }

    private void setupUI() {
        recyclerView = findViewById(R.id.recyclerTeamMessages);

        LinearLayoutManager lm = new LinearLayoutManager(this);
        lm.setReverseLayout(false);
        lm.setStackFromEnd(false);
        recyclerView.setLayoutManager(lm);

        adapter = new TeamMessageAdapter(currentUid);
        recyclerView.setAdapter(adapter);

        inputMessage = findViewById(R.id.inputTeamMessage);
        ImageButton btnSend = findViewById(R.id.btnSendTeamMessage);

        btnSend.setOnClickListener(v -> sendMessage());
    }

    private void sendMessage() {
        String text = inputMessage.getText().toString().trim();
        if (TextUtils.isEmpty(text)) return;

        if (currentUserName.isEmpty()) currentUserName = "Unknown";
        if (currentUserAvatar == null) currentUserAvatar = "";

        chatRepo.sendTeamMessage(
                teamId,
                text,
                currentUid,
                currentUserName,
                currentUserAvatar
        );

        inputMessage.setText("");
    }

    private void listenForMessages() {
        msgListener = chatRepo.listenForTeamMessages(teamId, (snap, err) -> {
            if (err != null || snap == null) return;

            List<ChatMessage> newList = new ArrayList<>();

            for (DocumentSnapshot d : snap.getDocuments()) {

                String senderId = d.getString("senderId");
                String text = d.getString("text");

                if ("Chat created".equals(text)) continue;

                Timestamp ts = d.getTimestamp("createdAt");
                long createdAt = ts != null ? ts.toDate().getTime() : 0;

                String senderName = d.getString("senderName");
                String senderAvatar = d.getString("senderAvatar");

                newList.add(new ChatMessage(
                        d.getId(),
                        senderId,
                        text,
                        createdAt,
                        senderName,
                        senderAvatar
                ));
            }

            newList.sort(Comparator.comparingLong(ChatMessage::getCreatedAt));

            adapter.submitList(newList);

            recyclerView.post(() ->
                    recyclerView.scrollToPosition(newList.size() - 1)
            );

            lastMessages.clear();
            lastMessages.addAll(newList);
        });
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (msgListener != null) msgListener.remove();
    }
}
