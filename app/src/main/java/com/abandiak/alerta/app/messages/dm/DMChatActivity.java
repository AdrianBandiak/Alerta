package com.abandiak.alerta.app.messages.dm;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.abandiak.alerta.R;
import com.abandiak.alerta.app.messages.MessagesDMFragment;
import com.abandiak.alerta.core.utils.BaseActivity;
import com.abandiak.alerta.core.utils.SystemBars;
import com.abandiak.alerta.data.model.ChatMessage;
import com.abandiak.alerta.data.repository.ChatRepository;
import com.abandiak.alerta.data.repository.UserRepository;
import com.bumptech.glide.Glide;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.util.ArrayList;
import java.util.List;

public class DMChatActivity extends BaseActivity {

    private String otherUserId;

    private MaterialToolbar topBar;
    private RecyclerView recycler;
    private DMMessageAdapter adapter;

    private EditText input;
    private ImageButton btnSend;

    private ListenerRegistration listener;

    private ChatRepository chatRepo;
    private UserRepository userRepo;

    private String otherUserAvatar;
    private String otherUserName;
    private String currentUserAvatar;

    private String getCurrentUid() {
        return FirebaseAuth.getInstance().getUid();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SystemBars.apply(this);
        setContentView(R.layout.activity_dm_chat);

        otherUserId = getIntent().getStringExtra("otherUserId");
        if (otherUserId == null) {
            finish();
            return;
        }

        chatRepo = new ChatRepository();
        userRepo = new UserRepository();

        setupToolbar();
        setupRecycler();
        setupSendButton();

        loadOtherUserInfo(this::loadCurrentUserInfo);
    }

    private void setupToolbar() {
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
        recycler = findViewById(R.id.recyclerDmMessages);

        adapter = new DMMessageAdapter();
        adapter.setCurrentUid(getCurrentUid());

        LinearLayoutManager lm = new LinearLayoutManager(this);

        lm.setReverseLayout(false);
        lm.setStackFromEnd(false);

        recycler.setLayoutManager(lm);
        recycler.setAdapter(adapter);
    }


    private void setupSendButton() {
        input = findViewById(R.id.inputMessage);
        btnSend = findViewById(R.id.btnSendMessage);

        btnSend.setOnClickListener(v -> {
            String text = input.getText().toString().trim();
            if (text.isEmpty()) return;

            chatRepo.sendDirectMessage(otherUserId, text, existedBefore -> {
                if (!existedBefore) {
                    MessagesDMFragment.requestRefresh();
                }
            });

            input.setText("");
        });
    }

    private void loadOtherUserInfo(Runnable afterLoaded) {
        userRepo.getUserById(otherUserId, userDoc -> {
            if (userDoc == null || !userDoc.exists()) {
                if (afterLoaded != null) afterLoaded.run();
                return;
            }

            String first = userDoc.getString("firstName");
            String last = userDoc.getString("lastName");

            otherUserAvatar = userDoc.getString("photoUrl");
            otherUserName = (first + " " + last).trim();

            topBar.setTitle(otherUserName);

            ImageView avatarView = findViewById(R.id.headerAvatar);
            Glide.with(this)
                    .load(otherUserAvatar)
                    .placeholder(R.drawable.ic_avatar_placeholder)
                    .into(avatarView);

            adapter.setOtherUserAvatar(otherUserAvatar);

            if (afterLoaded != null) afterLoaded.run();
        });
    }

    private void loadCurrentUserInfo() {
        String uid = getCurrentUid();
        if (uid == null) return;

        userRepo.getUserById(uid, userDoc -> {
            if (userDoc == null || !userDoc.exists()) {
                listenMessages();
                return;
            }

            currentUserAvatar = userDoc.getString("photoUrl");

            listenMessages();
        });
    }

    private void listenMessages() {
        listener = chatRepo.listenForDmMessages(otherUserId, (snap, err) -> {
            if (err != null || snap == null) return;

            List<ChatMessage> list = new ArrayList<>();
            String uid = getCurrentUid();

            for (DocumentSnapshot d : snap.getDocuments()) {

                String id = d.getId();
                String senderId = d.getString("senderId");
                String text = d.getString("text");

                Timestamp ts = d.getTimestamp("createdAt");
                long createdAt = ts != null ? ts.toDate().getTime() : 0;

                ChatMessage msg = new ChatMessage(id, senderId, text, createdAt);

                if (senderId.equals(uid)) {
                    msg.setSenderName("You");
                    msg.setSenderAvatar(currentUserAvatar);
                } else {
                    msg.setSenderName(otherUserName);
                    msg.setSenderAvatar(otherUserAvatar);
                }

                list.add(msg);
            }

            adapter.submitList(list);

            if (!list.isEmpty()) {
                recycler.post(() -> recycler.smoothScrollToPosition(list.size() - 1));
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (listener != null) listener.remove();
    }
}
