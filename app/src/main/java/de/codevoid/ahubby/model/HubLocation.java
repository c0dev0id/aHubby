package de.codevoid.ahubby.model;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class HubLocation {
    public final String id;
    public final String title;
    public final String continent;
    public final String country;
    public final String mainCategory;
    public final List<String> categories;
    public final double latitude;
    public final double longitude;
    public boolean showOnMap;
    public final boolean isPublic;
    public final String shortDescription;
    public final String address;
    public final String thumbnailUrl;

    public HubLocation(String id, String title, String continent, String country,
                       String mainCategory, List<String> categories,
                       double latitude, double longitude,
                       boolean showOnMap, boolean isPublic,
                       String shortDescription, String address, String thumbnailUrl) {
        this.id = id;
        this.title = title;
        this.continent = continent;
        this.country = country;
        this.mainCategory = mainCategory;
        this.categories = categories;
        this.latitude = latitude;
        this.longitude = longitude;
        this.showOnMap = showOnMap;
        this.isPublic = isPublic;
        this.shortDescription = shortDescription;
        this.address = address;
        this.thumbnailUrl = thumbnailUrl;
    }

    public static List<HubLocation> parseList(String json) {
        List<HubLocation> result = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);

                List<String> cats = new ArrayList<>();
                JSONArray catArr = o.optJSONArray("category");
                if (catArr != null) {
                    for (int j = 0; j < catArr.length(); j++) {
                        cats.add(catArr.optString(j, ""));
                    }
                }

                result.add(new HubLocation(
                        o.optString("id", ""),
                        o.optString("title", ""),
                        o.optString("continent", ""),
                        o.optString("country", ""),
                        o.optString("main_category", ""),
                        cats,
                        o.optDouble("latitude", 0.0),
                        o.optDouble("longitude", 0.0),
                        o.optBoolean("show_on_map", false),
                        o.optBoolean("is_public", false),
                        o.optString("short_description", ""),
                        o.optString("address", ""),
                        o.optString("thumbnail_url", "")
                ));
            }
        } catch (Exception ignored) {
        }
        return result;
    }
}
