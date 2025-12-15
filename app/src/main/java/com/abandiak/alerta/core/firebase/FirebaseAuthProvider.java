package com.abandiak.alerta.core.firebase;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class FirebaseAuthProvider implements AuthProvider {

    private final FirebaseAuth auth = FirebaseAuth.getInstance();

    @Override
    public Task<String> signIn(String email, String password) {
        return auth.signInWithEmailAndPassword(email, password)
                .continueWith(t -> {
                    if (!t.isSuccessful()) throw t.getException();
                    if (auth.getCurrentUser() == null) throw new Exception("User not found");
                    return auth.getCurrentUser().getUid();
                });
    }

    @Override
    public Task<String> register(String email, String password) {
        return auth.createUserWithEmailAndPassword(email, password)
                .continueWith(t -> {
                    if (!t.isSuccessful()) throw t.getException();
                    if (auth.getCurrentUser() == null) throw new Exception("User null");
                    return auth.getCurrentUser().getUid();
                });
    }

    @Override
    public String getCurrentUid() {
        return auth.getCurrentUser() == null ? null : auth.getCurrentUser().getUid();
    }

    @Override
    public FirebaseAuth getRawFirebaseAuth() {
        return auth;
    }
}
