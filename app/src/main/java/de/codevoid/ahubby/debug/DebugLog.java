package de.codevoid.ahubby.debug;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DebugLog {

    private static final String PREFS = "ahubby_debug";
    private static final String KEY_ENABLED = "log_enabled";
    private static final int MAX_ENTRIES = 500;

    private static DebugLog instance;

    private final SharedPreferences prefs;
    private final List<String> entries = new ArrayList<>();
    private boolean enabled;

    private static final SimpleDateFormat TS_FMT =
            new SimpleDateFormat("HH:mm:ss.SSS", Locale.US);

    private DebugLog(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        enabled = prefs.getBoolean(KEY_ENABLED, false);
    }

    public static synchronized DebugLog getInstance(Context context) {
        if (instance == null) {
            instance = new DebugLog(context);
        }
        return instance;
    }

    public synchronized boolean isEnabled() {
        return enabled;
    }

    public synchronized void setEnabled(boolean value) {
        enabled = value;
        prefs.edit().putBoolean(KEY_ENABLED, value).apply();
    }

    public synchronized void log(String entry) {
        if (!enabled) return;
        String line = TS_FMT.format(new Date()) + "  " + entry;
        entries.add(line);
        if (entries.size() > MAX_ENTRIES) {
            entries.remove(0);
        }
    }

    public synchronized List<String> getEntries() {
        return new ArrayList<>(entries);
    }

    public synchronized void clear() {
        entries.clear();
    }

    public synchronized String dump() {
        StringBuilder sb = new StringBuilder();
        for (String e : entries) {
            sb.append(e).append('\n');
        }
        return sb.toString();
    }
}
