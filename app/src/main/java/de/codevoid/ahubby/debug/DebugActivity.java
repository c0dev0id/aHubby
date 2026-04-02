package de.codevoid.ahubby.debug;

import android.content.ContentValues;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.switchmaterial.SwitchMaterial;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import de.codevoid.ahubby.R;

public class DebugActivity extends AppCompatActivity {

    private DebugLog debugLog;
    private TextView logText;
    private ScrollView scroll;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_debug);

        debugLog = DebugLog.getInstance(this);

        SwitchMaterial toggle = findViewById(R.id.debug_toggle);
        logText = findViewById(R.id.log_text);
        scroll = findViewById(R.id.scroll);

        toggle.setChecked(debugLog.isEnabled());
        toggle.setOnCheckedChangeListener((btn, checked) -> debugLog.setEnabled(checked));

        findViewById(R.id.btn_clear).setOnClickListener(v -> {
            debugLog.clear();
            refreshLog();
        });

        findViewById(R.id.btn_save).setOnClickListener(v -> saveLog());
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshLog();
    }

    private void refreshLog() {
        List<String> entries = debugLog.getEntries();
        if (entries.isEmpty()) {
            logText.setText(getString(R.string.debug_empty));
        } else {
            StringBuilder sb = new StringBuilder();
            for (String e : entries) {
                sb.append(e).append('\n');
            }
            logText.setText(sb);
            scroll.post(() -> scroll.fullScroll(ScrollView.FOCUS_DOWN));
        }
    }

    private void saveLog() {
        String dump = debugLog.dump();
        if (dump.isEmpty()) {
            Toast.makeText(this, R.string.debug_save_empty, Toast.LENGTH_SHORT).show();
            return;
        }

        String ts = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(new Date());
        String filename = "ahubby_debug_" + ts + ".txt";

        ContentValues cv = new ContentValues();
        cv.put(MediaStore.Downloads.DISPLAY_NAME, filename);
        cv.put(MediaStore.Downloads.MIME_TYPE, "text/plain");

        Uri uri = getContentResolver().insert(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv);

        if (uri == null) {
            Toast.makeText(this, R.string.debug_save_failed, Toast.LENGTH_SHORT).show();
            return;
        }

        try (OutputStream os = getContentResolver().openOutputStream(uri)) {
            if (os == null) throw new IOException("null stream");
            os.write(dump.getBytes(StandardCharsets.UTF_8));
            Toast.makeText(this,
                    getString(R.string.debug_save_ok, filename),
                    Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Toast.makeText(this, R.string.debug_save_failed, Toast.LENGTH_SHORT).show();
        }
    }
}
