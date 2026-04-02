package de.codevoid.ahubby.api;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import de.codevoid.ahubby.auth.AuthException;
import de.codevoid.ahubby.auth.AuthStore;
import de.codevoid.ahubby.debug.DebugLog;

public class ApiClient {

    public static class LoginResult {
        public final String userToken;
        public final String userId;
        public final String email;
        public final String displayName;
        public final String deviceName;
        public final boolean mapLicense;
        public final int communityPoints;

        LoginResult(String userToken, String userId, String email, String displayName,
                    String deviceName, boolean mapLicense, int communityPoints) {
            this.userToken = userToken;
            this.userId = userId;
            this.email = email;
            this.displayName = displayName;
            this.deviceName = deviceName;
            this.mapLicense = mapLicense;
            this.communityPoints = communityPoints;
        }
    }

    private static final String BASE_URL = "https://app.advhub.net";

    private final AuthStore store;
    private final DebugLog log;

    public ApiClient(AuthStore store) {
        this.store = store;
        this.log = DebugLog.getInstance(store.getContext());
    }

    public LoginResult login(String email, String password) throws IOException, JSONException {
        JSONObject reqJson = new JSONObject().put("email", email).put("password", "[redacted]");
        log.log(">> POST /api/dmd_connector.php\n   " + reqJson);

        byte[] body = new JSONObject()
                .put("email", email)
                .put("password", password)
                .toString()
                .getBytes(StandardCharsets.UTF_8);

        HttpURLConnection conn = (HttpURLConnection) new URL(BASE_URL + "/api/dmd_connector.php").openConnection();
        try {
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(15_000);
            conn.setReadTimeout(15_000);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body);
            }

            int status = conn.getResponseCode();
            if (status == 401) {
                log.log("<< POST /api/dmd_connector.php  401 Invalid credentials");
                throw new IOException("Invalid credentials");
            }

            InputStream is = status < 400 ? conn.getInputStream() : conn.getErrorStream();
            String responseBody = readStream(is);
            log.log("<< POST /api/dmd_connector.php  " + status + "\n   " + responseBody);

            if (status != 200) {
                throw new IOException("HTTP " + status);
            }

            JSONObject json = new JSONObject(responseBody);
            String token = json.optString("user_token", null);
            if (token == null || token.isEmpty()) {
                String msg = json.optString("message", json.optString("error", "Login failed"));
                throw new IOException(msg);
            }

