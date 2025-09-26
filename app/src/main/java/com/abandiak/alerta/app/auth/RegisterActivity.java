package com.abandiak.alerta.app.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.abandiak.alerta.R;
import com.abandiak.alerta.app.home.HomeActivity;
import com.abandiak.alerta.core.utils.ToastUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Locale;

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
            String email = safeText(emailInput).toLowerCase(Locale.US);
            String password = safeText(passwordInput);
            String confirmPassword = safeText(confirmPasswordInput);

            if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password) || TextUtils.isEmpty(confirmPassword)) {
                ToastUtils.show(this, getString(R.string.fill_all_fields));
                return;
            }

            if (!isValidEmail(email)) {
                ToastUtils.show(this, getString(R.string.invalid_email));
                return;
            }

            if (!isValidPassword(password)) {
                ToastUtils.show(this, getString(R.string.invalid_password));
                return;
            }

            if (!password.equals(confirmPassword)) {
                ToastUtils.show(this, getString(R.string.passwords_do_not_match));
                return;
            }

            registerButton.setEnabled(false);

            auth.createUserWithEmailAndPassword(email, password)
                    .addOnSuccessListener(authResult -> {
                        if (authResult.getUser() == null) {
                            Log.e(TAG, "User is null after registration");
                            ToastUtils.show(this, getString(R.string.data_save_error));
                            registerButton.setEnabled(true);
                            return;
                        }
                        String userId = authResult.getUser().getUid();
                        HashMap<String, Object> userMap = new HashMap<>();
                        userMap.put("email", email);
                        userMap.put("role", "user");
                        userMap.put("createdAt", System.currentTimeMillis());

                        firestore.collection("users").document(userId)
                                .set(userMap, SetOptions.merge())
                                .addOnSuccessListener(unused -> {
                                    ToastUtils.show(this, getString(R.string.account_created));
                                    Intent intent = new Intent(this, HomeActivity.class);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                    startActivity(intent);
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Firestore error: ", e);
                                    ToastUtils.show(this, getString(R.string.data_save_error));
                                    registerButton.setEnabled(true);
                                });
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Registration failed: ", e);
                        ToastUtils.show(this, getString(R.string.registration_failed, e.getMessage()));
                        registerButton.setEnabled(true);
                    });
        });
    }

    private boolean isValidEmail(String email) {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private boolean isValidPassword(String password) {
        return password.length() >= 8 && password.matches("^(?=.*[a-zA-Z])(?=.*\\d).+$");
    }

    private static String safeText(EditText e) {
        return e.getText() == null ? "" : e.getText().toString().trim();
    }
}
