package com.abandiak.alerta;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {

    private EditText editTextEmail, editTextPassword;
    private Button buttonLogin;
    private TextView textViewRegisterLink;
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        firebaseAuth = FirebaseAuth.getInstance();

        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        buttonLogin = findViewById(R.id.buttonLogin);
        textViewRegisterLink = findViewById(R.id.textViewRegisterLink);

        String baseText = "Don't have an account? ";
        String boldText = "Register";

        SpannableString spannable = new SpannableString(baseText + boldText);
        spannable.setSpan(
                new android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
                baseText.length(),
                baseText.length() + boldText.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        );

        spannable.setSpan(
                new android.text.style.ForegroundColorSpan(0xFFE53935),
                baseText.length(),
                baseText.length() + boldText.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        );

        textViewRegisterLink.setText(spannable);

        buttonLogin.setOnClickListener(view -> {
            String email = editTextEmail.getText().toString().trim();
            String password = editTextPassword.getText().toString().trim();

            if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
                showCustomToast("Please enter email and password.");
                return;
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                showCustomToast("Invalid email format.");
                return;
            }

            firebaseAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            showCustomToast("Login successful.");
                            startActivity(new Intent(this, HomeActivity.class));
                            finish();
                        } else {
                            showCustomToast("Login failed: Invalid credentials.");
                        }
                    });
        });

        textViewRegisterLink.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in_translate);

        ImageView logo = findViewById(R.id.logoImage);

        logo.setVisibility(View.VISIBLE);
        editTextEmail.setVisibility(View.VISIBLE);
        editTextPassword.setVisibility(View.VISIBLE);
        buttonLogin.setVisibility(View.VISIBLE);
        textViewRegisterLink.setVisibility(View.VISIBLE);

        logo.startAnimation(fadeIn);
        editTextEmail.startAnimation(fadeIn);
        editTextPassword.startAnimation(fadeIn);
        buttonLogin.startAnimation(fadeIn);
        textViewRegisterLink.startAnimation(fadeIn);
    }


    private void showCustomToast(String message) {
        LayoutInflater inflater = getLayoutInflater();
        View layout = inflater.inflate(R.layout.toast_custom, findViewById(android.R.id.content), false);

        TextView text = layout.findViewById(R.id.toast_text);
        ImageView icon = layout.findViewById(R.id.toast_icon);

        text.setText(message);
        icon.setImageResource(R.drawable.logo_alerta);

        Toast toast = new Toast(getApplicationContext());
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(layout);
        toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 100);
        toast.show();

        try {
            @SuppressLint("SoonBlockedPrivateApi") Object toastTN = toast.getClass().getDeclaredField("mTN").get(toast);
            Object params = toastTN.getClass().getDeclaredMethod("getWindowParams").invoke(toastTN);
            if (params instanceof android.view.WindowManager.LayoutParams) {
                ((android.view.WindowManager.LayoutParams) params).windowAnimations = R.style.ToastAnimation;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
