package hcmute.edu.vn.pantrysmart.fragment.helper;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import hcmute.edu.vn.pantrysmart.R;
import hcmute.edu.vn.pantrysmart.data.local.PantrySmartDatabase;
import hcmute.edu.vn.pantrysmart.data.local.dao.CookingLogDao;
import hcmute.edu.vn.pantrysmart.data.local.dao.PantryItemDao;
import hcmute.edu.vn.pantrysmart.data.local.entity.CookingLog;
import hcmute.edu.vn.pantrysmart.data.local.entity.CookingLogItem;
import hcmute.edu.vn.pantrysmart.data.local.entity.PantryItem;

/**
 * Helper hiển thị dialog xác nhận trừ nguyên liệu sau khi nấu xong.
 * Người dùng có thể điều chỉnh số lượng trừ tuỳ ý trước khi xác nhận.
 */
public class CookingDialogHelper {

    // Dữ liệu 1 dòng nguyên liệu trong dialog.
    private static class DeductRow {
        String ingredientName;
        EditText etQty;
        String unit;
    }

    // Hiển thị dialog trừ nguyên liệu.
    public static void showDeductDialog(Context context, String dishName,
            String[] ingredients, String imageUrl, String recipeJson,
            Runnable onComplete) {
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_deduct_ingredients);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
        }

        dialog.setCancelable(false);

        TextView tvDesc = dialog.findViewById(R.id.tvDialogDesc);
        tvDesc.setText("Nguyên liệu dùng cho \"" + dishName + "\" sẽ được trừ khỏi tủ lạnh");

        // Populate danh sách nguyên liệu
        LinearLayout llItems = dialog.findViewById(R.id.llDeductItems);
        LayoutInflater inflater = LayoutInflater.from(context);
        List<DeductRow> rows = new ArrayList<>();

        for (String ingredientStr : ingredients) {
            View row = inflater.inflate(R.layout.item_deduct_ingredient, llItems, false);

            TextView tvName = row.findViewById(R.id.tvDeductName);
            EditText etQty = row.findViewById(R.id.etDeductQty);
            TextView tvUnit = row.findViewById(R.id.tvDeductUnit);

            DeductRow data = new DeductRow();

            if (ingredientStr.contains(" - ")) {
                String[] parts = ingredientStr.split(" - ", 2);
                data.ingredientName = parts[0].trim();
                tvName.setText(data.ingredientName);

                // Tách số và đơn vị: "200g" → "200" + "g"
                String qtyPart = parts[1].trim();
                String numStr = extractNumber(qtyPart);
                String unitStr = qtyPart.substring(numStr.length()).trim();

                etQty.setText(numStr);
                tvUnit.setText(unitStr);
                data.unit = unitStr;
            } else {
                data.ingredientName = ingredientStr;
                tvName.setText(ingredientStr);
                etQty.setText("1");
                tvUnit.setText("");
                data.unit = "";
            }

            data.etQty = etQty;
            rows.add(data);
            llItems.addView(row);
        }

        // Nút Huỷ
        dialog.findViewById(R.id.btnDialogCancel).setOnClickListener(v -> dialog.dismiss());

        // Nút Xác nhận — validate rồi mới trừ
        dialog.findViewById(R.id.btnDialogConfirm).setOnClickListener(v -> {
            validateAndDeduct(context, dialog, dishName, imageUrl, recipeJson, rows, onComplete);
        });

        dialog.show();
    }

    // Kiểm tra số lượng nhập có vượt quá tủ lạnh không.
    // Nếu vượt: cảnh báo, nếu hợp lệ: trừ luôn.
    private static void validateAndDeduct(Context context, Dialog dialog,
            String dishName, String imageUrl, String recipeJson,
            List<DeductRow> rows, Runnable onComplete) {
        PantrySmartDatabase db = PantrySmartDatabase.getInstance(context);
        PantryItemDao dao = db.pantryItemDao();

        PantrySmartDatabase.databaseWriteExecutor.execute(() -> {
            StringBuilder warnings = new StringBuilder();

            for (DeductRow row : rows) {
                double amount;
                try {
                    String text = row.etQty.getText().toString().replace(",", ".").trim();
                    amount = text.isEmpty() ? 0 : Double.parseDouble(text);
                } catch (NumberFormatException e) {
                    amount = 0;
                }
                if (amount <= 0)
                    continue;

                PantryItem item = dao.findByName(row.ingredientName);
                if (item == null) {
                    warnings.append("• ").append(row.ingredientName)
                            .append(" — không có trong tủ\n");
                } else {
                    double converted = convertToItemUnit(amount, row.unit, item.getUnit());
                    if (converted > item.getQuantity()) {
                        String pantryInfo = formatQty(item.getQuantity()) + " " +
                                (item.getUnit() != null ? item.getUnit() : "");
                        warnings.append("• ").append(row.ingredientName)
                                .append(" — chỉ còn ").append(pantryInfo.trim())
                                .append(" (bạn nhập ").append(formatQty(amount))
                                .append(" ").append(row.unit).append(")\n");
                    }
                }
            }

            android.os.Handler mainHandler = new android.os.Handler(
                    android.os.Looper.getMainLooper());

            if (warnings.length() > 0) {
                mainHandler.post(() -> {
                    new android.app.AlertDialog.Builder(context)
                            .setTitle("Số lượng vượt quá")
                            .setMessage("Một số nguyên liệu vượt quá tồn kho:\n\n" +
                                    warnings.toString().trim() +
                                    "\n\nBạn vẫn muốn trừ? (phần vượt sẽ bị xoá khỏi tủ)")
                            .setPositiveButton("Vẫn trừ", (d, w) -> {
                                dialog.dismiss();
                                deductIngredients(context, dishName, imageUrl, recipeJson, rows, onComplete);
                            })
                            .setNegativeButton("Sửa lại", null)
                            .show();
                });
            } else {
                mainHandler.post(() -> {
                    dialog.dismiss();
                    deductIngredients(context, dishName, imageUrl, recipeJson, rows, onComplete);
                });
            }
        });
    }

    // Format số: 0.5 → "0.5", 2.0 → "2"
    private static String formatQty(double qty) {
        if (qty == Math.floor(qty)) {
            return String.valueOf((int) qty);
        }
        return String.format("%.1f", qty);
    }

    // Trích phần số từ chuỗi "200g" → "200", "2 quả" → "2".
    private static String extractNumber(String qtyStr) {
        StringBuilder sb = new StringBuilder();
        for (char c : qtyStr.toCharArray()) {
            if (Character.isDigit(c) || c == '.' || c == ',') {
                sb.append(c);
            } else if (sb.length() > 0) {
                break;
            }
        }
        return sb.length() > 0 ? sb.toString() : "1";
    }

    // Thực hiện trừ nguyên liệu trong DB theo số lượng người dùng đã chỉnh.
    private static void deductIngredients(Context context, String dishName,
            String imageUrl, String recipeJson,
            List<DeductRow> rows, Runnable onComplete) {
        PantrySmartDatabase db = PantrySmartDatabase.getInstance(context);
        PantryItemDao dao = db.pantryItemDao();

        PantrySmartDatabase.databaseWriteExecutor.execute(() -> {
            int deducted = 0;
            List<DeductedInfo> deductedItems = new ArrayList<>();

            for (DeductRow row : rows) {
                // Đọc số lượng người dùng đã chỉnh trong EditText
                double amount;
                try {
                    String text = row.etQty.getText().toString().replace(",", ".").trim();
                    amount = text.isEmpty() ? 0 : Double.parseDouble(text);
                } catch (NumberFormatException e) {
                    amount = 0;
                }

                if (amount <= 0)
                    continue;

                PantryItem item = dao.findByName(row.ingredientName);
                if (item != null && item.getQuantity() > 0) {
                    // Quy đổi đơn vị công thức → đơn vị tủ lạnh
                    double converted = convertToItemUnit(amount, row.unit, item.getUnit());
                    // Làm tròn 2 chữ số để tránh lỗi floating-point (4.7213... → 4.72)
                    double newQty = Math.round(Math.max(0, item.getQuantity() - converted) * 100.0) / 100.0;

                    if (newQty <= 0) {
                        dao.delete(item);
                    } else {
                        item.setQuantity(newQty);
                        dao.update(item);
                    }
                    deducted++;

                    // Ghi lại thông tin đã trừ
                    deductedItems.add(new DeductedInfo(
                            row.ingredientName, amount, row.unit,
                            item.getId()));
                }
            }

            // LƯU LỊCH SỬ NẤU ĂN
            if (deducted > 0) {
                saveCookingLog(db, dishName, imageUrl, recipeJson, deducted, deductedItems);
            }

            final int count = deducted;

            android.os.Handler mainHandler = new android.os.Handler(
                    android.os.Looper.getMainLooper());
            mainHandler.post(() -> {
                Toast.makeText(context,
                        "Đã trừ " + count + " nguyên liệu cho \"" + dishName + "\"",
                        Toast.LENGTH_SHORT).show();
                if (onComplete != null) {
                    onComplete.run();
                }
            });
        });
    }

    // Dữ liệu tạm lưu nguyên liệu đã trừ
    private static class DeductedInfo {
        String name;
        double quantity;
        String unit;
        int pantryItemId;

        DeductedInfo(String name, double quantity, String unit, int pantryItemId) {
            this.name = name;
            this.quantity = quantity;
            this.unit = unit;
            this.pantryItemId = pantryItemId;
        }
    }

    // Lưu lịch sử nấu ăn vào database
    private static void saveCookingLog(PantrySmartDatabase db,
            String dishName, String imageUrl, String recipeJson,
            int deductedCount, List<DeductedInfo> deductedItems) {
        CookingLogDao logDao = db.cookingLogDao();

        // Tạo log chính
        CookingLog log = new CookingLog();
        log.setDishName(dishName);
        log.setImageUrl(imageUrl);
        log.setRecipeJson(recipeJson);
        log.setIngredientsDeducted(deductedCount);
        long logId = logDao.insertLog(log);

        // Tạo chi tiết nguyên liệu
        List<CookingLogItem> logItems = new ArrayList<>();
        for (DeductedInfo info : deductedItems) {
            CookingLogItem logItem = new CookingLogItem();
            logItem.setCookingLogId((int) logId);
            logItem.setPantryItemId(info.pantryItemId);
            logItem.setItemName(info.name);
            logItem.setQuantityUsed(info.quantity);
            logItem.setUnit(info.unit);
            logItems.add(logItem);
        }
        logDao.insertLogItems(logItems);
    }

    /**
     * Quy đổi đơn vị từ công thức sang đơn vị trong tủ lạnh.
     *
     * Hỗ trợ:
     * - Khối lượng: g ↔ kg
     * - Thể tích: ml ↔ l/lít
     * - Đồng nghĩa: quả=trái, muỗng=thìa/thìa cà phê
     * - Đếm được cùng loại: củ, tép, con, lát... → 1:1
     */
    private static double convertToItemUnit(double amount, String recipeUnit, String pantryUnit) {
        if (recipeUnit == null || pantryUnit == null)
            return amount;

        String from = normalize(recipeUnit);
        String to = normalize(pantryUnit);

        // Cùng đơn vị → không cần đổi
        if (from.equals(to))
            return amount;

        // Khối lượng
        if (from.equals("g") && to.equals("kg"))
            return amount / 1000.0;
        if (from.equals("kg") && to.equals("g"))
            return amount * 1000.0;

        // Thể tích
        if (from.equals("ml") && to.equals("l"))
            return amount / 1000.0;
        if (from.equals("l") && to.equals("ml"))
            return amount * 1000.0;

        // Đếm được: củ ↔ tép (1 củ ≈ 10 tép — tỏi, hành)
        if (from.equals("tép") && to.equals("củ"))
            return amount / 10.0;
        if (from.equals("củ") && to.equals("tép"))
            return amount * 10.0;

        // Không quy đổi được → giữ nguyên
        return amount;
    }

    /**
     * Chuẩn hoá đơn vị: gộp các tên đồng nghĩa về 1 dạng.
     * "trái" → "quả", "lít" → "l", "thìa" → "muỗng", v.v.
     */
    private static String normalize(String unit) {
        if (unit == null)
            return "";
        String u = unit.toLowerCase().trim();

        // Đồng nghĩa khối lượng / thể tích
        if (u.equals("lít"))
            return "l";

        // Đồng nghĩa đếm được
        if (u.equals("trái"))
            return "quả";
        if (u.equals("thìa") || u.equals("thìa cà phê") || u.equals("muỗng cà phê"))
            return "muỗng";
        if (u.equals("thìa canh") || u.equals("muỗng canh"))
            return "muỗng canh";

        return u;
    }
}
