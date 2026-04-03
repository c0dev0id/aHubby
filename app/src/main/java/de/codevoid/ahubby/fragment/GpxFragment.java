package de.codevoid.ahubby.fragment;

import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.codevoid.ahubby.LoginActivity;
import de.codevoid.ahubby.R;
import de.codevoid.ahubby.adapter.GpxAdapter;
import de.codevoid.ahubby.api.ApiClient;
import de.codevoid.ahubby.auth.AuthException;
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
            ApiClient api = new ApiClient(new AuthStore(requireContext()));
            executor.execute(() -> {
                try {
                    api.toggleGpxVisibility(file.id, newValue);
                } catch (AuthException e) {
                    requireActivity().runOnUiThread(this::redirectToLogin);
                } catch (Exception e) {
                    requireActivity().runOnUiThread(() -> {
                        file.showOnMap = !newValue;
                        adapter.notifyDataSetChanged();
                        Snackbar.make(requireView(), R.string.error_toggle_failed, Snackbar.LENGTH_SHORT).show();
                    });
                }
            });
        });

        adapter.setDownloadListener(file -> {
            ApiClient api = new ApiClient(new AuthStore(requireContext()));
            executor.execute(() -> {
                try {
                    byte[] data = api.downloadGpx(file.id);
                    String filename = sanitizeFilename(file.title) + ".gpx";
                    ContentValues cv = new ContentValues();
                    cv.put(MediaStore.Downloads.DISPLAY_NAME, filename);
                    cv.put(MediaStore.Downloads.MIME_TYPE, "application/gpx+xml");
                    cv.put(MediaStore.Downloads.RELATIVE_PATH, "Download/");
                    Uri uri = requireContext().getContentResolver()
                            .insert(MediaStore.Downloads.getContentUri("external"), cv);
                    if (uri == null) throw new Exception("Could not create file in Downloads");
                    try (OutputStream os = requireContext().getContentResolver().openOutputStream(uri)) {
                        if (os == null) throw new Exception("Could not open output stream");
                        os.write(data);
                    }
                    requireActivity().runOnUiThread(() ->
                            Snackbar.make(requireView(),
                                    getString(R.string.gpx_download_ok, filename),
                                    Snackbar.LENGTH_LONG).show());
                } catch (Exception e) {
                    String msg = e.getMessage() != null ? e.getMessage() : "unknown error";
                    requireActivity().runOnUiThread(() ->
                            Snackbar.make(requireView(),
                                    getString(R.string.gpx_download_failed, msg),
                                    Snackbar.LENGTH_LONG).show());
                }
            });
        });

        view.findViewById(R.id.upload_button).setOnClickListener(v ->
                Snackbar.make(requireView(), R.string.gpx_upload_not_available, Snackbar.LENGTH_SHORT).show());

        filterButton.setOnClickListener(v -> showCountryFilter());

        loadData();
    }

    private void loadData() {
        progress.setVisibility(View.VISIBLE);
        emptyText.setVisibility(View.GONE);

        ApiClient api = new ApiClient(new AuthStore(requireContext()));
        executor.execute(() -> {
            try {
                String json = api.fetchGpxList();
                List<GpxFile> files = GpxFile.parseList(json);
                requireActivity().runOnUiThread(() -> {
                    progress.setVisibility(View.GONE);
                    adapter.setItems(files);
                    emptyText.setVisibility(files.isEmpty() ? View.VISIBLE : View.GONE);
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

    private static String sanitizeFilename(String title) {
        return title.replaceAll("[^a-zA-Z0-9._\\- ]", "_").trim();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        executor.shutdown();
    }
}
