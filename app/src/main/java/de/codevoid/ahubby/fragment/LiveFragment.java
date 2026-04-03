package de.codevoid.ahubby.fragment;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.switchmaterial.SwitchMaterial;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import de.codevoid.ahubby.R;
import de.codevoid.ahubby.api.ApiClient;
import de.codevoid.ahubby.auth.AuthStore;

public class LiveFragment extends Fragment implements LocationListener {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final long GPS_MIN_TIME_MS = 500;
    private static final float GPS_MIN_DIST_M = 0;

    private TextView positionText;
    private TextView groupText;
    private TextView statusText;
    private SwitchMaterial shareToggle;

    private LocationManager locationManager;
    private Location lastLocation;

    private ApiClient apiClient;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> sendTask;

    private String activeGroupId;

    private final ActivityResultLauncher<String> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    startGps();
                } else {
                    statusText.setText(R.string.live_permission_denied);
                    shareToggle.setEnabled(false);
                }
            });

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle saved) {
        return inflater.inflate(R.layout.fragment_live, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle saved) {
        positionText = view.findViewById(R.id.live_position_text);
        groupText = view.findViewById(R.id.live_group_text);
        statusText = view.findViewById(R.id.live_status_text);
        shareToggle = view.findViewById(R.id.live_share_toggle);

        AuthStore store = new AuthStore(requireContext());
        apiClient = new ApiClient(store);
        activeGroupId = store.getActiveGroupId();

        groupText.setText(activeGroupId.isEmpty()
                ? getString(R.string.live_no_group)
                : activeGroupId);

        shareToggle.setOnCheckedChangeListener((btn, checked) -> {
            if (checked) {
                startSending();
            } else {
                stopSending();
            }
        });

        requestLocationPermission();
    }

    private void requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            startGps();
        } else {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    private void startGps() {
        locationManager = (LocationManager) requireContext().getSystemService(android.content.Context.LOCATION_SERVICE);
        try {
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, GPS_MIN_TIME_MS, GPS_MIN_DIST_M, this);
            Location last = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (last != null) onLocationChanged(last);
        } catch (SecurityException ignored) {
            statusText.setText(R.string.live_permission_denied);
            shareToggle.setEnabled(false);
        }
    }

    private void startSending() {
        if (sendTask != null && !sendTask.isDone()) return;
        sendTask = scheduler.scheduleAtFixedRate(this::sendLocation, 0, 1, TimeUnit.SECONDS);
    }

    private void stopSending() {
        if (sendTask != null) {
            sendTask.cancel(false);
            sendTask = null;
        }
        requireActivity().runOnUiThread(() ->
                statusText.setText(R.string.live_status_idle));
    }

    private void sendLocation() {
        Location loc = lastLocation;
        if (loc == null) return;
        try {
            apiClient.sendLiveLocation(
                    loc.getLatitude(), loc.getLongitude(),
                    loc.getSpeed() * 3.6f,
                    loc.getBearing(),
                    loc.getAltitude(),
                    loc.getAccuracy());
            String time = LocalTime.now().format(TIME_FMT);
            requireActivity().runOnUiThread(() ->
                    statusText.setText(getString(R.string.live_status_sending, time)));
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : "unknown error";
            requireActivity().runOnUiThread(() ->
                    statusText.setText(getString(R.string.live_status_error, msg)));
        }
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        lastLocation = location;
        String pos = String.format("%.5f, %.5f  ±%.0fm",
                location.getLatitude(), location.getLongitude(), location.getAccuracy());
        requireActivity().runOnUiThread(() -> positionText.setText(pos));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopSending();
        if (locationManager != null) {
            locationManager.removeUpdates(this);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        scheduler.shutdownNow();
    }
}
