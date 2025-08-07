package com.abandiak.alerta;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Objects;

public class RegisterActivity extends AppCompatActivity {

    private EditText emailInput, passwordInput, confirmPasswordInput;
    private Button registerButton;
    private FirebaseAuth auth;
    private FirebaseFirestore firestore;
    private static final String TAG = "RegisterActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput);
        registerButton = findViewById(R.id.registerButton);

        registerButton.setOnClickListener(v -> {
            String email = emailInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();
            String confirmPassword = confirmPasswordInput.getText().toString().trim();

            if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password) || TextUtils.isEmpty(confirmPassword)) {
                showCustomToast(getString(R.string.fill_all_fields));
                return;
            }

            if (!isValidEmail(email)) {
                showCustomToast(getString(R.string.invalid_email));
                return;
            }

            if (!isValidPassword(password)) {
                showCustomToast(getString(R.string.invalid_password));
                return;
            }

            if (!password.equals(confirmPassword)) {
                showCustomToast(getString(R.string.passwords_do_not_match));
                return;
            }

            auth.createUserWithEmailAndPassword(email, password)
                    .addOnSuccessListener(authResult -> {
                        String userId = Objects.requireNonNull(auth.getCurrentUser()).getUid();
                        HashMap<String, Object> userMap = new HashMap<>();
                        userMap.put("email", email);
                        userMap.put("role", "user");

                        firestore.collection("users").document(userId)
                                .set(userMap)
                                .addOnSuccessListener(unused -> {
                                    showCustomToast(getString(R.string.account_created));
                                    Log.d(TAG, "Redirecting to LoginActivity");
                                    Intent intent = new Intent(this, LoginActivity.class);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                    startActivity(intent);
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Firestore error: ", e);
                                    showCustomToast(getString(R.string.data_save_error));
                                });
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Registration failed: ", e);
                        showCustomToast(getString(R.string.registration_failed, e.getMessage()));
                    });
        });
    }

    private boolean isValidEmail(String email) {
        String emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.(pl|com|[a-z]{2,3})";
        return email.matches(emailPattern);
    }

    private boolean isValidPassword(String password) {
        return password.length() >= 6 && password.matches("^(?=.*[a-zA-Z])(?=.*\\d).+$");
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

