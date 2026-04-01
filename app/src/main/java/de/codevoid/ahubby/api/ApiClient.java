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

        LoginResult(String userToken, String userId) {
            this.userToken = userToken;
            this.userId = userId;
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

            return new LoginResult(token, json.optString("_id", ""));
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
