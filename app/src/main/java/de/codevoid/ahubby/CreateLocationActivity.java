package de.codevoid.ahubby;

import android.os.Bundle;
import android.view.KeyEvent;
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

    static final String[] CATEGORIES = {
        "Campground", "View Point", "Fuel Station", "Restaurant",
        "Hotel", "Parking", "Ferry", "Workshop", "Photo Spot", "Other"
    };

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

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        searchInput = findViewById(R.id.search_input);
        titleInput = findViewById(R.id.title_input);
        coordsInput = findViewById(R.id.coords_input);
        countryInput = findViewById(R.id.country_input);
        continentInput = findViewById(R.id.continent_input);
        categoryDropdown = findViewById(R.id.category_dropdown);

        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, CATEGORIES);
        categoryDropdown.setAdapter(catAdapter);
        categoryDropdown.setText(CATEGORIES[0], false);

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
        coordsInput.setText(result.coordinates());
        if (!result.country.isEmpty()) {
            countryInput.setText(result.country);
            continentInput.setText(NominatimClient.continentForCode(result.countryCode));
        }
        // Populate title only if empty
        if (titleInput.getText() == null || titleInput.getText().toString().isEmpty()) {
            // Use only the first part of display_name as a suggested title
            String[] parts = result.displayName.split(",", 2);
            titleInput.setText(parts[0].trim());
        }
        titleInput.requestFocus();
    }

    private void doSave() {
        String title = text(titleInput);
        String coords = text(coordsInput);
        String country = text(countryInput);
        String continent = text(continentInput);
        String category = categoryDropdown.getText().toString().trim();

        if (title.isEmpty() || coords.isEmpty() || country.isEmpty()
                || continent.isEmpty() || category.isEmpty()) {
            Toast.makeText(this, R.string.location_save_fields_required, Toast.LENGTH_SHORT).show();
            return;
        }

        setUiEnabled(false);
        List<String> categories = Arrays.asList(category);
        executor.execute(() -> {
            try {
                new ApiClient(new AuthStore(this))
                        .createLocation(title, coords, continent, country, categories, category);
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
        coordsInput.setEnabled(enabled);
        countryInput.setEnabled(enabled);
        continentInput.setEnabled(enabled);
        categoryDropdown.setEnabled(enabled);
        findViewById(R.id.save_button).setEnabled(enabled);
    }

    private String text(TextInputEditText field) {
        return field.getText() != null ? field.getText().toString().trim() : "";
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
