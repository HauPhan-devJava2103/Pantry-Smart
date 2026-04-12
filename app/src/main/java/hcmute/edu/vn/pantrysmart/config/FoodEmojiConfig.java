package hcmute.edu.vn.pantrysmart.config;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * - Lấy emoji mặc định: FoodEmojiConfig.DEFAULT_EMOJI
 * - Lấy tất cả emoji phẳng: FoodEmojiConfig.getAllEmojis()
 * - Lấy emoji theo nhóm: FoodEmojiConfig.getGroupedEmojis()
 * - Lấy danh sách danh mục: FoodEmojiConfig.CATEGORIES
 * - Lấy emoji đại diện danh mục: FoodEmojiConfig.getCategoryEmoji("MEAT")
 * - Lấy tên hiển thị danh mục: FoodEmojiConfig.getCategoryLabel("MEAT")
 */
public final class FoodEmojiConfig {

    private FoodEmojiConfig() {
    }

    // EMOJI MẶC ĐỊNH — dùng khi item chưa được gán emoji
    public static final String DEFAULT_EMOJI = "📦";

    // DANH MỤC THỰC PHẨM (Food Categories)
    public static final String[][] CATEGORIES = {
            { "DAIRY", "🥛", "Sữa & Trứng" },
            { "VEGETABLE", "🥦", "Rau củ" },
            { "FRUIT", "🍎", "Trái cây" },
            { "MEAT", "🥩", "Thịt" },
            { "SEAFOOD", "🦐", "Hải sản" },
            { "DRINK", "🧃", "Đồ uống" },
            { "SPICE", "🧂", "Gia vị" },
            { "OTHER", "📦", "Khác" },
    };

    // EMOJI THỰC PHẨM — Phân theo nhóm

    public static final String[] DAIRY_EMOJIS = {
            "🧀", "🧈", "🥚", "🍦", "🍰", "🍮"
    };

    public static final String[] VEGETABLE_EMOJIS = {
            "🥬", "🥕", "🌽", "🧅", "🍅", "🥒", "🌶️",
            "🧄", "🥔", "🍆", "🫑", "🥗", "🍄", "🫘"
    };

    public static final String[] FRUIT_EMOJIS = {
            "🍊", "🍋", "🍇", "🍌", "🍉", "🥝", "🍓",
            "🍑", "🍍", "🥭", "🫐", "🍈", "🍐", "🥥"
    };

    public static final String[] MEAT_EMOJIS = {
            "🍗", "🥓", "🍖"
    };

    public static final String[] SEAFOOD_EMOJIS = {
            "🐟", "🦀", "🦑", "🐙", "🦞"
    };

    public static final String[] DRINK_EMOJIS = {
            "💧", "🥤", "☕", "🍵", "🧋", "🍺", "🍷"
    };

    public static final String[] SPICE_EMOJIS = {
            "🍯", "🫒", "🥜", "🌰"
    };

    public static final String[] OTHER_EMOJIS = {
            "🍞", "🍚", "🍜", "🍝", "🥐", "🥖", "🫙",
            "🍫", "🍿", "🧁"
    };

    // PHƯƠNG THỨC TIỆN ÍCH (Utility Methods)
    /**
     * Trả về Map LinkedHashMap: "🥛 Sữa & Trứng" → String[] emojis.
     * Giữ đúng thứ tự nhóm.
     */
    public static Map<String, String[]> getGroupedEmojis() {
        Map<String, String[]> map = new LinkedHashMap<>();
        map.put("🥛 Sữa & Trứng", DAIRY_EMOJIS);
        map.put("🥦 Rau củ", VEGETABLE_EMOJIS);
        map.put("🍎 Trái cây", FRUIT_EMOJIS);
        map.put("🥩 Thịt", MEAT_EMOJIS);
        map.put("🦐 Hải sản", SEAFOOD_EMOJIS);
        map.put("🧃 Đồ uống", DRINK_EMOJIS);
        map.put("🧂 Gia vị", SPICE_EMOJIS);
        map.put("📦 Khác", OTHER_EMOJIS);
        return map;
    }

    /**
     * Trả về mảng chứa tất cả emoji (không phân nhóm).
     */
    public static String[] getAllEmojis() {
        int total = DAIRY_EMOJIS.length + VEGETABLE_EMOJIS.length + FRUIT_EMOJIS.length
                + MEAT_EMOJIS.length + SEAFOOD_EMOJIS.length + DRINK_EMOJIS.length
                + SPICE_EMOJIS.length + OTHER_EMOJIS.length;
        String[] all = new String[total];
        int idx = 0;
        for (String[] group : new String[][] {
                DAIRY_EMOJIS, VEGETABLE_EMOJIS, FRUIT_EMOJIS, MEAT_EMOJIS,
                SEAFOOD_EMOJIS, DRINK_EMOJIS, SPICE_EMOJIS, OTHER_EMOJIS
        }) {
            for (String emoji : group) {
                all[idx++] = emoji;
            }
        }
        return all;
    }

    /**
     * Lấy mảng key[] dùng để lưu vào DB
     * Ví dụ: {"DAIRY", "VEGETABLE", "FRUIT", ...}
     */
    public static String[] getCategoryKeys() {
        String[] keys = new String[CATEGORIES.length];
        for (int i = 0; i < CATEGORIES.length; i++) {
            keys[i] = CATEGORIES[i][0];
        }
        return keys;
    }

    /**
     * Lấy mảng label[] hiển thị trên UI
     * Ví dụ: {"Sữa & Trứng", "Rau củ", ...}
     */
    public static String[] getCategoryLabels() {
        String[] labels = new String[CATEGORIES.length];
        for (int i = 0; i < CATEGORIES.length; i++) {
            labels[i] = CATEGORIES[i][1] + " " + CATEGORIES[i][2]; // "🥛 Sữa & Trứng"
        }
        return labels;
    }

    /**
     * Lấy emoji đại diện của một danh mục theo key.
     * Ví dụ: getCategoryEmoji("MEAT") → "🥩"
     */
    public static String getCategoryEmoji(String categoryKey) {
        if (categoryKey == null)
            return DEFAULT_EMOJI;
        for (String[] cat : CATEGORIES) {
            if (cat[0].equals(categoryKey)) {
                return cat[1];
            }
        }
        return DEFAULT_EMOJI;
    }

    /**
     * Lấy tên hiển thị của một danh mục theo key.
     * Ví dụ: getCategoryLabel("MEAT") → "Thịt"
     */
    public static String getCategoryLabel(String categoryKey) {
        if (categoryKey == null)
            return "Khác";
        for (String[] cat : CATEGORIES) {
            if (cat[0].equals(categoryKey)) {
                return cat[2];
            }
        }
        return "Khác";
    }

    public static String safeEmoji(String emoji) {
        return (emoji != null && !emoji.isEmpty()) ? emoji : DEFAULT_EMOJI;
    }
}
