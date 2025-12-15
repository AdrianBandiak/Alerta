package com.abandiak.alerta.core.firebase;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public interface AuthProvider {

    Task<String> signIn(String email, String password);

    Task<String> register(String email, String password);

    String getCurrentUid();

    FirebaseAuth getRawFirebaseAuth();
}
