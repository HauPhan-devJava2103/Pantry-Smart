package hcmute.edu.vn.pantrysmart.fragment.helper;

/**
 * Helper phân loại và gắn tag cho công thức nấu ăn.
 * Bao gồm: phân loại bữa ăn, xác định chay/mặn, parse thời gian.
 */
public class RecipeTagHelper {

    // Phân loại bữa ăn dựa vào tên món.
    public static String getMealType(String dishName) {
        if (dishName == null)
            return "Bữa chính";
        String lower = dishName.toLowerCase();
        if (lower.contains("cháo") || lower.contains("bánh mì") ||
                lower.contains("sinh tố") || lower.contains("trứng chiên") ||
                lower.contains("xôi")) {
            return "Bữa sáng";
        }
        if (lower.contains("chè") || lower.contains("kem") ||
                lower.contains("bánh") || lower.contains("snack")) {
            return "Ăn vặt";
        }
        return "Bữa chính";
    }

    // Parse số phút từ chuỗi như "10 phút", "30 phút".
    public static int parseMinutes(String timeStr) {
        if (timeStr == null)
            return 0;
        try {
            return Integer.parseInt(timeStr.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    // Tag chay/mặn dựa vào nguyên liệu.
    public static String getDietTag(String[] ingredients) {
        if (ingredients == null)
            return "Chay";
        String[] meatKeywords = { "thịt", "bò", "gà", "heo", "tôm", "cá",
                "mực", "sườn", "lợn", "vịt", "cua" };
        for (String ingredient : ingredients) {
            if (ingredient == null)
                continue;
            String lower = ingredient.toLowerCase();
            for (String meat : meatKeywords) {
                if (lower.contains(meat))
                    return "Mặn";
            }
        }
        return "Chay";
    }
}
