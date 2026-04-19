package hcmute.edu.vn.pantrysmart.worker;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import hcmute.edu.vn.pantrysmart.R;
import hcmute.edu.vn.pantrysmart.data.local.PantrySmartDatabase;
import hcmute.edu.vn.pantrysmart.data.local.dao.PantryItemDao;
import hcmute.edu.vn.pantrysmart.data.local.entity.PantryItem;
import hcmute.edu.vn.pantrysmart.notification.NotificationHelper;

// Kiểm tra thực phẩm hết hạn
public class ExpiryCheckWorker extends Worker {

    private static final String TAG = "ExpiryCheckWorker";

    public ExpiryCheckWorker(@NonNull Context context,
            @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "Kiểm tra hạn sử dụng");

        Context context = getApplicationContext();
        PantrySmartDatabase db = PantrySmartDatabase.getInstance(context);
        PantryItemDao dao = db.pantryItemDao();
        // Lấy tất cả item đang active
        List<PantryItem> allItems = dao.getAllActiveItems();
        // Lấy thời gian hiện tại
        long now = System.currentTimeMillis();
        // Ngưỡng hạn: 2 ngày
        long threshold = now + TimeUnit.DAYS.toMillis(2);

        int expiringSoon = 0;
        int alreadyExpired = 0;
        ArrayList<String> lines = new ArrayList<>();

        for (PantryItem item : allItems) {
            Long expiryDate = item.getExpiryDate();
            if (expiryDate == null)
                continue;

            String qty = formatQty(item.getQuantity()) + " " +
                    (item.getUnit() != null ? item.getUnit() : "");
            String qtyTrimmed = qty.trim();

            if (expiryDate < now) {
                alreadyExpired++;
                lines.add(context.getString(R.string.notif_expired,
                        item.getName(), qtyTrimmed));
            } else if (expiryDate <= threshold) {
                long daysLeft = TimeUnit.MILLISECONDS.toDays(expiryDate - now);
                expiringSoon++;
                if (daysLeft == 0) {
                    lines.add(context.getString(R.string.notif_today,
                            item.getName(), qtyTrimmed));
                } else {
                    lines.add(context.getString(R.string.notif_days_left,
                            (int) daysLeft, item.getName(), qtyTrimmed));
                }
            }
        }

        int total = expiringSoon + alreadyExpired;
        if (total > 0) {
            Log.d(TAG, "Tìm thấy " + total + " item cần cảnh báo");
            NotificationHelper.sendExpiryNotification(
                    context, expiringSoon, alreadyExpired,
                    lines.toArray(new String[0]));
        } else {
            Log.d(TAG, "Không có item nào sắp hết hạn");
        }
        return Result.success();
    }

    private static String formatQty(double qty) {
        if (qty == Math.floor(qty))
            return String.valueOf((int) qty);
        return String.format("%.1f", qty);
    }
}
