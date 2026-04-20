package hcmute.edu.vn.pantrysmart.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.shape.CornerFamily;

import java.util.List;
import java.util.concurrent.TimeUnit;

import hcmute.edu.vn.pantrysmart.R;
import hcmute.edu.vn.pantrysmart.data.local.entity.CookingLogItem;
import hcmute.edu.vn.pantrysmart.data.local.relation.CookingLogWithItems;

// Adapter hiển thị danh sách lịch sử nấu ăn
public class CookingHistoryAdapter extends RecyclerView.Adapter<CookingHistoryAdapter.ViewHolder> {

    private List<CookingLogWithItems> logs;
    private OnViewRecipeListener listener;

    public interface OnViewRecipeListener {
        void onViewRecipe(CookingLogWithItems log);
    }

    public CookingHistoryAdapter(List<CookingLogWithItems> logs, OnViewRecipeListener listener) {
        this.logs = logs;
        this.listener = listener;
    }

    public void updateData(List<CookingLogWithItems> newLogs) {
        this.logs = newLogs;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_cooking_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CookingLogWithItems log = logs.get(position);

        // Tên món
        holder.tvRecipeName.setText(log.cookingLog.getDishName());

        // Tóm tắt nguyên liệu ĐÃ TRỪ (kèm số lượng thực tế)
        StringBuilder ingredients = new StringBuilder();
        for (int i = 0; i < log.items.size(); i++) {
            CookingLogItem item = log.items.get(i);
            if (i > 0) ingredients.append(", ");
            ingredients.append(item.getItemName());

            // Hiện số lượng đã trừ
            double qty = item.getQuantityUsed();
            String unit = item.getUnit();
            if (qty > 0) {
                String qtyStr = (qty == Math.floor(qty))
                        ? String.valueOf((int) qty) : String.format("%.1f", qty);
                ingredients.append(" (").append(qtyStr);
                if (unit != null && !unit.isEmpty()) {
                    ingredients.append(" ").append(unit);
                }
                ingredients.append(")");
            }
        }
        holder.tvIngredients.setText(
                ingredients.length() > 0 ? ingredients.toString() : "Không có nguyên liệu");

        // Thời gian tương đối
        holder.tvTime.setText(getRelativeTime(log.cookingLog.getCookedAt()));

        // Ảnh món ăn từ Pexels
        String imageUrl = log.cookingLog.getImageUrl();
        // Bo tròn ảnh
        float radius = holder.itemView.getContext()
                .getResources().getDisplayMetrics().density * 12;
        holder.ivDishImage.setShapeAppearanceModel(
                holder.ivDishImage.getShapeAppearanceModel().toBuilder()
                        .setAllCorners(CornerFamily.ROUNDED, radius)
                        .build());

        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(imageUrl)
                    .centerCrop()
                    .placeholder(R.drawable.ic_restaurant)
                    .into(holder.ivDishImage);
        } else {
            holder.ivDishImage.setImageResource(R.drawable.ic_restaurant);
        }

        // Nút xem lại — ẩn nếu không có recipe data
        String recipeJson = log.cookingLog.getRecipeJson();
        if (recipeJson != null && !recipeJson.isEmpty() && listener != null) {
            holder.btnViewRecipe.setVisibility(View.VISIBLE);
            holder.btnViewRecipe.setOnClickListener(v -> listener.onViewRecipe(log));
        } else {
            holder.btnViewRecipe.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return logs != null ? logs.size() : 0;
    }

    // Tính thời gian tương đối
    private String getRelativeTime(long timestamp) {
        long diff = System.currentTimeMillis() - timestamp;
        long minutes = TimeUnit.MILLISECONDS.toMinutes(diff);
        long hours = TimeUnit.MILLISECONDS.toHours(diff);
        long days = TimeUnit.MILLISECONDS.toDays(diff);

        if (minutes < 1) return "Vừa xong";
        if (minutes < 60) return minutes + " phút trước";
        if (hours < 24) return hours + " giờ trước";
        if (days == 1) return "Hôm qua";
        if (days < 7) return days + " ngày trước";
        return days / 7 + " tuần trước";
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ShapeableImageView ivDishImage;
        TextView tvRecipeName, tvIngredients, tvTime, btnViewRecipe;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivDishImage = itemView.findViewById(R.id.ivDishImage);
            tvRecipeName = itemView.findViewById(R.id.tvHistoryRecipeName);
            tvIngredients = itemView.findViewById(R.id.tvHistoryIngredients);
            tvTime = itemView.findViewById(R.id.tvHistoryTime);
            btnViewRecipe = itemView.findViewById(R.id.btnViewRecipe);
        }
    }
}
