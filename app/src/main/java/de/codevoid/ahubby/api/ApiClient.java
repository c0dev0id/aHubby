package de.codevoid.ahubby.api;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

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

    private final HttpClient http = HttpClient.newHttpClient();

    public LoginResult login(String email, String password)
            throws IOException, InterruptedException, JSONException {
        String body = new JSONObject()
                .put("email", email)
                .put("password", password)
                .toString();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/dmd_connector.php"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 401) {
            throw new IOException("Invalid credentials");
        }
        if (response.statusCode() != 200) {
            throw new IOException("HTTP " + response.statusCode());
        }

        JSONObject json = new JSONObject(response.body());
        String token = json.optString("user_token", null);
        if (token == null || token.isEmpty()) {
            String msg = json.optString("message", json.optString("error", "Login failed"));
            throw new IOException(msg);
        }

        return new LoginResult(token, json.optString("_id", ""));
    }
}
