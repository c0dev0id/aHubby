package de.codevoid.ahubby.api;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
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
        public final String activeGroupId;

        LoginResult(String userToken, String userId, String email, String displayName,
                    String deviceName, boolean mapLicense, int communityPoints,
                    String activeGroupId) {
            this.userToken = userToken;
            this.userId = userId;
            this.email = email;
            this.displayName = displayName;
            this.deviceName = deviceName;
            this.mapLicense = mapLicense;
            this.communityPoints = communityPoints;
            this.activeGroupId = activeGroupId;
        }
    }

    private static final String BASE_URL = "https://app.advhub.net";

    private final AuthStore store;
    private final DebugLog log;

    public ApiClient(AuthStore store) {
        this.store = store;
        this.log = DebugLog.getInstance(store.getContext());
    }

    // -------------------------------------------------------------------------
    // Auth
    // -------------------------------------------------------------------------

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

            if (status != 200) throw new IOException("HTTP " + status);

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
                    json.optInt("community_points", 0),
                    json.optString("active_group_id", "")
            );
        } finally {
            conn.disconnect();
        }
    }

    // -------------------------------------------------------------------------
    // GPX
    // -------------------------------------------------------------------------

    /**
     * Returns a flat JSON array of all GPX files across all folders, by traversing
     * the folder tree returned by the iOS API.
     */
    public String fetchGpxList() throws IOException, JSONException {
        JSONArray all = new JSONArray();
        fetchGpxFolder(null, all);
        return all.toString();
    }

    private void fetchGpxFolder(String folderId, JSONArray collector) throws IOException, JSONException {
        String url = BASE_URL + "/api/ios/my-gpx?action=list";
        if (folderId != null) url += "&folder_id=" + folderId;
        String body = get(url);
        JSONObject resp = new JSONObject(body);

        JSONArray files = resp.optJSONArray("files");
        if (files != null) {
            for (int i = 0; i < files.length(); i++) collector.put(files.getJSONObject(i));
        }

        JSONArray folders = resp.optJSONArray("folders");
        if (folders != null) {
            for (int i = 0; i < folders.length(); i++) {
                JSONObject folder = folders.getJSONObject(i);
                if (folder.optInt("file_count", 0) > 0 || folder.optInt("folder_count", 0) > 0) {
                    String id = folder.optString("id", "");
                    if (!id.isEmpty()) fetchGpxFolder(id, collector);
                }
            }
        }
    }

    public void toggleGpxVisibility(String id, boolean show) throws IOException, JSONException {
        JSONObject body = new JSONObject()
                .put("action", "toggle_show_on_map")
                .put("gpx_id", id)
                .put("show_on_map", show);
        post(BASE_URL + "/api/ios/my-gpx", body);
    }

    /**
     * Downloads GPX file content as raw bytes.
     * Uses Bearer auth only — no hub session required.
     */
    public byte[] downloadGpx(String id) throws IOException {
        String bearer = store.getIosBearerToken();
        log.log(">> GET /api/ios/gpx/" + id + "/content");
        HttpURLConnection conn = (HttpURLConnection)
                new URL(BASE_URL + "/api/ios/gpx/" + id + "/content").openConnection();
        try {
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bearer " + bearer);
            conn.setConnectTimeout(15_000);
            conn.setReadTimeout(60_000);
            int status = conn.getResponseCode();
            log.log("<< GET /api/ios/gpx/" + id + "/content  " + status);
            if (status != 200) throw new IOException("HTTP " + status);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            try (InputStream is = conn.getInputStream()) {
                int n;
                while ((n = is.read(buf)) != -1) bos.write(buf, 0, n);
            }
            return bos.toByteArray();
        } finally {
            conn.disconnect();
        }
    }

    /**
     * GPX upload — endpoint not yet discovered.
     * Use getPlannerSession() + hub upload once the native endpoint is found.
     */
    public void uploadGpx(android.net.Uri fileUri) throws IOException {
        throw new UnsupportedOperationException("GPX upload endpoint not yet discovered");
    }

    // -------------------------------------------------------------------------
    // Locations
    // -------------------------------------------------------------------------

    /**
     * Returns a JSON array string of the user's locations.
     */
    public String fetchLocationsList() throws IOException, JSONException {
        String body = get(BASE_URL + "/api/ios/my-locations");
        JSONArray locations = new JSONObject(body).optJSONArray("locations");
        return locations != null ? locations.toString() : "[]";
    }

    public void toggleLocationVisibility(String id, boolean show) throws IOException, JSONException {
        JSONObject body = new JSONObject()
                .put("action", "update")
                .put("location_id", id)
                .put("show_on_map", show);
        post(BASE_URL + "/api/ios/my-locations", body);
    }

    public void createLocation(String title, double lat, double lon,
            String continent, String country,
            java.util.List<String> categories, String mainCategory)
            throws IOException, JSONException {
        JSONArray cats = new JSONArray();
        for (String c : categories) cats.put(c);
        JSONObject body = new JSONObject()
                .put("action", "create")
                .put("title", title)
                .put("latitude", lat)
                .put("longitude", lon)
                .put("continent", continent)
                .put("country", country)
                .put("category", cats)
                .put("main_category", mainCategory)
                .put("public", false)
                .put("show_on_map", false);
        post(BASE_URL + "/api/ios/my-locations", body);
    }

    public void updateLocation(String id, String title, double lat, double lon,
            String continent, String country,
            java.util.List<String> categories, String mainCategory)
            throws IOException, JSONException {
        JSONArray cats = new JSONArray();
        for (String c : categories) cats.put(c);
        JSONObject body = new JSONObject()
                .put("action", "update")
                .put("location_id", id)
                .put("title", title)
                .put("latitude", lat)
                .put("longitude", lon)
                .put("continent", continent)
                .put("country", country)
                .put("category", cats)
                .put("main_category", mainCategory);
        post(BASE_URL + "/api/ios/my-locations", body);
    }

    public void deleteLocation(String id) throws IOException, JSONException {
        JSONObject body = new JSONObject()
                .put("action", "delete")
                .put("location_id", id);
        post(BASE_URL + "/api/ios/my-locations", body);
    }

    // -------------------------------------------------------------------------
    // Live location
    // -------------------------------------------------------------------------

    /**
     * Sends a live GPS position update. Rate-limited to ~1 Hz by the server;
     * exceeding the limit is silently ignored — the next scheduled call succeeds.
     *
     * @param speedKmh  speed in km/h (convert from Android m/s with * 3.6)
     * @param headingDeg bearing in degrees 0–360
     * @param altitudeM  altitude in metres
     * @param accuracyM  horizontal accuracy in metres
     */
    public void sendLiveLocation(double lat, double lon,
                                 float speedKmh, float headingDeg,
                                 double altitudeM, float accuracyM)
            throws IOException, JSONException {
        JSONObject data = new JSONObject()
                .put("_id", store.getUserId())
                .put("lat", lat)
                .put("lon", lon)
                .put("speed", speedKmh)
                .put("heading", (int) headingDeg)
                .put("altitude", altitudeM)
                .put("accuracy", accuracyM);
        String resp = post(BASE_URL + "/api/location_update.php", new JSONObject().put("data", data));
        JSONObject json = new JSONObject(resp);
        if (json.has("error")) {
            String err = json.optString("error", "");
            if (err.contains("Rate limit")) return;
            throw new IOException(err);
        }
    }

    // -------------------------------------------------------------------------
    // Navigate-to
    // -------------------------------------------------------------------------

    public void sendNavigateTo(double lat, double lon, String name) throws IOException, JSONException {
        JSONObject body = new JSONObject()
                .put("action", "set")
                .put("lat", lat)
                .put("lng", lon)
                .put("name", name);
        post(BASE_URL + "/api/ios/navigate-to", body);
    }

    // -------------------------------------------------------------------------
    // Token refresh
    // -------------------------------------------------------------------------

    /**
     * Re-authenticates using stored credentials. Synchronized to prevent concurrent
     * re-auth attempts. Updates stored token and login timestamp on success.
     */
    private synchronized void refreshToken(String staleRawToken) throws IOException, JSONException {
        String current = store.getToken();
        if (current != null && !current.equals(staleRawToken)) {
            return; // another thread already refreshed
        }
        String email = store.getEmail();
        String password = store.getPassword();
        if (email == null || email.isEmpty() || password == null || password.isEmpty()) {
            throw new AuthException("No stored credentials — please log in again");
        }
        try {
            LoginResult result = login(email, password);
            store.save(result.userToken, result.userId);
            store.saveLoginTimestamp(System.currentTimeMillis() / 1000L);
            store.saveProfile(result.email, result.displayName, result.deviceName,
                    result.mapLicense, result.communityPoints);
        } catch (IOException e) {
            throw new AuthException("Re-authentication failed: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // HTTP helpers
    // -------------------------------------------------------------------------

    private String get(String url) throws IOException, JSONException {
        String staleRawToken = store.getToken();
        int[] statusHolder = new int[1];
        String body = doGet(url, store.getIosBearerToken(), statusHolder);
        if (statusHolder[0] == 401) {
            refreshToken(staleRawToken);
            body = doGet(url, store.getIosBearerToken(), statusHolder);
            if (statusHolder[0] == 401) throw new AuthException("Session expired — please log in again");
        }
        if (statusHolder[0] != 200) throw new IOException("HTTP " + statusHolder[0] + ": " + body);
        return body;
    }

    private String post(String url, JSONObject json) throws IOException, JSONException {
        String staleRawToken = store.getToken();
        int[] statusHolder = new int[1];
        String body = doPost(url, store.getIosBearerToken(), json, statusHolder);
        if (statusHolder[0] == 401) {
            refreshToken(staleRawToken);
            body = doPost(url, store.getIosBearerToken(), json, statusHolder);
            if (statusHolder[0] == 401) throw new AuthException("Session expired — please log in again");
        }
        if (statusHolder[0] != 200) throw new IOException("HTTP " + statusHolder[0] + ": " + body);
        return body;
    }

    private String doGet(String url, String bearer, int[] statusHolder) throws IOException {
        String path = url.substring(BASE_URL.length());
        log.log(">> GET " + path);

        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        try {
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bearer " + bearer);
            conn.setConnectTimeout(15_000);
            conn.setReadTimeout(15_000);

            int status = conn.getResponseCode();
            statusHolder[0] = status;
            InputStream is = status < 400 ? conn.getInputStream() : conn.getErrorStream();
            String body = is != null ? readStream(is) : "";
            log.log("<< GET " + path + "  " + status);
            return body;
        } finally {
            conn.disconnect();
        }
    }

    private String doPost(String url, String bearer, JSONObject json, int[] statusHolder) throws IOException {
        String path = url.substring(BASE_URL.length());
        log.log(">> POST " + path + "\n   " + json);

        byte[] body = json.toString().getBytes(StandardCharsets.UTF_8);
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        try {
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer " + bearer);
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
