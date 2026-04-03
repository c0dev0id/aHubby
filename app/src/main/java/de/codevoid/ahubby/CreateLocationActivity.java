package de.codevoid.ahubby;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.codevoid.ahubby.api.ApiClient;
import de.codevoid.ahubby.api.NominatimClient;
import de.codevoid.ahubby.auth.AuthStore;
import de.codevoid.ahubby.model.NominatimResult;

public class CreateLocationActivity extends AppCompatActivity {

    public static final String EXTRA_ID        = "loc_id";
    public static final String EXTRA_TITLE     = "loc_title";
    public static final String EXTRA_LAT       = "loc_lat";
    public static final String EXTRA_LON       = "loc_lon";
    public static final String EXTRA_COUNTRY   = "loc_country";
    public static final String EXTRA_CONTINENT = "loc_continent";
    public static final String EXTRA_CATEGORY  = "loc_category";

    static final String[] CATEGORIES = {
        "Campground", "View Point", "Fuel Station", "Restaurant",
        "Hotel", "Parking", "Ferry", "Workshop", "Photo Spot", "Other"
    };

    private String editId; // null = create mode, non-null = edit mode
    private double lat = Double.NaN;
    private double lon = Double.NaN;

    private TextInputEditText searchInput;
    private TextInputEditText titleInput;
    private TextInputEditText coordsInput;
    private TextInputEditText countryInput;
    private TextInputEditText continentInput;
    private AutoCompleteTextView categoryDropdown;

    private final NominatimClient nominatim = new NominatimClient();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_location);

        editId = getIntent().getStringExtra(EXTRA_ID);
        boolean editMode = editId != null;

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(editMode ? R.string.location_edit_title : R.string.location_create_title);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        searchInput     = findViewById(R.id.search_input);
        titleInput      = findViewById(R.id.title_input);
        coordsInput     = findViewById(R.id.coords_input);
        countryInput    = findViewById(R.id.country_input);
        continentInput  = findViewById(R.id.continent_input);
        categoryDropdown = findViewById(R.id.category_dropdown);

        coordsInput.setFocusable(false);
        coordsInput.setClickable(false);

        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, CATEGORIES);
        categoryDropdown.setAdapter(catAdapter);

        if (editMode) {
            titleInput.setText(getIntent().getStringExtra(EXTRA_TITLE));
            lat = getIntent().getDoubleExtra(EXTRA_LAT, Double.NaN);
            lon = getIntent().getDoubleExtra(EXTRA_LON, Double.NaN);
            if (!Double.isNaN(lat) && !Double.isNaN(lon)) {
                coordsInput.setText(formatCoords(lat, lon));
            }
            countryInput.setText(getIntent().getStringExtra(EXTRA_COUNTRY));
            continentInput.setText(getIntent().getStringExtra(EXTRA_CONTINENT));
            String cat = getIntent().getStringExtra(EXTRA_CATEGORY);
            categoryDropdown.setText(cat != null ? cat : CATEGORIES[0], false);

            View deleteButton = findViewById(R.id.delete_button);
            deleteButton.setVisibility(View.VISIBLE);
            deleteButton.setOnClickListener(v -> confirmDelete());
        } else {
            categoryDropdown.setText(CATEGORIES[0], false);
        }

        searchInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                doSearch();
                return true;
            }
            return false;
        });

        findViewById(R.id.search_button).setOnClickListener(v -> doSearch());
        findViewById(R.id.save_button).setOnClickListener(v -> doSave());
    }

    private void doSearch() {
        String query = searchInput.getText() != null ? searchInput.getText().toString().trim() : "";
        if (query.isEmpty()) return;

        setUiEnabled(false);
        executor.execute(() -> {
            try {
                List<NominatimResult> results = nominatim.search(query);
                runOnUiThread(() -> {
                    setUiEnabled(true);
                    if (results.isEmpty()) {
                        Toast.makeText(this, R.string.location_search_no_results, Toast.LENGTH_SHORT).show();
                    } else {
                        showResultsPicker(results);
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    setUiEnabled(true);
                    Toast.makeText(this, R.string.location_search_failed, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void showResultsPicker(List<NominatimResult> results) {
        String[] labels = new String[results.size()];
        for (int i = 0; i < results.size(); i++) labels[i] = results.get(i).shortLabel();

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.location_search_pick)
                .setItems(labels, (dialog, which) -> onResultSelected(results.get(which)))
                .show();
    }

    private void onResultSelected(NominatimResult result) {
        try {
            lat = Double.parseDouble(result.lat);
            lon = Double.parseDouble(result.lon);
            coordsInput.setText(formatCoords(lat, lon));
        } catch (NumberFormatException ignored) {
        }
        if (!result.country.isEmpty()) {
            countryInput.setText(result.country);
            continentInput.setText(NominatimClient.continentForCode(result.countryCode));
        }
        if (titleInput.getText() == null || titleInput.getText().toString().isEmpty()) {
            String[] parts = result.displayName.split(",", 2);
            titleInput.setText(parts[0].trim());
        }
        titleInput.requestFocus();
    }

    private void doSave() {
        String title     = text(titleInput);
        String country   = text(countryInput);
        String continent = text(continentInput);
        String category  = categoryDropdown.getText().toString().trim();

        if (title.isEmpty() || Double.isNaN(lat) || Double.isNaN(lon)
                || country.isEmpty() || continent.isEmpty() || category.isEmpty()) {
            Toast.makeText(this, R.string.location_save_fields_required, Toast.LENGTH_SHORT).show();
            return;
        }

        setUiEnabled(false);
        List<String> categories = Arrays.asList(category);
        ApiClient api = new ApiClient(new AuthStore(this));
        executor.execute(() -> {
            try {
                if (editId != null) {
                    api.updateLocation(editId, title, lat, lon, continent, country, categories, category);
                } else {
                    api.createLocation(title, lat, lon, continent, country, categories, category);
                }
                runOnUiThread(() -> {
                    setResult(RESULT_OK);
                    finish();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    setUiEnabled(true);
                    Toast.makeText(this,
                            getString(R.string.location_save_failed, e.getMessage()),
                            Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void confirmDelete() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.location_delete_confirm_title)
                .setMessage(R.string.location_delete_confirm_message)
                .setPositiveButton(R.string.location_delete, (d, w) -> doDelete())
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void doDelete() {
        setUiEnabled(false);
        ApiClient api = new ApiClient(new AuthStore(this));
        executor.execute(() -> {
            try {
                api.deleteLocation(editId);
                runOnUiThread(() -> {
                    setResult(RESULT_OK);
                    finish();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    setUiEnabled(true);
                    Toast.makeText(this,
                            getString(R.string.location_save_failed, e.getMessage()),
                            Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void setUiEnabled(boolean enabled) {
        searchInput.setEnabled(enabled);
        findViewById(R.id.search_button).setEnabled(enabled);
        titleInput.setEnabled(enabled);
        countryInput.setEnabled(enabled);
        continentInput.setEnabled(enabled);
        categoryDropdown.setEnabled(enabled);
        findViewById(R.id.save_button).setEnabled(enabled);
        View deleteButton = findViewById(R.id.delete_button);
        if (deleteButton.getVisibility() == View.VISIBLE) deleteButton.setEnabled(enabled);
    }

    private String text(TextInputEditText field) {
        return field.getText() != null ? field.getText().toString().trim() : "";
    }

    private static String formatCoords(double lat, double lon) {
        return String.format("%.6f, %.6f", lat, lon);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
