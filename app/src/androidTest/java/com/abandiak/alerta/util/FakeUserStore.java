package com.abandiak.alerta.util;

import java.util.HashMap;
import java.util.Map;

public class FakeUserStore {

    private static final Map<String, Map<String, Object>> USERS = new HashMap<>();

    public static void reset() {
        USERS.clear();
    }

    public static void putUser(String uid, Map<String, Object> data) {
        USERS.put(uid, data);
    }

    public static Map<String, Object> getUser(String uid) {
        return USERS.get(uid);
    }
}
