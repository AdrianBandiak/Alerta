package com.abandiak.alerta;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;

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

            auth.createUserWithEmailAndPassword(email, password)
                    .addOnSuccessListener(authResult -> {
                        if (authResult.getUser() == null) {
                            Log.e(TAG, "User is null after registration");
                            ToastUtils.show(this, getString(R.string.data_save_error));
                            return;
                        }
                        String userId = authResult.getUser().getUid();
                        HashMap<String, Object> userMap = new HashMap<>();
                        userMap.put("email", email);
                        userMap.put("role", "user");

                        firestore.collection("users").document(userId)
                                .set(userMap)
                                .addOnSuccessListener(unused -> {
                                    ToastUtils.show(this, getString(R.string.account_created));
                                    Intent intent = new Intent(this, LoginActivity.class);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                    startActivity(intent);
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Firestore error: ", e);
                                    ToastUtils.show(this, getString(R.string.data_save_error));
                                });
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Registration failed: ", e);
                        ToastUtils.show(this, getString(R.string.registration_failed, e.getMessage()));
                    });
        });
    }

    private boolean isValidEmail(String email) {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private boolean isValidPassword(String password) {
        return password.length() >= 6 && password.matches("^(?=.*[a-zA-Z])(?=.*\\d).+$");
    }
}
