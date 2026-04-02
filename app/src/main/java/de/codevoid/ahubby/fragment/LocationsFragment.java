package de.codevoid.ahubby.fragment;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.codevoid.ahubby.CreateLocationActivity;
import de.codevoid.ahubby.LoginActivity;
import de.codevoid.ahubby.R;
import de.codevoid.ahubby.adapter.LocationsAdapter;
import de.codevoid.ahubby.api.ApiClient;
import de.codevoid.ahubby.auth.AuthException;
import de.codevoid.ahubby.auth.AuthStore;
import de.codevoid.ahubby.model.HubLocation;

public class LocationsFragment extends Fragment {

    private LocationsAdapter adapter;
    private ProgressBar progress;
    private TextView emptyText;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final ActivityResultLauncher<Intent> createLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK) loadData();
            });

    private final ActivityResultLauncher<Intent> editLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK) loadData();
            });

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle saved) {
        return inflater.inflate(R.layout.fragment_locations, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle saved) {
        progress = view.findViewById(R.id.progress);
        emptyText = view.findViewById(R.id.empty_text);

        RecyclerView recycler = view.findViewById(R.id.recycler);
        adapter = new LocationsAdapter();
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        recycler.setAdapter(adapter);

        adapter.setToggleListener((loc, newValue) -> {
            ApiClient api = new ApiClient(new AuthStore(requireContext()));
            executor.execute(() -> {
                try {
                    api.toggleLocationVisibility(loc.id, newValue);
                } catch (AuthException e) {
                    requireActivity().runOnUiThread(this::redirectToLogin);
                } catch (Exception e) {
                    requireActivity().runOnUiThread(() -> {
                        loc.showOnMap = !newValue;
                        adapter.notifyDataSetChanged();
                        Snackbar.make(requireView(), R.string.error_toggle_failed, Snackbar.LENGTH_SHORT).show();
                    });
                }
            });
        });

        adapter.setEditListener(loc -> {
            Intent intent = new Intent(requireContext(), CreateLocationActivity.class);
            intent.putExtra(CreateLocationActivity.EXTRA_ID,        loc.id);
            intent.putExtra(CreateLocationActivity.EXTRA_TITLE,     loc.title);
            intent.putExtra(CreateLocationActivity.EXTRA_COORDS,    loc.coordinates);
            intent.putExtra(CreateLocationActivity.EXTRA_COUNTRY,   loc.country);
            intent.putExtra(CreateLocationActivity.EXTRA_CONTINENT, loc.continent);
            intent.putExtra(CreateLocationActivity.EXTRA_CATEGORY,  loc.mainCategory);
            editLauncher.launch(intent);
        });

        view.findViewById(R.id.add_location_button).setOnClickListener(v ->
                createLauncher.launch(new Intent(requireContext(), CreateLocationActivity.class)));

        loadData();
    }

    private void loadData() {
        progress.setVisibility(View.VISIBLE);
        emptyText.setVisibility(View.GONE);

        ApiClient api = new ApiClient(new AuthStore(requireContext()));
        executor.execute(() -> {
            try {
                String json = api.fetchLocationsList();
                List<HubLocation> locs = HubLocation.parseList(json);
                requireActivity().runOnUiThread(() -> {
                    progress.setVisibility(View.GONE);
                    adapter.setItems(locs);
                    emptyText.setVisibility(locs.isEmpty() ? View.VISIBLE : View.GONE);
                });
            } catch (AuthException e) {
                requireActivity().runOnUiThread(this::redirectToLogin);
            } catch (Exception e) {
                requireActivity().runOnUiThread(() -> {
                    progress.setVisibility(View.GONE);
                    Snackbar.make(requireView(), R.string.error_load_failed, Snackbar.LENGTH_LONG).show();
                });
            }
        });
    }

    private void redirectToLogin() {
        new AuthStore(requireContext()).clear();
        Intent intent = new Intent(requireActivity(), LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        executor.shutdown();
    }
}
