package com.abandiak.alerta.app.messages.teams;

import android.content.Intent;
import android.os.Bundle;

import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.abandiak.alerta.R;
import com.abandiak.alerta.app.messages.dm.DMChatActivity;
import com.abandiak.alerta.core.utils.BaseActivity;
import com.abandiak.alerta.core.utils.SystemBars;
import com.abandiak.alerta.data.repository.ChatRepository;
import com.abandiak.alerta.data.repository.TeamRepository;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class TeamMembersActivity extends BaseActivity {

    private String teamId;
    private TeamMembersAdapter adapter;
    private RecyclerView recycler;
    private MaterialToolbar topBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SystemBars.apply(this);
        setContentView(R.layout.activity_team_members);

        teamId = getIntent().getStringExtra("teamId");

        setupToolbarInsets();
        setupRecycler();
        loadMembers();
    }

    private void setupToolbarInsets() {
        topBar = findViewById(R.id.topBar);
        topBar.setNavigationOnClickListener(v -> finishWithAnimation());

        AppBarLayout appBar = findViewById(R.id.appBar);

        ViewCompat.setOnApplyWindowInsetsListener(appBar, (v, insets) -> {
            int topInset = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            v.setPadding(0, topInset, 0, 0);
            return insets;
        });
    }

    private void setupRecycler() {
        recycler = findViewById(R.id.recyclerMembers);
        recycler.setLayoutManager(new LinearLayoutManager(this));

        adapter = new TeamMembersAdapter(member -> {

            String otherUid = member.getUid();
            String currentUid = FirebaseAuth.getInstance().getUid();
            if (currentUid == null) return;

            ChatRepository repo = new ChatRepository();
            String chatId = repo.dmChatIdFor(currentUid, otherUid);

            Map<String, Object> meta = new HashMap<>();
            meta.put("participants", Arrays.asList(currentUid, otherUid));
            meta.put("lastMessage", "New chat");

            meta.put("lastTimestamp", System.currentTimeMillis());
            meta.put("lastTimestampServer", FieldValue.serverTimestamp());


            FirebaseFirestore.getInstance()
                    .collection("dm_chats")
                    .document(chatId)
                    .set(meta, SetOptions.merge())
                    .addOnSuccessListener(unused -> {

                        Intent i = new Intent(this, DMChatActivity.class);
                        i.putExtra("otherUserId", otherUid);
                        startActivity(i);
                    });
        });

        recycler.setAdapter(adapter);
    }

    private void loadMembers() {
        new TeamRepository().getFullTeamMembers(teamId, list -> {
            topBar.setSubtitle(list.size() + " members");
            adapter.submit(list);
        });
    }
}
