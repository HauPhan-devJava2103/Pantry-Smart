package hcmute.edu.vn.pantrysmart.notification;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import hcmute.edu.vn.pantrysmart.MainActivity;
import hcmute.edu.vn.pantrysmart.R;

// Helper gửi cảnh báo hết hạn
public class NotificationHelper {
    private static final String CHANNEL_ID = "expiry_channel";
    private static final String CHANNEL_NAME = "Cảnh báo hết hạn";
    private static final int NOTIFICATION_ID = 1001; // dung de update hoac ghi de

    // Tạo Notification Channel
    public static void createChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Tạo channel
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Thông báo khi thực phẩm trong tủ lạnh sắp hết hạn");
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    // Gửi cảnh báo thực phẩm
    public static void sendExpiryNotification(Context context,
            int expiringSoon,
            int alreadyExpired,
            String[] lines) {
        // Intent mở app khi bấm notification
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Tiêu đề từ string resource
        int total = expiringSoon + alreadyExpired;
        String title;
        if (alreadyExpired > 0 && expiringSoon > 0) {
            title = context.getString(R.string.notif_title_both,
                    alreadyExpired, expiringSoon);
        } else if (alreadyExpired > 0) {
            title = context.getString(R.string.notif_title_expired, alreadyExpired);
        } else {
            title = context.getString(R.string.notif_title_expiring, expiringSoon);
        }

        // InboxStyle — mỗi item 1 dòng riêng
        NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle()
                .setBigContentTitle(title)
                .setSummaryText(context.getString(R.string.notif_summary));

        for (String line : lines) {
            inboxStyle.addLine(line);
        }

        // Tạo notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notifications)
                .setContentTitle(title)
                .setContentText(context.getString(R.string.notif_content, total))
                .setStyle(inboxStyle)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, builder.build());
        }
    }

}
