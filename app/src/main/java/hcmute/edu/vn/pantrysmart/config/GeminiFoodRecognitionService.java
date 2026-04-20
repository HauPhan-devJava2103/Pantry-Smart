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

import hcmute.edu.vn.pantrysmart.BuildConfig;
import hcmute.edu.vn.pantrysmart.data.local.entity.PantryItem;

public class GeminiFoodRecognitionService {
    private static final String TAG = "GeminiFoodAI";

    private static final String API_KEY = BuildConfig.GEMINI_API_KEY;

    private static final String BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=";

    public interface RecognitionCallback {
        void onSuccess(PantryItem item);

        void onError(String errorMessage);
    }

    public static void recognizeFood(String base64Image, RecognitionCallback callback) {
        new Thread(() -> {
            try {
                // Làm sạch chuỗi Base64
                String cleanBase64 = base64Image.replaceAll("\\s+", "").replaceAll("\\n", "");
                if (cleanBase64.contains(",")) {
                    cleanBase64 = cleanBase64.substring(cleanBase64.indexOf(",") + 1);
                }

                // Xây dựng JSON payload
                JSONObject requestBody = new JSONObject();
                JSONArray contents = new JSONArray();
                JSONObject contentObj = new JSONObject();
                JSONArray parts = new JSONArray();

                // Phần Text (yêu cầu trả về JSON)
                JSONObject textPart = new JSONObject();
                textPart.put("text",
                        "Nhận diện thực phẩm trong ảnh. Trả về định dạng JSON thuần túy (không có markdown): " +
                                "{\"name\": \"tên\", \"quantity\": 1.0, \"unit\": \"kg\", \"category\": \"OTHER\", \"emoji\": \"🍴\"}");
                parts.put(textPart);

                // Phần Hình Ảnh (camelCase chuẩn)
                JSONObject imagePart = new JSONObject();
                JSONObject inlineData = new JSONObject();
                inlineData.put("mimeType", "image/jpeg");
                inlineData.put("data", cleanBase64);
                imagePart.put("inlineData", inlineData);
                parts.put(imagePart);

                contentObj.put("parts", parts);
                contents.put(contentObj);
                requestBody.put("contents", contents);

                // Gửi Request
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
                    while ((line = br.readLine()) != null)
                        res.append(line);

                    JSONObject jsonResponse = new JSONObject(res.toString());
                    String text = jsonResponse.getJSONArray("candidates")
                            .getJSONObject(0).getJSONObject("content")
                            .getJSONArray("parts").getJSONObject(0).getString("text");

                    text = text.trim();
                    if (text.startsWith("```")) {
                        text = text.replaceAll("```json\\s*", "").replaceAll("```\\s*", "");
                    }

                    Log.d(TAG, "Kết quả từ AI: " + text);

                    JSONObject foodJson = new JSONObject(text);
                    PantryItem item = new PantryItem();
                    item.setName(foodJson.optString("name", "Thực phẩm không rõ"));
                    item.setQuantity(foodJson.optDouble("quantity", 1.0));
                    item.setUnit(foodJson.optString("unit", "cái"));
                    item.setCategory(foodJson.optString("category", "OTHER"));
                    item.setEmoji(foodJson.optString("emoji", "🍴"));

                    callback.onSuccess(item);
                } else {
                    java.io.InputStream errorStream = conn.getErrorStream();
                    if (errorStream != null) {
                        BufferedReader errorBr = new BufferedReader(new InputStreamReader(errorStream));
                        StringBuilder errorRes = new StringBuilder();
                        String errorLine;
                        while ((errorLine = errorBr.readLine()) != null) {
                            errorRes.append(errorLine);
                        }
                        Log.e(TAG, "LỖI TỪ GOOGLE: " + errorRes.toString());
                        Log.e(TAG, "JSON ĐÃ GỬI LÊN: " + requestBody.toString());
                        callback.onError("Lỗi AI: " + responseCode + ". " + errorRes.toString());
                    } else {
                        callback.onError("Lỗi AI: " + responseCode);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Exception: ", e);
                callback.onError("Lỗi hệ thống: " + e.getMessage());
            }
        }).start();
    }
}