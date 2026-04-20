package hcmute.edu.vn.pantrysmart.config;

import java.util.LinkedHashMap;
import java.util.Map;

import hcmute.edu.vn.pantrysmart.R;

/**
 * FoodIconConfig — Map drawable name (String) <-> drawable resource ID (int).
 * <p>
 * DB luu drawable name (VD: "ic_food_steak"), UI dung resource ID
 * (R.drawable.ic_food_steak).
 * <p>
 * API:
 * - FoodIconConfig.getIconRes("ic_food_steak") -> R.drawable.ic_food_steak
 * - FoodIconConfig.safeIcon("ic_food_steak") -> never returns 0
 * - FoodIconConfig.getGroupedIcons() -> grouped for picker dialog
 * - FoodIconConfig.getCategoryIcon("MEAT") -> R.drawable.ic_food_steak
 */
public final class FoodIconConfig {

    private FoodIconConfig() {
    }

    // === DEFAULT ===
    public static final String DEFAULT_ICON_NAME = "ic_food_package";
    public static final int DEFAULT_ICON = R.drawable.ic_food_package;

    // === DRAWABLE NAME -> RESOURCE ID ===
    private static final Map<String, Integer> ICON_MAP = new LinkedHashMap<>();

    static {
        // Dairy
        ICON_MAP.put("ic_food_milk", R.drawable.ic_food_milk);
        ICON_MAP.put("ic_food_cheese", R.drawable.ic_food_cheese);
        ICON_MAP.put("ic_food_butter", R.drawable.ic_food_butter);
        ICON_MAP.put("ic_food_egg", R.drawable.ic_food_egg);
        ICON_MAP.put("ic_food_icecream", R.drawable.ic_food_icecream);
        ICON_MAP.put("ic_food_cake", R.drawable.ic_food_cake);
        ICON_MAP.put("ic_food_pudding", R.drawable.ic_food_pudding);

        // Vegetable
        ICON_MAP.put("ic_food_broccoli", R.drawable.ic_food_broccoli);
        ICON_MAP.put("ic_food_lettuce", R.drawable.ic_food_lettuce);
        ICON_MAP.put("ic_food_carrot", R.drawable.ic_food_carrot);
        ICON_MAP.put("ic_food_corn", R.drawable.ic_food_corn);
        ICON_MAP.put("ic_food_onion", R.drawable.ic_food_onion);
        ICON_MAP.put("ic_food_tomato", R.drawable.ic_food_tomato);
        ICON_MAP.put("ic_food_cucumber", R.drawable.ic_food_cucumber);
        ICON_MAP.put("ic_food_chili", R.drawable.ic_food_chili);
        ICON_MAP.put("ic_food_garlic", R.drawable.ic_food_garlic);
        ICON_MAP.put("ic_food_potato", R.drawable.ic_food_potato);
        ICON_MAP.put("ic_food_eggplant", R.drawable.ic_food_eggplant);
        ICON_MAP.put("ic_food_bellpepper", R.drawable.ic_food_bellpepper);
        ICON_MAP.put("ic_food_salad", R.drawable.ic_food_salad);
        ICON_MAP.put("ic_food_mushroom", R.drawable.ic_food_mushroom);
        ICON_MAP.put("ic_food_beans", R.drawable.ic_food_beans);

        // Fruit
        ICON_MAP.put("ic_food_apple", R.drawable.ic_food_apple);
        ICON_MAP.put("ic_food_orange", R.drawable.ic_food_orange);
        ICON_MAP.put("ic_food_lemon", R.drawable.ic_food_lemon);
        ICON_MAP.put("ic_food_grape", R.drawable.ic_food_grape);
        ICON_MAP.put("ic_food_banana", R.drawable.ic_food_banana);
        ICON_MAP.put("ic_food_watermelon", R.drawable.ic_food_watermelon);
        ICON_MAP.put("ic_food_kiwi", R.drawable.ic_food_kiwi);
        ICON_MAP.put("ic_food_strawberry", R.drawable.ic_food_strawberry);
        ICON_MAP.put("ic_food_peach", R.drawable.ic_food_peach);
        ICON_MAP.put("ic_food_pineapple", R.drawable.ic_food_pineapple);
        ICON_MAP.put("ic_food_mango", R.drawable.ic_food_mango);
        ICON_MAP.put("ic_food_blueberry", R.drawable.ic_food_blueberry);
        ICON_MAP.put("ic_food_melon", R.drawable.ic_food_melon);
        ICON_MAP.put("ic_food_pear", R.drawable.ic_food_pear);
        ICON_MAP.put("ic_food_coconut", R.drawable.ic_food_coconut);

        // Meat
        ICON_MAP.put("ic_food_steak", R.drawable.ic_food_steak);
        ICON_MAP.put("ic_food_drumstick", R.drawable.ic_food_drumstick);
        ICON_MAP.put("ic_food_bacon", R.drawable.ic_food_bacon);
        ICON_MAP.put("ic_food_meatbone", R.drawable.ic_food_meatbone);

        // Seafood
        ICON_MAP.put("ic_food_fish", R.drawable.ic_food_fish);
        ICON_MAP.put("ic_food_shrimp", R.drawable.ic_food_shrimp);
        ICON_MAP.put("ic_food_crab", R.drawable.ic_food_crab);
        ICON_MAP.put("ic_food_squid", R.drawable.ic_food_squid);
        ICON_MAP.put("ic_food_octopus", R.drawable.ic_food_octopus);
        ICON_MAP.put("ic_food_lobster", R.drawable.ic_food_lobster);

        // Drink
        ICON_MAP.put("ic_food_water", R.drawable.ic_food_water);
        ICON_MAP.put("ic_food_soda", R.drawable.ic_food_soda);
        ICON_MAP.put("ic_food_coffee", R.drawable.ic_food_coffee);
        ICON_MAP.put("ic_food_tea", R.drawable.ic_food_tea);
        ICON_MAP.put("ic_food_boba", R.drawable.ic_food_boba);
        ICON_MAP.put("ic_food_beer", R.drawable.ic_food_beer);
        ICON_MAP.put("ic_food_wine", R.drawable.ic_food_wine);
        ICON_MAP.put("ic_food_juice", R.drawable.ic_food_juice);

        // Spice
        ICON_MAP.put("ic_food_salt", R.drawable.ic_food_salt);
        ICON_MAP.put("ic_food_honey", R.drawable.ic_food_honey);
        ICON_MAP.put("ic_food_olive", R.drawable.ic_food_olive);
        ICON_MAP.put("ic_food_peanut", R.drawable.ic_food_peanut);
        ICON_MAP.put("ic_food_chestnut", R.drawable.ic_food_chestnut);

        // Other
        ICON_MAP.put("ic_food_bread", R.drawable.ic_food_bread);
        ICON_MAP.put("ic_food_rice", R.drawable.ic_food_rice);
        ICON_MAP.put("ic_food_noodle", R.drawable.ic_food_noodle);
        ICON_MAP.put("ic_food_pasta", R.drawable.ic_food_pasta);
        ICON_MAP.put("ic_food_croissant", R.drawable.ic_food_croissant);
        ICON_MAP.put("ic_food_baguette", R.drawable.ic_food_baguette);
        ICON_MAP.put("ic_food_chocolate", R.drawable.ic_food_chocolate);
        ICON_MAP.put("ic_food_popcorn", R.drawable.ic_food_popcorn);
        ICON_MAP.put("ic_food_cupcake", R.drawable.ic_food_cupcake);
        ICON_MAP.put("ic_food_package", R.drawable.ic_food_package);

        // Expense category icons
        ICON_MAP.put("ic_shopping_cart", R.drawable.ic_shopping_cart);
        ICON_MAP.put("ic_delivery", R.drawable.ic_delivery);
        ICON_MAP.put("ic_snack", R.drawable.ic_snack);
    }

