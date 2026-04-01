package de.codevoid.ahubby.model;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class HubLocation {
    public final String id;
    public final String title;
    public final String country;
    public final String mainCategory;
    public final List<String> categories;
    public final String coordinates;
    public boolean showOnMap;
    public final boolean isPublic;
    public final String shortDescription;

    public HubLocation(String id, String title, String country, String mainCategory,
                       List<String> categories, String coordinates,
                       boolean showOnMap, boolean isPublic, String shortDescription) {
        this.id = id;
        this.title = title;
        this.country = country;
        this.mainCategory = mainCategory;
        this.categories = categories;
        this.coordinates = coordinates;
        this.showOnMap = showOnMap;
        this.isPublic = isPublic;
        this.shortDescription = shortDescription;
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
                        o.optString("_id", ""),
                        o.optString("title", ""),
                        o.optString("country", ""),
                        o.optString("main_category", ""),
                        cats,
                        o.optString("coordinates", ""),
                        o.optInt("show_on_map", 0) != 0,
                        o.optInt("public", 0) != 0,
                        o.optString("short_description", "")
                ));
            }
        } catch (Exception ignored) {
        }
        return result;
    }
}
