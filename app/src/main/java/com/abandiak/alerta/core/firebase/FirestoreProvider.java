package com.abandiak.alerta.core.firebase;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Map;

public interface FirestoreProvider {

    Task<Map<String, Object>> getUserDocument(String uid);

    Task<Void> saveUserDocument(String uid, Map<String, Object> map);

    FirebaseFirestore getRawFirestore();
}