    // === CATEGORIES ===
    // { KEY, defaultIconName, drawableRes, label }
    public static final Object[][] CATEGORIES = {
            { "DAIRY", "ic_food_milk", R.drawable.ic_food_milk, "S\u1EEFa & Tr\u1EE9ng" },
            { "VEGETABLE", "ic_food_broccoli", R.drawable.ic_food_broccoli, "Rau c\u1EE7" },
            { "FRUIT", "ic_food_apple", R.drawable.ic_food_apple, "Tr\u00E1i c\u00E2y" },
            { "MEAT", "ic_food_steak", R.drawable.ic_food_steak, "Th\u1ECBt" },
            { "SEAFOOD", "ic_food_shrimp", R.drawable.ic_food_shrimp, "H\u1EA3i s\u1EA3n" },
            { "DRINK", "ic_food_juice", R.drawable.ic_food_juice, "\u0110\u1ED3 u\u1ED1ng" },
            { "SPICE", "ic_food_salt", R.drawable.ic_food_salt, "Gia v\u1ECB" },
            { "OTHER", "ic_food_package", R.drawable.ic_food_package, "Kh\u00E1c" },
    };

    // === PUBLIC API ===

    /** Lay drawable resource ID tu drawable name. */
    public static int getIconRes(String iconName) {
        if (iconName == null || iconName.isEmpty())
            return DEFAULT_ICON;
        Integer res = ICON_MAP.get(iconName);
        return res != null ? res : DEFAULT_ICON;
    }

    /** An toan — khong bao gio tra 0. */
    public static int safeIcon(String iconName) {
        return getIconRes(iconName);
    }

    /** Reverse lookup: resource ID -> drawable name. */
    public static String getIconName(int drawableRes) {
        for (Map.Entry<String, Integer> entry : ICON_MAP.entrySet()) {
            if (entry.getValue() == drawableRes)
                return entry.getKey();
        }
        return DEFAULT_ICON_NAME;
    }

