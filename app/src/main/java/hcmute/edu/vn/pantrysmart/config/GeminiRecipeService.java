package hcmute.edu.vn.pantrysmart.config;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import hcmute.edu.vn.pantrysmart.data.local.PantrySmartDatabase;
import hcmute.edu.vn.pantrysmart.data.local.entity.PantryItem;
import hcmute.edu.vn.pantrysmart.model.RecipeSuggestion;

/**
 * Service gọi Gemini REST API để gợi ý món ăn từ nguyên liệu có sẵn.
 * Sử dụng HttpURLConnection (không cần thêm SDK).
 */
public class GeminiRecipeService {

    private static final String TAG = "GeminiRecipeService";

    private static final String API_KEY = "AIzaSyBb1hVh_OM0q2PqtbPLO20A5P3djMLiI_8";
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=";

    public interface RecipeCallback {
        void onSuccess(List<RecipeSuggestion> recipes);

        void onError(String errorMessage);
    }

    /**
     * Gọi Gemini API để gợi ý món ăn. Chạy trên background thread.
     */
    public static void suggestRecipes(List<PantryItem> items, RecipeCallback callback) {
        PantrySmartDatabase.databaseWriteExecutor.execute(() -> {
            // Nếu chưa có API key → trả về demo data
            if (API_KEY.equals("YOUR_GEMINI_API_KEY") || API_KEY.isEmpty()) {
                Log.w(TAG, "API key chưa được cấu hình, sử dụng dữ liệu demo");
                callback.onSuccess(getDemoRecipes(items));
                return;
            }

            try {
                // 1. Build danh sách nguyên liệu
                StringBuilder ingredients = new StringBuilder();
                for (PantryItem item : items) {
                    ingredients.append("- ").append(item.getName())
                            .append(": ").append(item.getQuantity())
                            .append(" ").append(item.getUnit() != null ? item.getUnit() : "")
                            .append("\n");
                }

                // 2. Build prompt
                String prompt = "Bạn là đầu bếp Việt Nam chuyên nghiệp. Dựa vào nguyên liệu có sẵn, " +
                        "hãy gợi ý 5 món ăn ngon, đa dạng.\n\n" +
                        "Nguyên liệu có sẵn:\n" + ingredients + "\n" +
                        "Yêu cầu:\n" +
                        "- Ưu tiên món ăn Việt Nam\n" +
                        "- Mỗi món sử dụng ít nhất 2 nguyên liệu có sẵn\n" +
                        "- Đa dạng loại món (xào, canh, kho, chiên, hấp...)\n\n" +
                        "Trả về JSON array (KHÔNG markdown, KHÔNG giải thích thêm):\n" +
                        "[{\"dishName\":\"Tên món\",\"description\":\"Mô tả hương vị 1-2 câu\"," +
                        "\"cookingTime\":\"25 phút\",\"difficulty\":\"Dễ\"," +
                        "\"matchedIngredients\":[\"Nguyên liệu 1\",\"Nguyên liệu 2\"]," +
                        "\"imageSearch\":\"vietnamese dish name in english\"}]";

                // 3. Build request JSON
                JSONObject requestBody = new JSONObject();
                JSONArray contents = new JSONArray();
                JSONObject content = new JSONObject();
                JSONArray parts = new JSONArray();
                JSONObject part = new JSONObject();
                part.put("text", prompt);
                parts.put(part);
                content.put("parts", parts);
                contents.put(content);
                requestBody.put("contents", contents);

                // Force JSON response
                JSONObject genConfig = new JSONObject();
                genConfig.put("responseMimeType", "application/json");
                requestBody.put("generationConfig", genConfig);

                // 4. HTTP Request
                URL url = new URL(API_URL + API_KEY);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(30000);
                conn.setReadTimeout(30000);

                OutputStream os = conn.getOutputStream();
                os.write(requestBody.toString().getBytes("UTF-8"));
                os.close();

                int responseCode = conn.getResponseCode();
                if (responseCode != 200) {
                    BufferedReader errorReader = new BufferedReader(
                            new InputStreamReader(conn.getErrorStream()));
                    StringBuilder errorResponse = new StringBuilder();
                    String line;
                    while ((line = errorReader.readLine()) != null) {
                        errorResponse.append(line);
                    }
                    errorReader.close();
                    Log.e(TAG, "API Error " + responseCode + ": " + errorResponse);
                    // Fallback to demo
                    callback.onSuccess(getDemoRecipes(items));
                    return;
                }

                // 5. Read response
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                // 6. Parse response
                JSONObject jsonResponse = new JSONObject(response.toString());
                String text = jsonResponse.getJSONArray("candidates")
                        .getJSONObject(0)
                        .getJSONObject("content")
                        .getJSONArray("parts")
                        .getJSONObject(0)
                        .getString("text");

                // Clean markdown fences if present
                text = text.trim();
                if (text.startsWith("```")) {
                    text = text.replaceAll("```json\\s*", "").replaceAll("```\\s*", "");
                }

                JSONArray recipesArray = new JSONArray(text);
                List<RecipeSuggestion> recipes = new ArrayList<>();

                for (int i = 0; i < recipesArray.length(); i++) {
                    JSONObject obj = recipesArray.getJSONObject(i);
                    RecipeSuggestion recipe = new RecipeSuggestion();
                    recipe.setDishName(obj.optString("dishName", "Món ăn"));
                    recipe.setDescription(obj.optString("description", ""));
                    recipe.setCookingTime(obj.optString("cookingTime", "30 phút"));
                    recipe.setDifficulty(obj.optString("difficulty", "Trung bình"));
                    recipe.setImageSearch(obj.optString("imageSearch", ""));

                    JSONArray matched = obj.optJSONArray("matchedIngredients");
                    if (matched != null) {
                        List<String> matchedList = new ArrayList<>();
                        for (int j = 0; j < matched.length(); j++) {
                            matchedList.add(matched.getString(j));
                        }
                        recipe.setMatchedIngredients(matchedList);
                    }

                    recipes.add(recipe);
                }

                callback.onSuccess(recipes);

            } catch (Exception e) {
                Log.e(TAG, "Error calling Gemini API", e);
                callback.onSuccess(getDemoRecipes(items));
            }
        });
    }