            return new LoginResult(
                    token,
                    json.optString("_id", ""),
                    json.optString("email", email),
                    json.optString("displayname", ""),
                    json.optString("active_device_name", ""),
                    "true".equals(json.optString("map_license", "false")),
                    json.optInt("community_points", 0)
            );
        } finally {
            conn.disconnect();
        }
    }

    public String fetchGpxList() throws IOException, JSONException {
        return get(BASE_URL + "/api/gpx_proxy.php?action=list");
    }

    public String fetchLocationsList() throws IOException, JSONException {
        return get(BASE_URL + "/api/locations_proxy.php?action=list");
    }

    public void toggleGpxVisibility(String id, boolean show) throws IOException, JSONException {
        JSONObject data = new JSONObject().put("_id", id).put("show_on_map", show);
        JSONObject body = new JSONObject().put("data", data);
        post(BASE_URL + "/api/gpx_proxy.php?action=update_visibility", body);
    }

    public void toggleLocationVisibility(String id, boolean show) throws IOException, JSONException {
        JSONObject data = new JSONObject().put("_id", id).put("show_on_map", show);
        JSONObject body = new JSONObject().put("data", data);
        post(BASE_URL + "/api/locations_proxy.php?action=update", body);
    }

    public void updateLocation(String id, String title, String coordinates, String continent,
            String country, List<String> categories, String mainCategory)
            throws IOException, JSONException {
        JSONArray cats = new JSONArray();
        for (String c : categories) cats.put(c);
        JSONObject data = new JSONObject()
                .put("_id", id)
                .put("title", title)
                .put("coordinates", coordinates)
                .put("continent", continent)
                .put("country", country)
                .put("category", cats)
                .put("main_category", mainCategory);
        post(BASE_URL + "/api/locations_proxy.php?action=update", new JSONObject().put("data", data));
    }

    public void deleteLocation(String id) throws IOException, JSONException {
        get(BASE_URL + "/api/locations_proxy.php?action=delete&id=" + id);
    }

    public String createLocation(String title, String coordinates, String continent,
            String country, List<String> categories, String mainCategory)
            throws IOException, JSONException {
        JSONArray cats = new JSONArray();
        for (String c : categories) cats.put(c);
        JSONObject data = new JSONObject()
                .put("title", title)
                .put("coordinates", coordinates)
                .put("continent", continent)
                .put("country", country)
                .put("category", cats)
                .put("main_category", mainCategory);
        return post(BASE_URL + "/api/locations_proxy.php?action=create", new JSONObject().put("data", data));
    }

    /**
     * Re-authenticates using stored credentials. Synchronized to prevent concurrent
     * re-auth attempts: if the token was already refreshed by another thread while
     * this one waited for the lock, the new token is returned without a second login call.
     *
     * @param staleToken the expired token that triggered this refresh
     * @return the new valid token
     */
    private synchronized String refreshToken(String staleToken) throws IOException, JSONException {
        String current = store.getToken();
        if (current != null && !current.equals(staleToken)) {
            return current; // another thread already refreshed
        }
        String email = store.getEmail();
        String password = store.getPassword();
        if (email == null || email.isEmpty() || password == null || password.isEmpty()) {
            throw new AuthException("No stored credentials — please log in again");
        }
        try {
            LoginResult result = login(email, password);
            store.save(result.userToken, result.userId);
            store.saveProfile(result.email, result.displayName, result.deviceName,
                    result.mapLicense, result.communityPoints);
            return result.userToken;
        } catch (IOException e) {
            throw new AuthException("Re-authentication failed: " + e.getMessage());
        }
    }

    private String get(String url) throws IOException, JSONException {
        String token = store.getToken();
        int[] statusHolder = new int[1];
        String body = doGet(url, token, statusHolder);
        if (statusHolder[0] == 401) {
            token = refreshToken(token);
            body = doGet(url, token, statusHolder);
            if (statusHolder[0] == 401) {
                throw new AuthException("Session expired — please log in again");
            }
        }
        if (statusHolder[0] != 200) {
            throw new IOException("HTTP " + statusHolder[0] + ": " + body);
        }
        return body;
    }

    private String post(String url, JSONObject json) throws IOException, JSONException {
        String token = store.getToken();
        int[] statusHolder = new int[1];
        String body = doPost(url, token, json, statusHolder);
        if (statusHolder[0] == 401) {
            token = refreshToken(token);
            body = doPost(url, token, json, statusHolder);
            if (statusHolder[0] == 401) {
                throw new AuthException("Session expired — please log in again");
            }
        }
        if (statusHolder[0] != 200) {
            throw new IOException("HTTP " + statusHolder[0] + ": " + body);
        }
        return body;
    }

    private String doGet(String url, String token, int[] statusHolder) throws IOException {
        String path = url.contains("?") ? url.substring(BASE_URL.length()) : url.substring(BASE_URL.length());
        log.log(">> GET " + path + "\n   Authorization: Bearer [redacted]");

        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        try {
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bearer " + token);
            conn.setConnectTimeout(15_000);
            conn.setReadTimeout(15_000);

            int status = conn.getResponseCode();
            statusHolder[0] = status;
            InputStream is = status < 400 ? conn.getInputStream() : conn.getErrorStream();
            String body = is != null ? readStream(is) : "";
            log.log("<< GET " + path + "  " + status + "\n   " + body);
            return body;
        } finally {
            conn.disconnect();
        }
    }

    private String doPost(String url, String token, JSONObject json, int[] statusHolder) throws IOException {
        String path = url.substring(BASE_URL.length());
        log.log(">> POST " + path + "\n   Authorization: Bearer [redacted]\n   " + json);

        byte[] body = json.toString().getBytes(StandardCharsets.UTF_8);
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        try {
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer " + token);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(15_000);
            conn.setReadTimeout(15_000);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body);
            }

            int status = conn.getResponseCode();
            statusHolder[0] = status;
            InputStream is = status < 400 ? conn.getInputStream() : conn.getErrorStream();
            String responseBody = is != null ? readStream(is) : "";
            log.log("<< POST " + path + "  " + status + (responseBody.isEmpty() ? "" : "\n   " + responseBody));
            return responseBody;
        } finally {
            conn.disconnect();
        }
    }

    private static String readStream(InputStream is) throws IOException {
        char[] buf = new char[4096];
        StringBuilder sb = new StringBuilder();
        try (InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
            int n;
            while ((n = reader.read(buf)) != -1) sb.append(buf, 0, n);
        }
        return sb.toString();
    }
}
