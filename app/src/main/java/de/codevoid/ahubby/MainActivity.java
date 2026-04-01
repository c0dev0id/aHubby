package de.codevoid.ahubby;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.codevoid.ahubby.api.ApiClient;
import de.codevoid.ahubby.auth.AuthStore;

public class MainActivity extends AppCompatActivity {

    private TextView apiOutput;
    private final ApiClient apiClient = new ApiClient();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AuthStore auth = new AuthStore(this);
        if (!auth.isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        String token = auth.getToken();

        ((TextView) findViewById(R.id.token_text)).setText(token);
        apiOutput = findViewById(R.id.api_output);

        findViewById(R.id.fetch_gpx_button).setOnClickListener(v -> fetch(
                () -> apiClient.fetchGpxList(token)));

        findViewById(R.id.fetch_locations_button).setOnClickListener(v -> fetch(
                () -> apiClient.fetchLocationsList(token)));

        findViewById(R.id.logout_button).setOnClickListener(v -> {
            auth.clear();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    private void fetch(ApiCall call) {
        apiOutput.setText("…");
        executor.execute(() -> {
            try {
                String result = call.execute();
                runOnUiThread(() -> apiOutput.setText(result));
            } catch (Exception e) {
                runOnUiThread(() -> apiOutput.setText("Error: " + e.getMessage()));
            }
        });
    }

    interface ApiCall {
        String execute() throws Exception;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
