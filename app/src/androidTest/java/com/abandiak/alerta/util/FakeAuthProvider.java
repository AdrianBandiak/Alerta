package com.abandiak.alerta.util;

import com.abandiak.alerta.core.firebase.AuthProvider;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class FakeAuthProvider implements AuthProvider {

    private static final Map<String, String> USERS = new HashMap<>();
    private static final Map<String, String> IDS = new HashMap<>();
    private static String currentUid = null;

    private static boolean failNextLogin = false;

    public static void reset() {
        USERS.clear();
        IDS.clear();
        currentUid = null;
        failNextLogin = false;
    }

    public static void addUser(String email, String pass, String uid) {
        USERS.put(email, pass);
        IDS.put(email, uid);
    }

    public static void setFailNextLogin(boolean fail) {
        failNextLogin = fail;
    }

    @Override
    public Task<String> signIn(String email, String pass) {

        if (failNextLogin) {
            failNextLogin = false;
            return Tasks.forException(new Exception("Invalid credentials"));
        }

        if (!USERS.containsKey(email) || !USERS.get(email).equals(pass)) {
            return Tasks.forException(new Exception("Invalid credentials"));
        }

        currentUid = IDS.get(email);
        return Tasks.forResult(currentUid);
    }

    @Override
    public Task<String> register(String email, String pass) {

        if (USERS.containsKey(email)) {
            return Tasks.forException(new Exception("User already exists"));
        }

        String uid = "FAKE_" + System.currentTimeMillis();

        USERS.put(email, pass);
        IDS.put(email, uid);
        currentUid = uid;

        return Tasks.forResult(uid);
    }

    @Override
    public String getCurrentUid() {
        return currentUid;
    }

    @Override
    public FirebaseAuth getRawFirebaseAuth() {
        return null;
    }
}
