package com.abandiak.alerta;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Patterns;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {

    private TextView textViewRegisterLink;
    private FirebaseAuth firebaseAuth;
    private TextView editTextEmail, editTextPassword;
    private Button buttonLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        super.onCreate(savedInstanceState);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(Color.TRANSPARENT);
            getWindow().setNavigationBarColor(Color.TRANSPARENT);
        }

        setContentView(R.layout.activity_login);

        LinearLayout root = findViewById(R.id.login_root);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(
                    v.getPaddingLeft(),
                    bars.top + v.getPaddingTop(),
                    v.getPaddingRight(),
                    bars.bottom + v.getPaddingBottom()
            );
            return WindowInsetsCompat.CONSUMED;
        });

        WindowInsetsControllerCompat controller =
                new WindowInsetsControllerCompat(getWindow(), root);
        controller.setAppearanceLightStatusBars(true);
        controller.setAppearanceLightNavigationBars(true);
        controller.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        );

        firebaseAuth = FirebaseAuth.getInstance();

        textViewRegisterLink = findViewById(R.id.textViewRegisterLink);
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        buttonLogin = findViewById(R.id.buttonLogin);
        ImageView logo = findViewById(R.id.logoImage);

        String baseText = "Don't have an account? ";
        String boldText = "Register";
        SpannableString spannable = new SpannableString(baseText + boldText);
        int start = baseText.length();
        int end = start + boldText.length();
        spannable.setSpan(new StyleSpan(Typeface.BOLD), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        int red = ContextCompat.getColor(this, R.color.alerta_primary);
        spannable.setSpan(new ForegroundColorSpan(red), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        textViewRegisterLink.setText(spannable);

        buttonLogin.setOnClickListener(view -> {
            String email = editTextEmail.getText().toString().trim();
            String password = editTextPassword.getText().toString().trim();

            if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
                ToastUtils.show(this, "Please enter email and password.");
                return;
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                ToastUtils.show(this, "Invalid email format.");
                return;
            }

            firebaseAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            ToastUtils.show(this, "Login successful.");
                            startActivity(new Intent(this, HomeActivity.class));
                            finish();
                        } else {
                            ToastUtils.show(this, "Login failed: Invalid credentials.");
                        }
                    });
        });

        textViewRegisterLink.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in_translate);
        logo.setVisibility(View.VISIBLE);
        root.setVisibility(View.VISIBLE);
        root.startAnimation(fadeIn);
    }
}
