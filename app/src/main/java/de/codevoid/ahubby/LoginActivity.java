package de.codevoid.ahubby;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.codevoid.ahubby.api.ApiClient;
import de.codevoid.ahubby.auth.AuthStore;
import de.codevoid.ahubby.debug.DebugActivity;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText emailInput;
    private TextInputEditText passwordInput;
    private Button loginButton;
    private ProgressBar progress;
    private TextView errorText;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        emailInput = findViewById(R.id.email_input);
        passwordInput = findViewById(R.id.password_input);
        loginButton = findViewById(R.id.login_button);
        progress = findViewById(R.id.progress);
        errorText = findViewById(R.id.error_text);

        loginButton.setOnClickListener(v -> attemptLogin());
        findViewById(R.id.debug_button).setOnClickListener(
                v -> startActivity(new Intent(this, DebugActivity.class)));
    }

    private void attemptLogin() {
        String email = emailInput.getText() != null ? emailInput.getText().toString().trim() : "";
        String password = passwordInput.getText() != null ? passwordInput.getText().toString() : "";

        if (email.isEmpty() || password.isEmpty()) {
            showError(getString(R.string.error_fields_required));
            return;
        }

        setLoading(true);

        executor.execute(() -> {
            try {
                AuthStore store = new AuthStore(this);
                ApiClient.LoginResult result = new ApiClient(store).login(email, password);
                store.saveCredentials(email, password);
                store.save(result.userToken, result.userId);
                store.saveLoginTimestamp(System.currentTimeMillis() / 1000L);
                store.saveProfile(result.email, result.displayName, result.deviceName,
                        result.mapLicense, result.communityPoints);
                store.saveActiveGroupId(result.activeGroupId);
                runOnUiThread(this::onLoginSuccess);
            } catch (Exception e) {
                runOnUiThread(() -> {
                    setLoading(false);
                    showError(e.getMessage() != null ? e.getMessage() : getString(R.string.error_login_failed));
                });
            }
        });
    }

    private void onLoginSuccess() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private void showError(String message) {
        errorText.setText(message);
        errorText.setVisibility(View.VISIBLE);
    }

    private void setLoading(boolean loading) {
        loginButton.setEnabled(!loading);
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        errorText.setVisibility(View.GONE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
