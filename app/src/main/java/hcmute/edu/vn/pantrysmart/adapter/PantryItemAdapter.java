package hcmute.edu.vn.pantrysmart.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;

import java.util.List;
import java.util.concurrent.TimeUnit;

import hcmute.edu.vn.pantrysmart.R;
import hcmute.edu.vn.pantrysmart.config.FoodEmojiConfig;
import hcmute.edu.vn.pantrysmart.data.local.entity.PantryItem;

/**
 * Adapter hiển thị danh sách thực phẩm trong BottomSheet dialog.
 * Dùng RecyclerView + ViewHolder pattern để tái dụng view hiệu quả.
 */
public class PantryItemAdapter extends RecyclerView.Adapter<PantryItemAdapter.ViewHolder> {

    public interface OnItemActionListener {
        void onEdit(PantryItem item, int position);
        void onDelete(PantryItem item, int position);
    }

    private final Context context;
    private final List<PantryItem> items;
    private OnItemActionListener listener;

    public PantryItemAdapter(Context context, List<PantryItem> items) {
        this.context = context;
        this.items = items;
    }

    public void setOnItemActionListener(OnItemActionListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_pantry_row, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PantryItem item = items.get(position);
        long now = System.currentTimeMillis();

        // Hình ảnh / Emoji
        if (item.getImagePath() != null && !item.getImagePath().isEmpty()) {
            holder.imgFood.setVisibility(View.VISIBLE);
            holder.layoutEmojiHolder.setVisibility(View.GONE);
            Glide.with(context)
                    .load(item.getImagePath())
                    .centerCrop()
                    .transform(new RoundedCorners(dp(12)))
                    .placeholder(R.drawable.bg_item_image_placeholder)
                    .error(R.drawable.bg_item_image_placeholder)
                    .into(holder.imgFood);
        } else {
            holder.imgFood.setVisibility(View.GONE);
            holder.layoutEmojiHolder.setVisibility(View.VISIBLE);
            holder.tvEmoji.setText(FoodEmojiConfig.safeEmoji(item.getEmoji()));
        }

        // Tên + Số lượng
        holder.tvItemName.setText(item.getName());
        holder.tvItemQuantity.setText(formatQuantity(item.getQuantity()) + " " + item.getUnit());

        // Badge hạn sử dụng
        if (item.getExpiryDate() != null) {
            long daysLeft = TimeUnit.MILLISECONDS.toDays(item.getExpiryDate() - now);
            boolean isExpired  = item.getExpiryDate() < now;
            boolean isExpiring = !isExpired && daysLeft <= 2;

            if (isExpired) {
                holder.tvExpiryBadge.setText("Đã hết hạn");
                holder.tvExpiryBadge.setTextColor(Color.parseColor("#FB2C36"));
                holder.tvExpiryBadge.setBackgroundResource(R.drawable.bg_status_badge_expired);
            } else if (isExpiring) {
                holder.tvExpiryBadge.setText("Còn " + daysLeft + " ngày");
                holder.tvExpiryBadge.setTextColor(Color.parseColor("#EA580C"));
                holder.tvExpiryBadge.setBackgroundResource(R.drawable.bg_status_badge_warning);
            } else {
                holder.tvExpiryBadge.setText("Còn " + daysLeft + " ngày");
                holder.tvExpiryBadge.setTextColor(Color.parseColor("#059669"));
                holder.tvExpiryBadge.setBackgroundResource(R.drawable.bg_status_badge_fresh);
            }
        } else {
            holder.tvExpiryBadge.setText("Không rõ hạn");
            holder.tvExpiryBadge.setTextColor(Color.parseColor("#9CA3AF"));
            holder.tvExpiryBadge.setBackgroundResource(R.drawable.bg_status_badge_fresh);
        }

        // Action buttons
        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) listener.onEdit(item, holder.getAdapterPosition());
        });
        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDelete(item, holder.getAdapterPosition());
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    /** Xoá item khỏi danh sách và thông báo RecyclerView cập nhật */
    public void removeItem(int position) {
        items.remove(position);
        notifyItemRemoved(position);
        notifyItemRangeChanged(position, items.size());
    }

    private String formatQuantity(double qty) {
        if (qty == (long) qty) return String.valueOf((long) qty);
        return String.valueOf(qty);
    }

    private int dp(int dp) {
        return (int) (dp * context.getResources().getDisplayMetrics().density + 0.5f);
    }

    // ViewHolder

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView imgFood;
        final LinearLayout layoutEmojiHolder;
        final TextView tvEmoji;
        final TextView tvItemName;
        final TextView tvItemQuantity;
        final TextView tvExpiryBadge;
        final View btnEdit;
        final View btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgFood           = itemView.findViewById(R.id.imgFood);
            layoutEmojiHolder = itemView.findViewById(R.id.layoutEmojiPlaceholder);
            tvEmoji           = itemView.findViewById(R.id.tvEmoji);
            tvItemName        = itemView.findViewById(R.id.tvItemName);
            tvItemQuantity    = itemView.findViewById(R.id.tvItemQuantity);
            tvExpiryBadge     = itemView.findViewById(R.id.tvExpiryBadge);
            btnEdit           = itemView.findViewById(R.id.btnEditItem);
            btnDelete         = itemView.findViewById(R.id.btnDeleteItem);
        }
    }
}
