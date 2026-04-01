package de.codevoid.ahubby;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.navigationrail.NavigationRailView;

import de.codevoid.ahubby.auth.AuthStore;
import de.codevoid.ahubby.fragment.GpxFragment;
import de.codevoid.ahubby.fragment.LiveFragment;
import de.codevoid.ahubby.fragment.LocationsFragment;
import de.codevoid.ahubby.fragment.ProfileFragment;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!new AuthStore(this).isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        NavigationRailView navRail = findViewById(R.id.nav_rail);

        if (savedInstanceState == null) {
            showFragment(new GpxFragment());
            navRail.setSelectedItemId(R.id.nav_gpx);
        }

        navRail.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_gpx) showFragment(new GpxFragment());
            else if (id == R.id.nav_locations) showFragment(new LocationsFragment());
            else if (id == R.id.nav_live) showFragment(new LiveFragment());
            else if (id == R.id.nav_profile) showFragment(new ProfileFragment());
            return true;
        });
    }

    private void showFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }
}
