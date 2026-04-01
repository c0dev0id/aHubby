package de.codevoid.ahubby.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.codevoid.ahubby.R;
import de.codevoid.ahubby.adapter.GpxAdapter;
import de.codevoid.ahubby.api.ApiClient;
import de.codevoid.ahubby.auth.AuthStore;
import de.codevoid.ahubby.model.GpxFile;

public class GpxFragment extends Fragment {

    private GpxAdapter adapter;
    private ProgressBar progress;
    private TextView emptyText;
    private MaterialButton filterButton;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle saved) {
        return inflater.inflate(R.layout.fragment_gpx, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle saved) {
        progress = view.findViewById(R.id.progress);
        emptyText = view.findViewById(R.id.empty_text);
        filterButton = view.findViewById(R.id.filter_button);

        RecyclerView recycler = view.findViewById(R.id.recycler);
        adapter = new GpxAdapter();
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        recycler.setAdapter(adapter);

        adapter.setToggleListener((file, newValue) -> {
            String token = new AuthStore(requireContext()).getToken();
            executor.execute(() -> {
                try {
                    new ApiClient().toggleGpxVisibility(token, file.id, newValue);
                } catch (Exception e) {
                    requireActivity().runOnUiThread(() -> {
                        file.showOnMap = !newValue;
                        adapter.notifyDataSetChanged();
                        Snackbar.make(requireView(), R.string.error_toggle_failed, Snackbar.LENGTH_SHORT).show();
                    });
                }
            });
        });

        filterButton.setOnClickListener(v -> showCountryFilter());

        loadData();
    }

    private void loadData() {
        progress.setVisibility(View.VISIBLE);
        emptyText.setVisibility(View.GONE);

        String token = new AuthStore(requireContext()).getToken();
        executor.execute(() -> {
            try {
                String json = new ApiClient().fetchGpxList(token);
                List<GpxFile> files = GpxFile.parseList(json);
                requireActivity().runOnUiThread(() -> {
                    progress.setVisibility(View.GONE);
                    adapter.setItems(files);
                    emptyText.setVisibility(files.isEmpty() ? View.VISIBLE : View.GONE);
                });
            } catch (Exception e) {
                requireActivity().runOnUiThread(() -> {
                    progress.setVisibility(View.GONE);
                    Snackbar.make(requireView(), R.string.error_load_failed, Snackbar.LENGTH_LONG).show();
                });
            }
        });
    }

    private void showCountryFilter() {
        List<String> countries = adapter.getCountries();
        String[] entries = new String[countries.size() + 1];
        entries[0] = getString(R.string.filter_all);
        for (int i = 0; i < countries.size(); i++) entries[i + 1] = countries.get(i);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.filter_country)
                .setItems(entries, (dialog, which) -> {
                    if (which == 0) {
                        adapter.setCountryFilter(null);
                        filterButton.setText(R.string.filter_all);
                    } else {
                        String country = countries.get(which - 1);
                        adapter.setCountryFilter(country);
                        filterButton.setText(country);
                    }
                })
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        executor.shutdown();
    }
}
