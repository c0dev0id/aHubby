package de.codevoid.ahubby;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import de.codevoid.ahubby.auth.AuthStore;

public class MainActivity extends AppCompatActivity {

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

        findViewById(R.id.logout_button).setOnClickListener(v -> {
            auth.clear();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }
}
