package hcmute.edu.vn.pantrysmart.config;

import android.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import hcmute.edu.vn.pantrysmart.BuildConfig;
import hcmute.edu.vn.pantrysmart.data.local.entity.PantryItem;

public class GeminiFoodRecognitionService {
    private static final String TAG = "GeminiFoodAI";
    private static final String API_KEY = BuildConfig.GEMINI_API_KEY;
    private static final String BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-lite:generateContent?key=";

    public interface RecognitionCallback {
        void onSuccess(List<PantryItem> items);
        void onError(String errorMessage);
    }

    public static void recognizeFood(String base64Image, RecognitionCallback callback) {
        new Thread(() -> {
            try {
                String cleanBase64 = base64Image.replaceAll("\\s+", "").replaceAll("\\n", "");
                if (cleanBase64.contains(",")) {
                    cleanBase64 = cleanBase64.substring(cleanBase64.indexOf(",") + 1);
                }

                JSONObject requestBody = new JSONObject();
                JSONArray contents = new JSONArray();
                JSONObject contentObj = new JSONObject();
                JSONArray parts = new JSONArray();
                JSONObject textPart = new JSONObject();
                textPart.put("text",
                        "Hãy nhận diện TẤT CẢ các loại thực phẩm, đồ uống có trong ảnh này. " +
                                "Phân tích tên, số lượng ước tính, đơn vị và phân loại chúng. " +
                                "Danh mục (category) CHỈ ĐƯỢC chọn một trong: DAIRY, VEGETABLE, FRUIT, MEAT, SEAFOOD, DRINK, SPICE, OTHER. " +
                                "Trường 'emoji' hãy trả về tên icon phù hợp (ví dụ: ic_food_broccoli, ic_food_steak, ic_food_apple, ic_food_milk, ic_food_package). " +
                                "Trả về DUY NHẤT một JSON array thuần túy, không bao gồm markdown hay văn bản giải thích: " +
                                "[{\"name\": \"Tên thực phẩm\", \"quantity\": 1.0, \"unit\": \"kg\", \"category\": \"VEGETABLE\", \"emoji\": \"ic_food_broccoli\"}]");
                parts.put(textPart);

                JSONObject imagePart = new JSONObject();
                JSONObject inlineData = new JSONObject();
                inlineData.put("mimeType", "image/jpeg");
                inlineData.put("data", cleanBase64);
                imagePart.put("inlineData", inlineData);
                parts.put(imagePart);

                contentObj.put("parts", parts);
                contents.put(contentObj);
                requestBody.put("contents", contents);

                URL url = new URL(BASE_URL + API_KEY.trim());
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(requestBody.toString().getBytes(StandardCharsets.UTF_8));
                }

                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder res = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) res.append(line);

                    JSONObject jsonResponse = new JSONObject(res.toString());
                    String text = jsonResponse.getJSONArray("candidates")
                            .getJSONObject(0).getJSONObject("content")
                            .getJSONArray("parts").getJSONObject(0).getString("text");

                    text = text.trim();
                    if (text.startsWith("```")) {
                        text = text.replaceAll("```json\\s*", "").replaceAll("```\\s*", "");
                    }

                    // Parse kết quả từ String sang danh sách đối tượng
                    JSONArray jsonArray = new JSONArray(text);
                    List<PantryItem> items = new ArrayList<>();
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject obj = jsonArray.getJSONObject(i);
                        PantryItem item = new PantryItem();
                        item.setName(obj.optString("name", "Thực phẩm không rõ"));
                        item.setQuantity(obj.optDouble("quantity", 1.0));
                        item.setUnit(obj.optString("unit", "cái"));
                        item.setCategory(obj.optString("category", "OTHER"));
                        item.setEmoji(obj.optString("emoji", "ic_food_package"));
                        items.add(item);
                    }
                    callback.onSuccess(items);
                } else {
                    callback.onError("Lỗi AI: " + responseCode);
                }
            } catch (Exception e) {
                Log.e(TAG, "Exception: ", e);
                callback.onError("Lỗi hệ thống: " + e.getMessage());
            }
        }).start();
    }
}