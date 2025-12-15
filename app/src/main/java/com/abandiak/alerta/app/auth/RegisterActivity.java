package com.abandiak.alerta.app.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;

import com.abandiak.alerta.R;
import com.abandiak.alerta.core.firebase.AuthProvider;
import com.abandiak.alerta.core.firebase.FirebaseAuthProvider;
import com.abandiak.alerta.core.firebase.FirebaseFirestoreProvider;
import com.abandiak.alerta.core.firebase.FirestoreProvider;
import com.abandiak.alerta.core.utils.BaseActivity;
import com.abandiak.alerta.core.utils.SystemBars;
import com.abandiak.alerta.core.utils.ToastUtils;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class RegisterActivity extends BaseActivity {

    public static AuthProvider authOverride = null;
    public static FirestoreProvider dbOverride = null;
    public static boolean disableFinishForTests = false;
    public static boolean testNavigatedToLogin = false;

    private AuthProvider authProvider;
    private FirestoreProvider firestoreProvider;

    private EditText emailInput, passwordInput, confirmPasswordInput;
    private Button registerButton;

    private static final String TAG = "RegisterActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        SystemBars.apply(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        authProvider = (authOverride != null)
                ? authOverride
                : new FirebaseAuthProvider();

        firestoreProvider = (dbOverride != null)
                ? dbOverride
                : new FirebaseFirestoreProvider();

        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput);
        registerButton = findViewById(R.id.registerButton);

        registerButton.setOnClickListener(v -> registerUser());
    }

    private void registerUser() {

        String email = safe(emailInput).toLowerCase(Locale.US);
        String password = safe(passwordInput);
        String confirmPassword = safe(confirmPasswordInput);

        if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            ToastUtils.show(this, getString(R.string.fill_all_fields));
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            ToastUtils.show(this, getString(R.string.invalid_email));
            return;
        }

        if (password.length() < 8 || !password.matches("^(?=.*[a-zA-Z])(?=.*\\d).+$")) {
            ToastUtils.show(this, getString(R.string.invalid_password));
            return;
        }

        if (!password.equals(confirmPassword)) {
            ToastUtils.show(this, getString(R.string.passwords_do_not_match));
            return;
        }

        registerButton.setEnabled(false);

        authProvider.register(email, password)
                .addOnSuccessListener(uid -> {

                    Map<String, Object> map = new HashMap<>();
                    map.put("email", email);
                    map.put("role", "user");
                    map.put("createdAt", System.currentTimeMillis());

                    firestoreProvider.saveUserDocument(uid, map)
                            .addOnSuccessListener(a -> {
                                ToastUtils.show(this, getString(R.string.account_created));

                                testNavigatedToLogin = true;

                                Intent intent = new Intent(this, LoginActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);

                                if (!disableFinishForTests) finish();
                            })
                            .addOnFailureListener(e -> {
                                ToastUtils.show(this, getString(R.string.data_save_error));
                                registerButton.setEnabled(true);
                            });
                })
                .addOnFailureListener(e -> {
                    ToastUtils.show(this, getString(R.string.registration_failed, e.getMessage()));
                    registerButton.setEnabled(true);
                });
    }

    private static String safe(EditText e) {
        return e.getText() == null ? "" : e.getText().toString().trim();
    }
}
