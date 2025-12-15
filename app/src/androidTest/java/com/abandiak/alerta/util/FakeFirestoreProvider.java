package com.abandiak.alerta.util;

import com.abandiak.alerta.core.firebase.FirestoreProvider;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Map;

public class FakeFirestoreProvider implements FirestoreProvider {

    @Override
    public Task<Map<String, Object>> getUserDocument(String uid) {
        return Tasks.forResult(FakeUserStore.getUser(uid));
    }

    @Override
    public Task<Void> saveUserDocument(String uid, Map<String, Object> map) {
        FakeUserStore.putUser(uid, map);
        return Tasks.forResult(null);
    }

    @Override
    public FirebaseFirestore getRawFirestore() {
        return null;
    }
}
