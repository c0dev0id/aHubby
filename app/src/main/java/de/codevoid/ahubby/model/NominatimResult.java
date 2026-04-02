package de.codevoid.ahubby.model;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class NominatimResult {
    public final String displayName;
    public final String lat;
    public final String lon;
    public final String country;
    public final String countryCode;

    public NominatimResult(String displayName, String lat, String lon,
                           String country, String countryCode) {
        this.displayName = displayName;
        this.lat = lat;
        this.lon = lon;
        this.country = country;
        this.countryCode = countryCode;
    }

    /** Short label for list display — first two comma-separated parts of display_name. */
    public String shortLabel() {
        String[] parts = displayName.split(",", 3);
        if (parts.length >= 2) return parts[0].trim() + ", " + parts[1].trim();
        return displayName;
    }

    /** Coordinates in the DMD format: "lat, lon" with 6 decimal places. */
    public String coordinates() {
        try {
            return String.format("%.6f, %.6f",
                    Double.parseDouble(lat), Double.parseDouble(lon));
        } catch (NumberFormatException e) {
            return lat + ", " + lon;
        }
    }

    public static NominatimResult fromJson(JSONObject o) {
        JSONObject addr = o.optJSONObject("address");
        String country = addr != null ? addr.optString("country", "") : "";
        String cc = addr != null ? addr.optString("country_code", "") : "";
        return new NominatimResult(
                o.optString("display_name", ""),
                o.optString("lat", ""),
                o.optString("lon", ""),
                country,
                cc
        );
    }

    public static List<NominatimResult> parseList(String json) {
        List<NominatimResult> out = new ArrayList<>();
        try {
            org.json.JSONArray arr = new org.json.JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                out.add(fromJson(arr.getJSONObject(i)));
            }
        } catch (Exception ignored) {}
        return out;
    }
}
