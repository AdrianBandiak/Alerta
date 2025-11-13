package com.abandiak.alerta.app.more;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.abandiak.alerta.R;
import com.abandiak.alerta.app.auth.LoginActivity;
import com.abandiak.alerta.app.home.HomeActivity;
import com.abandiak.alerta.app.map.MapActivity;
import com.abandiak.alerta.app.tasks.TasksActivity;
import com.abandiak.alerta.app.teams.TeamsActivity;
import com.abandiak.alerta.core.utils.SystemBars;
import com.abandiak.alerta.core.utils.ToastUtils;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;

import java.util.Objects;

public class MoreActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private ImageView imageProfile;
    private TextView textName, textEmail;

    private final ActivityResultLauncher<Intent> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    uploadProfileImage(uri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SystemBars.apply(this);

        setContentView(R.layout.activity_more);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        imageProfile = findViewById(R.id.imageProfile);
        textName = findViewById(R.id.textUserName);
        textEmail = findViewById(R.id.textUserEmail);

        loadUserData();

        imageProfile.setOnClickListener(v -> openImagePicker());
        findViewById(R.id.textEditProfile).setOnClickListener(v ->
                ToastUtils.show(this, "Edit profile coming soon!"));

        findViewById(R.id.btnLogout).setOnClickListener(v -> {
            auth.signOut();
            startActivity(new Intent(this, LoginActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK));
            finish();
        });

        setupBottomNav();
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickImageLauncher.launch(intent);
    }

    private void uploadProfileImage(Uri uri) {
        String uid = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
        Log.d("STORAGE", "Uploading to: profile_pics/" + uid + "/profile.jpg");

        FirebaseStorage.getInstance()
                .getReference("profile_pics/" + uid + "/profile.jpg")
                .putFile(uri)
                .addOnSuccessListener(t -> {
                    ToastUtils.show(this, "Profile picture updated!");
                    imageProfile.setImageURI(uri);
                })
                .addOnFailureListener(e -> {
                    Log.e("STORAGE", "Upload failed", e);
                    ToastUtils.show(this, "Upload failed: " + e.getMessage());
                });
    }

    private void loadUserData() {
        String uid = auth.getCurrentUser().getUid();
        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        textName.setText(doc.getString("firstName") + " " + doc.getString("lastName"));
                        textEmail.setText(auth.getCurrentUser().getEmail());
                    }
                });
    }

    private void setupBottomNav() {
        bottomNav = findViewById(R.id.bottomNav);
        if (bottomNav == null) return;
        bottomNav.setSelectedItemId(R.id.nav_more);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            Intent intent = null;
            if (id == R.id.nav_home) intent = new Intent(this, HomeActivity.class);
            else if (id == R.id.nav_map) intent = new Intent(this, MapActivity.class);
            else if (id == R.id.nav_tasks) intent = new Intent(this, TasksActivity.class);
            else if (id == R.id.nav_teams) intent = new Intent(this, TeamsActivity.class);
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                overridePendingTransition(0, 0);
            }
            return true;
        });
    }
}
