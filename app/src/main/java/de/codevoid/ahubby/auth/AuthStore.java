package de.codevoid.ahubby.auth;

import android.content.Context;
import android.content.SharedPreferences;

public class AuthStore {

    private static final String PREFS = "ahubby_auth";
    private static final String KEY_TOKEN = "user_token";
    private static final String KEY_USER_ID = "user_id";

    private final SharedPreferences prefs;

    public AuthStore(Context context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public void save(String userToken, String userId) {
        prefs.edit()
                .putString(KEY_TOKEN, userToken)
                .putString(KEY_USER_ID, userId)
                .apply();
    }

    public String getToken() {
        return prefs.getString(KEY_TOKEN, null);
    }

    public String getUserId() {
        return prefs.getString(KEY_USER_ID, null);
    }

    public boolean isLoggedIn() {
        String token = getToken();
        return token != null && !token.isEmpty();
    }

    public void clear() {
        prefs.edit().clear().apply();
    }
}
