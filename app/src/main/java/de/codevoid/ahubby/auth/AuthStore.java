package de.codevoid.ahubby.auth;

import android.content.Context;
import android.content.SharedPreferences;

public class AuthStore {

    private static final String PREFS = "ahubby_auth";
    private static final String KEY_TOKEN = "user_token";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_PASSWORD = "password";
    private static final String KEY_DISPLAY_NAME = "display_name";
    private static final String KEY_DEVICE_NAME = "device_name";
    private static final String KEY_MAP_LICENSE = "map_license";
    private static final String KEY_COMMUNITY_POINTS = "community_points";
    private static final String KEY_ACTIVE_GROUP_ID = "active_group_id";
    private static final String KEY_LOGIN_TIMESTAMP = "login_timestamp";

    private final Context context;
    private final SharedPreferences prefs;

    public AuthStore(Context context) {
        this.context = context.getApplicationContext();
        prefs = this.context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public Context getContext() {
        return context;
    }

    public void save(String userToken, String userId) {
        prefs.edit()
                .putString(KEY_TOKEN, userToken)
                .putString(KEY_USER_ID, userId)
                .apply();
    }

    public void saveLoginTimestamp(long epochSeconds) {
        prefs.edit().putLong(KEY_LOGIN_TIMESTAMP, epochSeconds).apply();
    }

    public long getLoginTimestamp() {
        return prefs.getLong(KEY_LOGIN_TIMESTAMP, 0L);
    }

    /**
     * Builds the iOS-style Bearer token: Base64(<_id>:<epoch_seconds>:<user_token>).
     * This is the auth format expected by all /api/ios/ endpoints.
     */
    public String getIosBearerToken() {
        String userId = getUserId();
        String token = getToken();
        if (userId == null || token == null) return null;
        String raw = userId + ":" + getLoginTimestamp() + ":" + token;
        return android.util.Base64.encodeToString(
                raw.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                android.util.Base64.NO_WRAP);
    }

    public void saveCredentials(String email, String password) {
        prefs.edit()
                .putString(KEY_EMAIL, email)
                .putString(KEY_PASSWORD, password)
                .apply();
    }

    public String getPassword() {
        return prefs.getString(KEY_PASSWORD, null);
    }

    public void saveProfile(String email, String displayName, String deviceName,
                            boolean mapLicense, int communityPoints) {
        prefs.edit()
                .putString(KEY_EMAIL, email)
                .putString(KEY_DISPLAY_NAME, displayName)
                .putString(KEY_DEVICE_NAME, deviceName)
                .putBoolean(KEY_MAP_LICENSE, mapLicense)
                .putInt(KEY_COMMUNITY_POINTS, communityPoints)
                .apply();
    }

    public String getToken() {
        return prefs.getString(KEY_TOKEN, null);
    }

    public String getUserId() {
        return prefs.getString(KEY_USER_ID, null);
    }

    public String getEmail() {
        return prefs.getString(KEY_EMAIL, "");
    }

    public String getDisplayName() {
        return prefs.getString(KEY_DISPLAY_NAME, "");
    }

    public String getDeviceName() {
        return prefs.getString(KEY_DEVICE_NAME, "");
    }

    public boolean getMapLicense() {
        return prefs.getBoolean(KEY_MAP_LICENSE, false);
    }

    public int getCommunityPoints() {
        return prefs.getInt(KEY_COMMUNITY_POINTS, 0);
    }

    public void saveActiveGroupId(String activeGroupId) {
        prefs.edit().putString(KEY_ACTIVE_GROUP_ID, activeGroupId).apply();
    }

    public String getActiveGroupId() {
        return prefs.getString(KEY_ACTIVE_GROUP_ID, "");
    }

    public boolean isLoggedIn() {
        String token = getToken();
        return token != null && !token.isEmpty();
    }

    public void clear() {
        prefs.edit().clear().apply();
    }
}
