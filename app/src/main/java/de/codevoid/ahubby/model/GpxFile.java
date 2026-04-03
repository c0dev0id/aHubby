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
    public final double distanceKm;
    public final int tracksCount;
    public final int waypointsCount;
    public final String color;
    public final boolean allowDownload;

    public GpxFile(String id, String title, String country, String continent,
                   boolean showOnMap, boolean isPublic, String description,
                   double distanceKm, int tracksCount, int waypointsCount,
                   String color, boolean allowDownload) {
        this.id = id;
        this.title = title;
        this.country = country;
        this.continent = continent;
        this.showOnMap = showOnMap;
        this.isPublic = isPublic;
        this.description = description;
        this.distanceKm = distanceKm;
        this.tracksCount = tracksCount;
        this.waypointsCount = waypointsCount;
        this.color = color;
        this.allowDownload = allowDownload;
    }

    public static List<GpxFile> parseList(String json) {
        List<GpxFile> result = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                result.add(new GpxFile(
                        o.optString("id", ""),
                        o.optString("title", ""),
                        o.optString("country", ""),
                        o.optString("continent", ""),
                        o.optBoolean("show_on_map", false),
                        o.optBoolean("is_public", false),
                        o.optString("description", ""),
                        o.optDouble("distance_km", 0.0),
                        o.optInt("tracks_count", 0),
                        o.optInt("waypoints_count", 0),
                        o.optString("color", ""),
                        o.optBoolean("allow_download", false)
                ));
            }
        } catch (Exception ignored) {
        }
        return result;
    }
}
