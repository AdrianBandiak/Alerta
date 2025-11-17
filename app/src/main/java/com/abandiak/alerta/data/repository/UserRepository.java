package com.abandiak.alerta.data.repository;

import com.google.firebase.firestore.*;
import java.util.*;

public class UserRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final Map<String, DocumentSnapshot> cache = new HashMap<>();

    private CollectionReference users() {
        return db.collection("users");
    }

    public interface UserCallback {
        void onResult(DocumentSnapshot document);
    }

    public void getUserById(String userId, UserCallback callback) {
        if (cache.containsKey(userId)) {
            callback.onResult(cache.get(userId));
            return;
        }

        users().document(userId).get()
                .addOnSuccessListener(doc -> {
                    cache.put(userId, doc);
                    callback.onResult(doc);
                })
                .addOnFailureListener(e -> callback.onResult(null));
    }
}
