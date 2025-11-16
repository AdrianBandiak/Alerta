package com.abandiak.alerta.app.more;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Html;
import android.view.View;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;

import com.abandiak.alerta.R;
import com.abandiak.alerta.app.auth.LoginActivity;
import com.abandiak.alerta.app.home.HomeActivity;
import com.abandiak.alerta.app.map.MapActivity;
import com.abandiak.alerta.app.profile.CompleteProfileActivity;
import com.abandiak.alerta.app.tasks.TasksActivity;
import com.abandiak.alerta.app.teams.TeamsActivity;
import com.abandiak.alerta.core.utils.BaseActivity;
import com.abandiak.alerta.core.utils.SystemBars;
import com.abandiak.alerta.core.utils.ToastUtils;
import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;

import java.util.Objects;

public class MoreActivity extends BaseActivity {

    private BottomNavigationView bottomNav;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private ShapeableImageView imageProfile;
    private TextView textName, textEmail;

    private final ActivityResultLauncher<Intent> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) uploadProfileImage(uri);
                }
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
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

        findViewById(R.id.textEditProfile).setOnClickListener(v -> {
            Intent intent = new Intent(this, CompleteProfileActivity.class);
            intent.putExtra("edit_mode", true);
            startActivity(intent);
        });

        findViewById(R.id.btnAbout).setOnClickListener(v -> showAboutDialog());

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

        String uid = Objects.requireNonNull(auth.getCurrentUser()).getUid();

        FirebaseStorage.getInstance()
                .getReference("profile_pics/" + uid + "/profile.jpg")
                .putFile(uri)
                .addOnSuccessListener(task -> {

                    task.getStorage().getDownloadUrl().addOnSuccessListener(downloadUri -> {

                        db.collection("users")
                                .document(uid)
                                .update("photoUrl", downloadUri.toString());

                        imageProfile.setImageURI(uri);
                        imageProfile.setStrokeWidth(0);

                        ToastUtils.show(this, "Profile picture updated!");
                    });

                })
                .addOnFailureListener(e ->
                        ToastUtils.show(this, "Upload failed: " + e.getMessage()));
    }

    private void loadUserData() {
        String uid = auth.getCurrentUser().getUid();

        db.collection("users").document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;

                    String first = doc.getString("firstName");
                    String last = doc.getString("lastName");

                    textName.setText(first + " " + last);
                    textEmail.setText(auth.getCurrentUser().getEmail());

                    String photoUrl = doc.getString("photoUrl");

                    if (photoUrl != null && !photoUrl.isEmpty()) {
                        Glide.with(this)
                                .load(photoUrl)
                                .centerCrop()
                                .into(imageProfile);

                        imageProfile.setStrokeWidth(0);

                    } else {
                        imageProfile.setImageResource(R.drawable.ic_person_placeholder);
                        imageProfile.setStrokeWidth(6);
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
            }

            return true;
        });
    }

    private void showAboutDialog() {
        View view = getLayoutInflater().inflate(R.layout.dialog_about_alerta, null);

        TextView content = view.findViewById(R.id.textAboutContent);
        TextView github = view.findViewById(R.id.textGitHub);

        content.setText(Html.fromHtml(getAboutText(), Html.FROM_HTML_MODE_LEGACY));

        github.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/AdrianBandiak/Alerta"));
            startActivity(i);
        });

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .setCancelable(true)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        dialog.show();
    }


    private String getAboutText() {

        return "<h2><b>About Alerta</b></h2>"

                + "<p><b>Alerta</b> is a modern emergency coordination app built to help people stay "
                + "safe, informed, and connected during unexpected or high-risk events. Designed with "
                + "speed, clarity, and user experience in mind, Alerta enables you to instantly report "
                + "incidents, receive real-time alerts, and collaborate with local communities or "
                + "emergency teams. All in one unified platform.</p>"

                + "<br><b>Main features:</b><br>"
                + "• Instant incident reporting with photos, GPS & verification<br>"
                + "• Live map with dynamic clustering and category filtering<br>"
                + "• Team-based coordination and assignment tools<br>"
                + "• Task creation for organized crisis response<br>"
                + "• Offline-first architecture for unstable environments<br>"
                + "• Clean, accessible and modern Material Design UI<br><br>"

                + "<p>Alerta empowers communities to communicate faster, act smarter, and stay aligned "
                + "when it matters most.</p>";
    }


    @Override
    protected void onResume() {
        super.onResume();
        loadUserData();
        if (bottomNav != null) bottomNav.setSelectedItemId(R.id.nav_more);
    }
}
