package hcmute.edu.vn.pantrysmart.config;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import hcmute.edu.vn.pantrysmart.BuildConfig;

public class PexelsImageService {

    private static final String TAG = "PexelsImageService";
    private static final String API_URL = "https://api.pexels.com/v1/search";
    private static final String API_KEY = BuildConfig.PEXELS_API_KEY;

    public static String searchFoodImage(String imageSearch) {

        if (API_KEY == null || API_KEY.isEmpty())
            return null;

        // QUERY ƯU TIÊN
        String[] queries = new String[] {
                imageSearch + " food close up",
                imageSearch + " food",
                imageSearch + " dish",
                "vietnamese " + imageSearch,
                "vietnamese food"
        };

        for (String q : queries) {
            String result = doSearch(q);
            if (result != null)
                return result;
        }

        return null;
    }

    private static String doSearch(String query) {
        try {
            String urlStr = API_URL +
                    "?query=" + URLEncoder.encode(query, "UTF-8") +
                    "&per_page=1" +
                    "&orientation=square" +
                    "&category=food";

            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", API_KEY);

            if (conn.getResponseCode() == 200) {

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream()));

                StringBuilder res = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    res.append(line);
                }
                reader.close();

                JSONObject json = new JSONObject(res.toString());
                JSONArray photos = json.getJSONArray("photos");

                if (photos.length() > 0) {
                    return photos.getJSONObject(0)
                            .getJSONObject("src")
                            .getString("medium");
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "Search error: " + query, e);
        }

        return null;
    }
}