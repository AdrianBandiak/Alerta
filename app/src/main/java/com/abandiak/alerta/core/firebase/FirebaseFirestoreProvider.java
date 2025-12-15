package com.abandiak.alerta.core.firebase;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.Map;

public class FirebaseFirestoreProvider implements FirestoreProvider {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    public Task<Map<String, Object>> getUserDocument(String uid) {
        return db.collection("users")
                .document(uid)
                .get()
                .continueWith(t -> {
                    if (!t.isSuccessful()) throw t.getException();
                    if (!t.getResult().exists()) return null;
                    return t.getResult().getData();
                });
    }

    @Override
    public Task<Void> saveUserDocument(String uid, Map<String, Object> map) {
        return db.collection("users")
                .document(uid)
                .set(map, SetOptions.merge());
    }

    @Override
    public FirebaseFirestore getRawFirestore() {
        return db;
    }
}
