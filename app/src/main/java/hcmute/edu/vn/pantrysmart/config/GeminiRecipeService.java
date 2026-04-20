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

import hcmute.edu.vn.pantrysmart.BuildConfig;
import hcmute.edu.vn.pantrysmart.data.local.PantrySmartDatabase;
import hcmute.edu.vn.pantrysmart.data.local.entity.PantryItem;
import hcmute.edu.vn.pantrysmart.model.RecipeSuggestion;

public class GeminiRecipeService {

    private static final String TAG = "GeminiRecipeService";

    private static final String API_KEY = BuildConfig.GEMINI_API_KEY;
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=";

    public interface RecipeCallback {
        void onSuccess(List<RecipeSuggestion> recipes);

        void onError(String errorMessage);
    }

    public static void suggestRecipes(List<PantryItem> items, RecipeCallback callback) {
        PantrySmartDatabase.databaseWriteExecutor.execute(() -> {

            if (API_KEY == null || API_KEY.isEmpty()) {
                Log.w(TAG, "No API key: demo");
                callback.onSuccess(getDemoRecipes(items));
                return;
            }

            try {
                // 1. Lấy danh sách nguyên liệu
                StringBuilder ingredients = new StringBuilder();
                for (PantryItem item : items) {
                    ingredients.append("- ").append(item.getName())
                            .append(": ").append(item.getQuantity())
                            .append(" ").append(item.getUnit() != null ? item.getUnit() : "")
                            .append("\n");
                }

                // 2. PROMPT
                String prompt = "Bạn là đầu bếp Việt Nam chuyên nghiệp.\n\n" +
                        "Nguyên liệu có sẵn:\n" + ingredients + "\n\n" +

                        "Hãy gợi ý 5 món ăn CHO 1 NGƯỜI ĂN.\n" +
                        "Yêu cầu:\n" +
                        "- Ưu tiên món Việt\n" +
                        "- Mỗi món dùng ít nhất 2 nguyên liệu\n" +
                        "- Đa dạng cách nấu\n" +
                        "- matchedIngredients phải ghi RÕ SỐ LƯỢNG cho 1 người ăn\n" +
                        "- Định dạng: \"Tên nguyên liệu - số lượng đơn vị\"\n" +
                        "- Đồ đếm được (quả, con, lát, miếng, nhánh, tép, cây) phải dùng SỐ NGUYÊN (1, 2, 3...)\n" +
                        "- Chỉ dùng số lẻ cho đơn vị cân/đo (g, ml, thìa, muỗng)\n" +
                        "- Ví dụ: [\"Trứng gà - 2 quả\", \"Thịt heo xay - 100g\", \"Hành lá - 1 nhánh\"]\n\n" +

                        "QUAN TRỌNG VỀ STEPS:\n" +
                        "- Mỗi bước nấu PHẢI ghi RÕ SỐ LƯỢNG nguyên liệu sử dụng\n" +
                        "- ĐÚNG: \"Cho 200g cà chua thái nhỏ vào chảo xào 2 phút\"\n" +
                        "- ĐÚNG: \"Đập 2 quả trứng vào bát, thêm 1 thìa nước mắm, đánh đều\"\n" +
                        "- SAI: \"Cho cà chua vào xào\" (thiếu số lượng)\n" +
                        "- SAI: \"Đập trứng vào bát\" (thiếu số lượng)\n\n" +

                        "QUAN TRỌNG VỀ IMAGE:\n" +
                        "- imageSearch phải là tiếng Anh\n" +
                        "- Ngắn gọn 2-5 từ\n" +
                        "- Mô tả CHÍNH XÁC món ăn\n" +
                        "- Không dùng từ chung chung như 'delicious food'\n\n" +

                        "Ví dụ imageSearch tốt:\n" +
                        "- 'stir fried beef onion'\n" +
                        "- 'vietnamese caramelized pork'\n" +
                        "- 'shrimp fried rice'\n\n" +

                        "Trả về JSON array:\n" +
                        "[{" +
                        "\"dishName\":\"Tên món\"," +
                        "\"description\":\"Mô tả\"," +
                        "\"cookingTime\":\"25 phút\"," +
                        "\"difficulty\":\"Dễ\"," +
                        "\"matchedIngredients\":[\"Trứng gà - 2 quả\",\"Thịt heo - 150g\"]," +
                        "\"steps\":[\"Đập 2 quả trứng vào bát, thêm 1 thìa nước mắm\",\"Cho 150g thịt heo vào chảo xào 3 phút\"]," +
                        "\"imageSearch\":\"english keyword\"" +
                        "}]";

                // 3. Request JSON
                JSONObject body = new JSONObject();
                JSONArray contents = new JSONArray();
                JSONObject content = new JSONObject();
                JSONArray parts = new JSONArray();

                JSONObject part = new JSONObject();
                part.put("text", prompt);

                parts.put(part);
                content.put("parts", parts);
                contents.put(content);
                body.put("contents", contents);

                JSONObject config = new JSONObject();
                config.put("responseMimeType", "application/json");
                body.put("generationConfig", config);

                // 4. CALL API
                URL url = new URL(API_URL + API_KEY);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                OutputStream os = conn.getOutputStream();
                os.write(body.toString().getBytes("UTF-8"));
                os.close();

                if (conn.getResponseCode() != 200) {
                    callback.onSuccess(getDemoRecipes(items));
                    return;
                }

                // 5. READ
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream()));
                StringBuilder res = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    res.append(line);
                }
                reader.close();

                // 6. PARSE
                JSONObject json = new JSONObject(res.toString());

                String text = json.getJSONArray("candidates")
                        .getJSONObject(0)
                        .getJSONObject("content")
                        .getJSONArray("parts")
                        .getJSONObject(0)
                        .getString("text");

                text = text.trim().replaceAll("```json|```", "");

                JSONArray arr = new JSONArray(text);
                List<RecipeSuggestion> list = new ArrayList<>();

                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.getJSONObject(i);

                    RecipeSuggestion r = new RecipeSuggestion();
                    r.setDishName(o.optString("dishName"));
                    r.setDescription(o.optString("description"));
                    r.setCookingTime(o.optString("cookingTime"));
                    r.setDifficulty(o.optString("difficulty"));
                    r.setImageSearch(o.optString("imageSearch"));

                    // Parse matchedIngredients
                    JSONArray ingArr = o.optJSONArray("matchedIngredients");
                    if (ingArr != null) {
                        List<String> ings = new ArrayList<>();
                        for (int j = 0; j < ingArr.length(); j++) {
                            ings.add(ingArr.getString(j));
                        }
                        r.setMatchedIngredients(ings);
                    }

                    // Parse steps
                    JSONArray stepsArr = o.optJSONArray("steps");
                    if (stepsArr != null) {
                        List<String> stepsList = new ArrayList<>();
                        for (int j = 0; j < stepsArr.length(); j++) {
                            stepsList.add(stepsArr.getString(j));
                        }
                        r.setSteps(stepsList);
                    }

                    list.add(r);
                }

