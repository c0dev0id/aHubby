package de.codevoid.ahubby.api;

import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

import de.codevoid.ahubby.model.NominatimResult;

public class NominatimClient {

    private static final String BASE = "https://nominatim.openstreetmap.org";
    private static final String UA = "aHubby/1.0";

    public List<NominatimResult> search(String query) throws IOException {
        String url = BASE + "/search?format=json&addressdetails=1&limit=8&q="
                + java.net.URLEncoder.encode(query, StandardCharsets.UTF_8);
        String body = get(url);
        return NominatimResult.parseList(body);
    }

    /** Returns the result populated with country/countryCode from the reverse lookup. */
    public NominatimResult reverse(String lat, String lon) throws IOException {
        String url = BASE + "/reverse?format=json&addressdetails=1&zoom=5&lat=" + lat + "&lon=" + lon;
        String body = get(url);
        try {
            return NominatimResult.fromJson(new JSONObject(body));
        } catch (Exception e) {
            throw new IOException("Reverse geocode parse failed: " + e.getMessage());
        }
    }

    public static String continentForCode(String countryCode) {
        if (countryCode == null) return "Europe";
        switch (countryCode.toLowerCase()) {
            case "al": case "ad": case "at": case "by": case "be": case "ba": case "bg":
            case "hr": case "cy": case "cz": case "dk": case "ee": case "fi": case "fr":
            case "de": case "gr": case "hu": case "is": case "ie": case "it": case "xk":
            case "lv": case "li": case "lt": case "lu": case "mt": case "md": case "mc":
            case "me": case "nl": case "mk": case "no": case "pl": case "pt": case "ro":
            case "ru": case "sm": case "rs": case "sk": case "si": case "es": case "se":
            case "ch": case "tr": case "ua": case "gb": case "va":
                return "Europe";
            case "dz": case "ao": case "bj": case "bw": case "bf": case "bi": case "cm":
            case "cv": case "cf": case "td": case "km": case "cg": case "cd": case "ci":
            case "dj": case "eg": case "gq": case "er": case "et": case "ga": case "gm":
            case "gh": case "gn": case "gw": case "ke": case "ls": case "lr": case "ly":
            case "mg": case "mw": case "ml": case "mr": case "mu": case "ma": case "mz":
            case "na": case "ne": case "ng": case "rw": case "st": case "sn": case "sl":
            case "so": case "za": case "ss": case "sd": case "sz": case "tz": case "tg":
            case "tn": case "ug": case "zm": case "zw": case "eh": case "sc":
                return "Africa";
            case "af": case "am": case "az": case "bh": case "bd": case "bt": case "bn":
            case "kh": case "cn": case "ge": case "in": case "id": case "ir": case "iq":
            case "il": case "jp": case "jo": case "kz": case "kw": case "kg": case "la":
            case "lb": case "my": case "mv": case "mn": case "mm": case "np": case "kp":
            case "om": case "pk": case "ps": case "ph": case "qa": case "sa": case "sg":
            case "kr": case "lk": case "sy": case "tw": case "tj": case "th": case "tl":
            case "tm": case "ae": case "uz": case "vn": case "ye": case "hk": case "mo":
                return "Asia";
            case "ag": case "bs": case "bb": case "bz": case "ca": case "cr": case "cu":
            case "dm": case "do": case "sv": case "gd": case "gt": case "ht": case "hn":
            case "jm": case "mx": case "ni": case "pa": case "kn": case "lc": case "vc":
            case "tt": case "us": case "pr":
                return "North America";
            case "ar": case "bo": case "br": case "cl": case "co": case "ec": case "gy":
            case "py": case "pe": case "sr": case "uy": case "ve": case "gf":
                return "South America";
            case "au": case "fj": case "ki": case "mh": case "fm": case "nr": case "nz":
            case "pw": case "pg": case "ws": case "sb": case "to": case "tv": case "vu":
                return "Oceania";
            case "aq":
                return "Antarctica";
            default:
                return "Europe";
        }
    }

    private String get(String url) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        try {
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", UA);
            conn.setRequestProperty("Accept-Language", "en");
            conn.setConnectTimeout(15_000);
            conn.setReadTimeout(15_000);

            int status = conn.getResponseCode();
            InputStream is = status < 400 ? conn.getInputStream() : conn.getErrorStream();
            if (is == null) throw new IOException("HTTP " + status);
            char[] buf = new char[4096];
            StringBuilder sb = new StringBuilder();
            try (InputStreamReader r = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                int n;
                while ((n = r.read(buf)) != -1) sb.append(buf, 0, n);
            }
            if (status != 200) throw new IOException("Nominatim HTTP " + status + ": " + sb);
            return sb.toString();
        } finally {
            conn.disconnect();
        }
    }
}