    /**
     * Dữ liệu demo khi chưa có API key hoặc API lỗi.
     */
    public static List<RecipeSuggestion> getDemoRecipes(List<PantryItem> items) {
        List<RecipeSuggestion> recipes = new ArrayList<>();

        RecipeSuggestion r1 = new RecipeSuggestion();
        r1.setDishName("Trứng chiên thịt");
        r1.setDescription("Trứng chiên giòn với thịt heo băm, thơm ngon cho bữa cơm gia đình.");
        r1.setCookingTime("15 phút");
        r1.setDifficulty("Dễ");
        r1.setMatchedIngredients(Arrays.asList("Trứng gà", "Thịt heo"));
        r1.setImageSearch("vietnamese fried egg with pork");
        recipes.add(r1);

        RecipeSuggestion r2 = new RecipeSuggestion();
        r2.setDishName("Canh cà rốt thịt heo");
        r2.setDescription("Canh ngọt thanh với cà rốt mềm và thịt heo tươi, bổ dưỡng.");
        r2.setCookingTime("25 phút");
        r2.setDifficulty("Dễ");
        r2.setMatchedIngredients(Arrays.asList("Cà rốt", "Thịt heo"));
        r2.setImageSearch("vietnamese carrot pork soup");
        recipes.add(r2);

        RecipeSuggestion r3 = new RecipeSuggestion();
        r3.setDishName("Mì xào hải sản");
        r3.setDescription("Mì xào giòn với tôm tươi, rau cải xanh, đậm đà hương vị biển.");
        r3.setCookingTime("20 phút");
        r3.setDifficulty("Trung bình");
        r3.setMatchedIngredients(Arrays.asList("Mì tôm", "Tôm", "Rau cải"));
        r3.setImageSearch("vietnamese stir fried noodles seafood");
        recipes.add(r3);

        RecipeSuggestion r4 = new RecipeSuggestion();
        r4.setDishName("Phô mai trứng nướng");
        r4.setDescription("Trứng nướng phô mai béo ngậy, thơm lừng, món snack hấp dẫn.");
        r4.setCookingTime("20 phút");
        r4.setDifficulty("Dễ");
        r4.setMatchedIngredients(Arrays.asList("Trứng gà", "Phô mai"));
        r4.setImageSearch("baked cheese egg");
        recipes.add(r4);

        RecipeSuggestion r5 = new RecipeSuggestion();
        r5.setDishName("Tôm rang cà rốt");
        r5.setDescription("Tôm rang cháy cạnh với cà rốt giòn ngọt, đơn giản mà ngon miệng.");
        r5.setCookingTime("15 phút");
        r5.setDifficulty("Dễ");
        r5.setMatchedIngredients(Arrays.asList("Tôm", "Cà rốt"));
        r5.setImageSearch("vietnamese stir fried shrimp carrot");
        recipes.add(r5);

        return recipes;
    }
}
