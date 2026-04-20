package hcmute.edu.vn.pantrysmart.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.concurrent.TimeUnit;

import hcmute.edu.vn.pantrysmart.R;
import hcmute.edu.vn.pantrysmart.config.FoodIconConfig;
import hcmute.edu.vn.pantrysmart.data.local.entity.PantryItem;

/**
 * Adapter hiển thị danh sách thông báo hết hạn / sắp hết hạn.
 */
public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {

    public static class NotifItem {
        public final PantryItem item;
        public final boolean isExpired; // true = đã hết hạn, false = sắp hết hạn
        public final long daysLeft; // số ngày còn lại (negative nếu đã qua)

        public NotifItem(PantryItem item, boolean isExpired, long daysLeft) {
            this.item = item;
            this.isExpired = isExpired;
            this.daysLeft = daysLeft;
        }
    }

    private final Context context;
    private final List<NotifItem> items;

    public NotificationAdapter(Context context, List<NotifItem> items) {
        this.context = context;
        this.items = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_notification, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        NotifItem notif = items.get(position);
        PantryItem item = notif.item;

        // Icon thực phẩm
        holder.imgIcon.setImageResource(
                FoodIconConfig.safeIcon(item.getEmoji()));

        // Tên + số lượng
        String qty = formatQty(item.getQuantity());
        String unit = item.getUnit() != null ? item.getUnit() : "";
        holder.tvName.setText(item.getName() + " (" + qty + " " + unit + ")");

        // Message + Badge
        if (notif.isExpired) {
            long daysAgo = Math.abs(notif.daysLeft);
            if (daysAgo == 0) {
                holder.tvMessage.setText("Đã hết hạn hôm nay");
            } else {
                holder.tvMessage.setText("Đã hết hạn " + daysAgo + " ngày trước");
            }
            holder.tvStatus.setText("Hết hạn");
            holder.tvStatus.setBackgroundResource(R.drawable.bg_notif_badge_expired);
        } else {
            if (notif.daysLeft == 0) {
                holder.tvMessage.setText("Hết hạn hôm nay!");
            } else if (notif.daysLeft == 1) {
                holder.tvMessage.setText("Còn 1 ngày nữa hết hạn");
            } else {
                holder.tvMessage.setText("Còn " + notif.daysLeft + " ngày nữa hết hạn");
            }
            holder.tvStatus.setText("Sắp hết");
            holder.tvStatus.setBackgroundResource(R.drawable.bg_notif_badge_warning);
        }
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    private String formatQty(double qty) {
        if (qty == Math.floor(qty))
            return String.valueOf((int) qty);
        return String.format("%.1f", qty);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgIcon;
        TextView tvName, tvMessage, tvStatus;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgIcon = itemView.findViewById(R.id.imgNotifIcon);
            tvName = itemView.findViewById(R.id.tvNotifItemName);
            tvMessage = itemView.findViewById(R.id.tvNotifMessage);
            tvStatus = itemView.findViewById(R.id.tvNotifStatus);
        }
    }
}
