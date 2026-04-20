package hcmute.edu.vn.pantrysmart.config;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.LruCache;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import hcmute.edu.vn.pantrysmart.BuildConfig;
import hcmute.edu.vn.pantrysmart.model.ScannedItem;

public class GeminiReceiptParser {
    private static final String TAG = "GeminiReceiptParser";
    private static final String API_KEY = BuildConfig.GEMINI_API_KEY;
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=";

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());
    private static final LruCache<String, List<ScannedItem>> cache = new LruCache<>(5);

    public interface ParseCallback {
        void onSuccess(List<ScannedItem> items);

        void onError(String errorMessage);
    }

    public static void parseReceiptText(String rawText, ParseCallback callback) {
        executor.execute(() -> {
            try {
                List<ScannedItem> cachedItems = cache.get(rawText);
                if (cachedItems != null) {
                    mainHandler.post(() -> callback.onSuccess(new ArrayList<>(cachedItems)));
                    return;
                }

                String promptText = "Bạn là một trợ lý chuyên trích xuất dữ liệu từ hóa đơn siêu thị tại Việt Nam.\n" +
                        "Tôi sẽ cung cấp cho bạn một đoạn chữ thô lấy từ hóa đơn. Nhiệm vụ của bạn là lọc và CHỈ trích xuất CÁC MẶT HÀNG THỰC PHẨM TƯƠI SỐNG, RAU CỦ, TRÁI CÂY VÀ THỊT CÁ (ví dụ: rau, thịt, cá, tôm, lẩu, trứng...).\n"
                        +
                        "1. CHỈ trích xuất tên sản phẩm thực tế thuộc nhóm thực phẩm tươi sống nói trên.\n" +
                        "2. BỎ QUA HOÀN TOÀN hóa mỹ phẩm, dụng cụ, đồ gia dụng, và đặc biệt BỎ QUA các loại gia vị (nước mắm, bột ngọt, muối, dầu ăn, hạt nêm). BỎ QUA các từ khóa tổng kết (Phải thanh toán, Tiền mặt, Điểm, Ngày giờ, Chiết khấu, VAT, mã vạch...).\n"
                        +
                        "3. Trả kết quả về ĐÚNG định dạng JSON Array. Mỗi object gồm: 'name' (tên), 'quantity' (số lượng), 'unit' (đơn vị), 'price' (tổng giá: Integer), 'category' (thịt, rau, trái cây, sữa, trứng, đồ uống, khô, gia dụng, khác), 'expiry_days' (ước tính số ngày bảo quản tối đa: Integer).\n"
                        +
                        "Chỉ trả mảng JSON hợp lệ, không dính ```json cục bộ, không giải thích thêm.\n\n--- VĂN BẢN HÓA ĐƠN ---\n"
                        + rawText;

                JSONObject textPart = new JSONObject();
                textPart.put("text", promptText);

                JSONArray parts = new JSONArray();
                parts.put(textPart);

                JSONObject content = new JSONObject();
                content.put("parts", parts);

                JSONArray contents = new JSONArray();
                contents.put(content);

                JSONObject generationConfig = new JSONObject();
                generationConfig.put("temperature", 0.4);

                JSONObject requestBody = new JSONObject();
                requestBody.put("contents", contents);
                requestBody.put("generationConfig", generationConfig);

                executeRequestWithRetry(requestBody, 0, rawText, callback);

            } catch (Exception e) {
                Log.e(TAG, "Gemini exception in parseReceipt setup", e);
                mainHandler.post(() -> callback.onError("Lỗi hệ thống: " + e.getMessage()));
            }
        });
    }

    public static void parseReceipt(Bitmap bitmap, ParseCallback callback) {
        executor.execute(() -> {
            try {
                String base64Image = bitmapToBase64(bitmap);
                List<ScannedItem> cachedItems = cache.get(base64Image);
                if (cachedItems != null) {
                    mainHandler.post(() -> callback.onSuccess(new ArrayList<>(cachedItems)));
                    return;
                }

                String promptText = "Bạn là một trợ lý chuyên trích xuất dữ liệu từ hóa đơn siêu thị tại Việt Nam.\n" +
                        "Tôi sẽ cung cấp cho bạn ảnh hóa đơn siêu thị. Nhiệm vụ của bạn là lọc và CHỈ trích xuất CÁC MẶT HÀNG THỰC PHẨM TƯƠI SỐNG, RAU CỦ, TRÁI CÂY VÀ THỊT CÁ (ví dụ: rau, thịt, cá, tôm, lẩu, trứng...).\n"
                        +
                        "1. CHỈ trích xuất tên sản phẩm thực tế thuộc nhóm thực phẩm tươi sống nói trên.\n" +
                        "2. BỎ QUA HOÀN TOÀN hóa mỹ phẩm, dụng cụ, đồ gia dụng, và đặc biệt BỎ QUA các loại gia vị (nước mắm, bột ngọt, muối, dầu ăn, hạt nêm). BỎ QUA các từ khóa tổng kết (Phải thanh toán, Tiền mặt, Điểm BHX, Ngày giờ, Chiết khấu, VAT, mã vạch, tên siêu thị...).\n"
                        +
                        "3. Trả kết quả về ĐÚNG định dạng JSON Array. Mỗi object gồm: 'name' (tên), 'quantity' (số lượng), 'price' (tổng giá: Integer), 'category' (thịt, rau, trái cây, sữa, trứng, đồ uống, khô, gia dụng, khác), 'expiry_days' (ước tính số ngày bảo quản tối đa: Integer).\n"
                        +
                        "Chỉ trả mảng JSON hợp lệ, không dính ```json cục bộ, không giải thích thêm.";

                JSONObject imagePart = new JSONObject();
                JSONObject inlineData = new JSONObject();
                inlineData.put("mimeType", "image/jpeg");
                inlineData.put("data", base64Image);
                imagePart.put("inlineData", inlineData);

                JSONObject textPart = new JSONObject();
                textPart.put("text", promptText);

                JSONArray parts = new JSONArray();
                parts.put(textPart);
                parts.put(imagePart);

                JSONObject content = new JSONObject();
                content.put("parts", parts);

                JSONArray contents = new JSONArray();
                contents.put(content);

                JSONObject generationConfig = new JSONObject();
                generationConfig.put("temperature", 0.4);

                JSONObject requestBody = new JSONObject();
                requestBody.put("contents", contents);
                requestBody.put("generationConfig", generationConfig);

                executeRequestWithRetry(requestBody, 0, base64Image, callback);

            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("Lỗi hệ thống: " + e.getMessage()));
            }
        });
    }

    private static void executeRequestWithRetry(JSONObject requestBody, int attempt, String imageCacheKey,
            ParseCallback callback) {
        final int MAX_RETRIES = 3;
        final long INITIAL_BACKOFF_MS = 2000;

        executor.execute(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(API_URL + API_KEY);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setDoOutput(true);
                conn.setConnectTimeout(100000);
                conn.setReadTimeout(100000);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(requestBody.toString().getBytes(StandardCharsets.UTF_8));
                }

                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    StringBuilder responseString = new StringBuilder();
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                        String line;
                        while ((line = reader.readLine()) != null)
                            responseString.append(line);
                    }

                    JSONObject jsonResponse = new JSONObject(responseString.toString());
                    String extractedJsonText = jsonResponse.getJSONArray("candidates").getJSONObject(0)
                            .getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text");

                    extractedJsonText = extractedJsonText.trim();
                    if (extractedJsonText.startsWith("```")) {
                        extractedJsonText = extractedJsonText.replaceAll("```(?:json)?\\s*", "")
                                .replaceAll("```\\s*$", "").trim();
                    }

                    List<ScannedItem> items = parseItemsFromJson(extractedJsonText);
                    if (items.isEmpty()) {
                        mainHandler.post(() -> callback.onError("Không nhận diện được món hàng nào. Hãy chụp rõ hơn."));
                    } else {
                        cache.put(imageCacheKey, new ArrayList<>(items));
                        mainHandler.post(() -> callback.onSuccess(items));
                    }
                } else if (responseCode == 429 && attempt < MAX_RETRIES) {
                    long backoffDelay = INITIAL_BACKOFF_MS * (long) Math.pow(2, attempt);
                    Thread.sleep(backoffDelay);
                    executeRequestWithRetry(requestBody, attempt + 1, imageCacheKey, callback);
                } else {
                    mainHandler.post(() -> callback.onError("Lỗi API Gemini (Code: " + responseCode + ")"));
                }
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("Lỗi máy chủ / Network: " + e.getMessage()));
            } finally {
                if (conn != null)
                    conn.disconnect();
            }
        });
    }

    private static List<ScannedItem> parseItemsFromJson(String jsonText) throws org.json.JSONException {
        List<ScannedItem> items = new ArrayList<>();
        JSONArray array = new JSONArray(jsonText);
        for (int i = 0; i < array.length(); i++) {
            JSONObject obj = array.getJSONObject(i);
            String name = obj.optString("name", "").trim();
            if (name.isEmpty())
                continue;

            double quantity = obj.optDouble("quantity", 1);
            long price = obj.optLong("price", 0);
            String unit = obj.optString("unit", "").trim();
            if (unit.isEmpty())
                unit = guessUnit(name);

            String category = obj.optString("category", "").toLowerCase(Locale.ROOT).trim();
            if (category.isEmpty())
                category = guessCategory(name);

            String emoji = guessEmoji(name, category);
            int expiryDays = obj.optInt("expiry_days", 0);
            if (expiryDays <= 0)
                expiryDays = guessExpiryDays(category);

            items.add(new ScannedItem(name, quantity, unit, price, category, emoji, expiryDays));
        }
        return items;
    }

    private static String bitmapToBase64(Bitmap bitmap) {
        int maxDim = 600;
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        if (width > maxDim || height > maxDim) {
            float ratio = Math.min((float) maxDim / width, (float) maxDim / height);
            bitmap = Bitmap.createScaledBitmap(bitmap, Math.round(ratio * width), Math.round(ratio * height), true);
        }
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 20, outputStream);
        return android.util.Base64.encodeToString(outputStream.toByteArray(), android.util.Base64.NO_WRAP);
    }

    private static String guessUnit(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        if (lower.contains("sữa") || lower.contains("nước") || lower.contains("bia"))
            return "chai";
        if (lower.contains("thịt") || lower.contains("cá") || lower.contains("khay"))
            return "khay";
        return "gói";
    }

    private static String guessCategory(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        if (lower.matches(".*(thịt|thit|bò|bo|heo|gà|ga|vịt|vit|cá|ca|tôm|tom|mực|muc|hải sản|hai san).*"))
            return "thịt";
        if (lower.matches(".*(rau|cải|cai|bắp cải|bap cai|xà lách).*"))
            return "rau";
        if (lower.matches(".*(trái cây|trai cay|cam|táo|tao|xoài|xoai|chuối|chuoi).*"))
            return "trái cây";
        if (lower.matches(".*(sữa|phô mai|cream|bơ|yogurt).*"))
            return "sữa";
        return "khác";
    }

    private static String guessEmoji(String name, String category) {
        String lower = name.toLowerCase(Locale.ROOT);
        if (lower.contains("trứng"))
            return "ic_food_egg";
        if (lower.contains("thịt") || "thịt".equals(category))
            return "ic_food_steak";
        if (lower.contains("rau") || "rau".equals(category))
            return "ic_food_lettuce";
        return "ic_food_package";
    }

    private static int guessExpiryDays(String category) {
        switch (category) {
            case "thịt":
                return 90;
            case "rau":
                return 7;
            case "trái cây":
                return 10;
            case "sữa":
                return 7;
            case "trứng":
                return 21;
            case "đồ uống":
                return 30;
            case "khô":
                return 180;
            case "gia dụng":
                return 365;
            default:
                return 7;
        }
    }
}