    /** Tra ve icon theo nhom — dung cho IconPickerDialog. */
    public static Map<String, int[]> getGroupedIcons() {
        Map<String, int[]> map = new LinkedHashMap<>();

        map.put("S\u1EEFa & Tr\u1EE9ng", new int[] {
                R.drawable.ic_food_milk, R.drawable.ic_food_cheese,
                R.drawable.ic_food_butter, R.drawable.ic_food_egg,
                R.drawable.ic_food_icecream, R.drawable.ic_food_cake,
                R.drawable.ic_food_pudding
        });
        map.put("Rau c\u1EE7", new int[] {
                R.drawable.ic_food_broccoli, R.drawable.ic_food_lettuce,
                R.drawable.ic_food_carrot, R.drawable.ic_food_corn,
                R.drawable.ic_food_onion, R.drawable.ic_food_tomato,
                R.drawable.ic_food_cucumber, R.drawable.ic_food_chili,
                R.drawable.ic_food_garlic, R.drawable.ic_food_potato,
                R.drawable.ic_food_eggplant, R.drawable.ic_food_bellpepper,
                R.drawable.ic_food_salad, R.drawable.ic_food_mushroom,
                R.drawable.ic_food_beans
        });
        map.put("Tr\u00E1i c\u00E2y", new int[] {
                R.drawable.ic_food_apple, R.drawable.ic_food_orange,
                R.drawable.ic_food_lemon, R.drawable.ic_food_grape,
                R.drawable.ic_food_banana, R.drawable.ic_food_watermelon,
                R.drawable.ic_food_kiwi, R.drawable.ic_food_strawberry,
                R.drawable.ic_food_peach, R.drawable.ic_food_pineapple,
                R.drawable.ic_food_mango, R.drawable.ic_food_blueberry,
                R.drawable.ic_food_melon, R.drawable.ic_food_pear,
                R.drawable.ic_food_coconut
        });
        map.put("Th\u1ECBt", new int[] {
                R.drawable.ic_food_steak, R.drawable.ic_food_drumstick,
                R.drawable.ic_food_bacon, R.drawable.ic_food_meatbone
        });
        map.put("H\u1EA3i s\u1EA3n", new int[] {
                R.drawable.ic_food_fish, R.drawable.ic_food_shrimp,
                R.drawable.ic_food_crab, R.drawable.ic_food_squid,
                R.drawable.ic_food_octopus, R.drawable.ic_food_lobster
        });
        map.put("\u0110\u1ED3 u\u1ED1ng", new int[] {
                R.drawable.ic_food_water, R.drawable.ic_food_soda,
                R.drawable.ic_food_coffee, R.drawable.ic_food_tea,
                R.drawable.ic_food_boba, R.drawable.ic_food_beer,
                R.drawable.ic_food_wine, R.drawable.ic_food_juice
        });
        map.put("Gia v\u1ECB", new int[] {
                R.drawable.ic_food_salt, R.drawable.ic_food_honey,
                R.drawable.ic_food_olive, R.drawable.ic_food_peanut,
                R.drawable.ic_food_chestnut
        });
        map.put("Kh\u00E1c", new int[] {
                R.drawable.ic_food_bread, R.drawable.ic_food_rice,
                R.drawable.ic_food_noodle, R.drawable.ic_food_pasta,
                R.drawable.ic_food_croissant, R.drawable.ic_food_baguette,
                R.drawable.ic_food_chocolate,
                R.drawable.ic_food_popcorn, R.drawable.ic_food_cupcake,
                R.drawable.ic_food_package
        });

        return map;
    }

    /** Icon dai dien danh muc. */
    public static int getCategoryIcon(String categoryKey) {
        if (categoryKey == null)
            return DEFAULT_ICON;
        for (Object[] cat : CATEGORIES) {
            if (cat[0].equals(categoryKey))
                return (int) cat[2];
        }
        return DEFAULT_ICON;
    }

    /** Drawable name dai dien danh muc. */
    public static String getCategoryIconName(String categoryKey) {
        if (categoryKey == null)
            return DEFAULT_ICON_NAME;
        for (Object[] cat : CATEGORIES) {
            if (cat[0].equals(categoryKey))
                return (String) cat[1];
        }
        return DEFAULT_ICON_NAME;
    }

    /** Label hien thi danh muc. */
    public static String getCategoryLabel(String categoryKey) {
        if (categoryKey == null)
            return "Kh\u00E1c";
        for (Object[] cat : CATEGORIES) {
            if (cat[0].equals(categoryKey))
                return (String) cat[3];
        }
        return "Kh\u00E1c";
    }

    /** Mang key danh muc. */
    public static String[] getCategoryKeys() {
        String[] keys = new String[CATEGORIES.length];
        for (int i = 0; i < CATEGORIES.length; i++) {
            keys[i] = (String) CATEGORIES[i][0];
        }
        return keys;
    }

    /** Mang label hien thi. */
    public static String[] getCategoryLabels() {
        String[] labels = new String[CATEGORIES.length];
        for (int i = 0; i < CATEGORIES.length; i++) {
            labels[i] = (String) CATEGORIES[i][3];
        }
        return labels;
    }
}