                callback.onSuccess(list);

            } catch (Exception e) {
                Log.e(TAG, "Error", e);
                callback.onSuccess(getDemoRecipes(items));
            }
        });
    }

    public static List<RecipeSuggestion> getDemoRecipes(List<PantryItem> items) {
        List<RecipeSuggestion> list = new ArrayList<>();

        RecipeSuggestion r = new RecipeSuggestion();
        r.setDishName("Trứng chiên thịt");
        r.setDescription("Món ăn nhanh, bổ dưỡng cho bữa sáng.");
        r.setCookingTime("10 phút");
        r.setDifficulty("Dễ");
        r.setImageSearch("fried egg pork");

        List<String> ings = new ArrayList<>();
        ings.add("Trứng gà - 2 quả");
        ings.add("Thịt heo xay - 100g");
        r.setMatchedIngredients(ings);

        List<String> steps = new ArrayList<>();
        steps.add("Đập 2 quả trứng vào bát, thêm 1/2 thìa muối, đánh tan đều.");
        steps.add("Phi 2 tép tỏi thơm, cho 100g thịt heo xay vào xào 2 phút cho chín.");
        steps.add("Đổ trứng vào chảo, chiên đều hai mặt khoảng 3 phút.");
        steps.add("Trang trí với hành lá thái nhỏ và thưởng thức.");
        r.setSteps(steps);

        list.add(r);

        return list;
    }
}