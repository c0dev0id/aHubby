package de.codevoid.ahubby.model;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class GpxFile {
    public final String id;
    public final String title;
    public final String country;
    public final String continent;
    public boolean showOnMap;
    public final boolean isPublic;
    public final String description;

    public GpxFile(String id, String title, String country, String continent,
                   boolean showOnMap, boolean isPublic, String description) {
        this.id = id;
        this.title = title;
        this.country = country;
        this.continent = continent;
        this.showOnMap = showOnMap;
        this.isPublic = isPublic;
        this.description = description;
    }

    public static List<GpxFile> parseList(String json) {
        List<GpxFile> result = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                result.add(new GpxFile(
                        o.optString("_id", ""),
                        o.optString("title", ""),
                        o.optString("country", ""),
                        o.optString("continent", ""),
                        o.optBoolean("show_on_map", false),
                        o.optBoolean("public", false),
                        o.optString("description", "")
                ));
            }
        } catch (Exception ignored) {
        }
        return result;
    }
}
