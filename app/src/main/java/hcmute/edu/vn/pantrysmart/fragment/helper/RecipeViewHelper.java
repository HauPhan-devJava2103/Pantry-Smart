package hcmute.edu.vn.pantrysmart.fragment.helper;

import hcmute.edu.vn.pantrysmart.R;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Helper xây dựng các thành phần giao diện cho màn hình chi tiết công thức.
 * Bao gồm: ingredient row, step row, tag pill.
 */
public class RecipeViewHelper {

    public static int dp(Context context, int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, value,
                context.getResources().getDisplayMetrics());
    }

    /**
     * Tạo row nguyên liệu: [icon] Trứng gà 2 quả Có sẵn
     * 
     * @param ingredientStr dạng "Tên - số lượng" (vd: "Trứng gà - 2 quả")
     * @param servings      số phần ăn (mặc định 1)
     */
    public static void addIngredientRow(Context context, LinearLayout container,
            String ingredientStr, boolean available, int servings) {
        // Parse "Tên - số lượng"
        String ingredientName;
        String quantityText;
        if (ingredientStr.contains(" - ")) {
            String[] parts = ingredientStr.split(" - ", 2);
            ingredientName = parts[0].trim();
            quantityText = scaleQuantity(parts[1].trim(), servings);
        } else {
            ingredientName = ingredientStr;
            quantityText = "";
        }

        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(context, 10), dp(context, 10), dp(context, 10), dp(context, 10));

        // Status icon: circle-plus (có sẵn) / circle-minus (thiếu)
        ImageView icon = new ImageView(context);
        int iconRes = available
                ? R.drawable.ic_ingredient_available
                : R.drawable.ic_ingredient_missing;
        icon.setImageResource(iconRes);
        icon.setLayoutParams(new LinearLayout.LayoutParams(dp(context, 20), dp(context, 20)));

        // Name
        TextView tvName = new TextView(context);
        tvName.setText(ingredientName);
        tvName.setTextSize(14);
        tvName.setTextColor(Color.parseColor("#1A1A1A"));
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        nameParams.setMarginStart(dp(context, 10));
        tvName.setLayoutParams(nameParams);

        // Quantity
        TextView tvQty = new TextView(context);
        tvQty.setText(quantityText);
        tvQty.setTextSize(12);
        tvQty.setTextColor(Color.parseColor("#6B7280"));
        LinearLayout.LayoutParams qtyParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        qtyParams.setMarginEnd(dp(context, 8));
        tvQty.setLayoutParams(qtyParams);

        // Status
        TextView tvStatus = new TextView(context);
        tvStatus.setText(available ? "Có sẵn" : "Thiếu");
        tvStatus.setTextSize(12);
        tvStatus.setTextColor(available ? Color.parseColor("#22C55E") : Color.parseColor("#EF4444"));

        row.addView(icon);
        row.addView(tvName);
        row.addView(tvQty);
        row.addView(tvStatus);
        container.addView(row);
    }

    // Tạo step row: [1] Đập trứng vào bát, đánh tan.
    public static void addStep(Context context, LinearLayout container, int number, String text) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(context, 6), 0, dp(context, 6));

        // Step number circle
        TextView numView = new TextView(context);
        numView.setText(String.valueOf(number));
        numView.setTextSize(12);
        numView.setTextColor(Color.WHITE);
        numView.setGravity(Gravity.CENTER);

        GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL);
        circle.setColor(Color.parseColor("#22C55E"));
        circle.setSize(dp(context, 24), dp(context, 24));
        numView.setBackground(circle);

        FrameLayout numFrame = new FrameLayout(context);
        numFrame.setLayoutParams(new LinearLayout.LayoutParams(dp(context, 24), dp(context, 24)));
        numFrame.addView(numView);

        // Step text in card
        TextView tvStep = new TextView(context);
        tvStep.setText(text);
        tvStep.setTextSize(14);
        tvStep.setTextColor(Color.parseColor("#4A5565"));
        tvStep.setPadding(dp(context, 12), dp(context, 8), dp(context, 12), dp(context, 8));

        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setColor(Color.parseColor("#F9FAFB"));
        cardBg.setCornerRadius(dp(context, 12));
        tvStep.setBackground(cardBg);

        LinearLayout.LayoutParams stepParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        stepParams.setMarginStart(dp(context, 12));
        tvStep.setLayoutParams(stepParams);

        row.addView(numFrame);
        row.addView(tvStep);
        container.addView(row);
    }

    // Tạo tag pill.
    public static void addTag(Context context, LinearLayout container, String text) {
        TextView tag = new TextView(context);
        tag.setText(text);
        tag.setTextSize(12);
        tag.setTextColor(Color.parseColor("#4A5565"));
        tag.setPadding(dp(context, 8), dp(context, 4), dp(context, 8), dp(context, 4));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#F3F4F6"));
        bg.setCornerRadius(dp(context, 100));
        bg.setStroke(dp(context, 1), Color.parseColor("#E5E7EB"));
        tag.setBackground(bg);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMarginEnd(dp(context, 8));
        tag.setLayoutParams(lp);

        container.addView(tag);
    }

    // Các đơn vị đếm được — luôn làm tròn lên thành số nguyên.
    private static final String[] COUNTABLE_UNITS = {
            "quả", "con", "lát", "miếng", "nhánh", "tép", "cây",
            "bông", "trái", "củ", "khúc", "lá", "cọng", "lon"
    };

    /**
     * Nhân số lượng nguyên liệu theo số phần ăn.
     * Đơn vị đếm được (quả, con, lát...) sẽ được làm tròn LÊN thành số nguyên.
     */
    public static String scaleQuantity(String qtyStr, int servings) {
        if (servings <= 1 || qtyStr == null || qtyStr.isEmpty())
            return qtyStr;

        // Tìm số đầu tiên trong chuỗi
        StringBuilder numPart = new StringBuilder();
        StringBuilder unitPart = new StringBuilder();
        boolean foundNum = false;
        boolean numDone = false;

        for (char c : qtyStr.toCharArray()) {
            if (!numDone && (Character.isDigit(c) || c == '.' || c == ',' || c == '/')) {
                numPart.append(c);
                foundNum = true;
            } else {
                if (foundNum)
                    numDone = true;
                unitPart.append(c);
            }
        }

        if (!foundNum)
            return qtyStr;

        try {
            String numStr = numPart.toString();
            double value;

            if (numStr.contains("/")) {
                String[] fraction = numStr.split("/");
                value = Double.parseDouble(fraction[0]) / Double.parseDouble(fraction[1]);
            } else {
                value = Double.parseDouble(numStr.replace(",", "."));
            }

            double scaled = value * servings;

            // Kiểm tra đơn vị có phải đếm được không
            String unit = unitPart.toString().trim().toLowerCase();
            boolean isCountable = false;
            for (String cu : COUNTABLE_UNITS) {
                if (unit.contains(cu)) {
                    isCountable = true;
                    break;
                }
            }

            String scaledStr;
            if (isCountable) {
                // Đơn vị đếm được luôn làm tròn LÊN
                scaledStr = String.valueOf((int) Math.ceil(scaled));
            } else if (scaled == Math.floor(scaled)) {
                scaledStr = String.valueOf((int) scaled);
            } else {
                scaledStr = String.format("%.1f", scaled);
            }

            return scaledStr + unitPart.toString();
        } catch (NumberFormatException e) {
            return qtyStr;
        }
    }

    /**
     * Trích tên nguyên liệu từ chuỗi "Tên - số lượng" (bỏ phần số lượng).
     * Dùng để tra cứu DB xem nguyên liệu có sẵn không.
     */
    public static String extractIngredientName(String ingredientStr) {
        if (ingredientStr == null)
            return "";
        if (ingredientStr.contains(" - ")) {
            return ingredientStr.split(" - ", 2)[0].trim();
        }
        return ingredientStr.trim();
    }
}
