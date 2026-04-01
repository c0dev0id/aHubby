package de.codevoid.ahubby.api;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

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

    public LoginResult login(String email, String password) throws IOException, JSONException {
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
                throw new IOException("Invalid credentials");
            }

            InputStream is = status < 400 ? conn.getInputStream() : conn.getErrorStream();
            String responseBody = readStream(is);

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

    public String fetchGpxList(String token) throws IOException {
        return get(BASE_URL + "/api/gpx_proxy.php?action=list", token);
    }

    public String fetchLocationsList(String token) throws IOException {
        return get(BASE_URL + "/api/locations_proxy.php?action=list", token);
    }

    public void toggleGpxVisibility(String token, String id, boolean show) throws IOException, JSONException {
        JSONObject data = new JSONObject().put("_id", id).put("show_on_map", show);
        JSONObject body = new JSONObject().put("data", data);
        post(BASE_URL + "/api/gpx_proxy.php?action=update_visibility", token, body);
    }

    public void toggleLocationVisibility(String token, String id, boolean show) throws IOException, JSONException {
        JSONObject data = new JSONObject().put("_id", id).put("show_on_map", show);
        JSONObject body = new JSONObject().put("data", data);
        post(BASE_URL + "/api/locations_proxy.php?action=update", token, body);
    }

    private String get(String url, String token) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        try {
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bearer " + token);
            conn.setConnectTimeout(15_000);
            conn.setReadTimeout(15_000);

            int status = conn.getResponseCode();
            InputStream is = status < 400 ? conn.getInputStream() : conn.getErrorStream();
            String body = readStream(is);

            if (status != 200) {
                throw new IOException("HTTP " + status + ": " + body);
            }
            return body;
        } finally {
            conn.disconnect();
        }
    }

    private void post(String url, String token, JSONObject json) throws IOException {
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
            if (status != 200) {
                InputStream es = conn.getErrorStream();
                String err = es != null ? readStream(es) : "";
                throw new IOException("HTTP " + status + ": " + err);
            }
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
