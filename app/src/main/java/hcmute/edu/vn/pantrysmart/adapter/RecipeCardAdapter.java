package hcmute.edu.vn.pantrysmart.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;

import java.util.List;

import hcmute.edu.vn.pantrysmart.R;
import hcmute.edu.vn.pantrysmart.model.RecipeSuggestion;

/**
 * Adapter cho ViewPager2 — Hiển thị từng gợi ý món ăn dạng card full-screen.
 * Mỗi card có gradient khác nhau để tạo hiệu ứng sinh động khi swipe.
 */
public class RecipeCardAdapter extends RecyclerView.Adapter<RecipeCardAdapter.CardViewHolder> {

    private final List<RecipeSuggestion> recipes;
    private final OnCookClickListener listener;

    // Gradient colors for smooth transition (used by SuggestFragment)
    public static final int[][] GRADIENT_COLORS = {
            {0xFFA78BFA, 0xFFB07BFC, 0xFFD946EF}, // Purple-Magenta
            {0xFFFB923C, 0xFFF97316, 0xFFEC4899}, // Orange-Pink
            {0xFF5EEAD4, 0xFF14B8A6, 0xFF06B6D4}, // Teal-Cyan
            {0xFFFDA4AF, 0xFFF43F5E, 0xFFA855F7}, // Rose-Purple
            {0xFF93C5FD, 0xFF3B82F6, 0xFF6366F1}, // Blue-Indigo
    };

    // Ảnh mẫu Unsplash (fallback khi Glide không load được)
    private static final String[] FOOD_IMAGES = {
            "https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=400&h=400&fit=crop",
            "https://images.unsplash.com/photo-1565958011703-44f9829ba187?w=400&h=400&fit=crop",
            "https://images.unsplash.com/photo-1482049016688-2d3e1b311543?w=400&h=400&fit=crop",
            "https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=400&h=400&fit=crop",
            "https://images.unsplash.com/photo-1493770348161-369560ae357d?w=400&h=400&fit=crop"
    };

    public interface OnCookClickListener {
        void onCookClick(RecipeSuggestion recipe);
    }

    public RecipeCardAdapter(List<RecipeSuggestion> recipes, OnCookClickListener listener) {
        this.recipes = recipes;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_suggest_recipe, parent, false);
        return new CardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CardViewHolder holder, int position) {
        RecipeSuggestion recipe = recipes.get(position);

        // Set food name
        holder.tvFoodName.setText(recipe.getDishName());

        // Load food image via Glide
        String imageUrl = FOOD_IMAGES[position % FOOD_IMAGES.length];
        Glide.with(holder.itemView.getContext())
                .load(imageUrl)
                .transform(new CenterCrop(), new RoundedCorners(48))
                .into(holder.ivFoodImage);

        // Cook button click
        holder.btnStartCooking.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCookClick(recipe);
            }
        });
    }

    @Override
    public int getItemCount() {
        return recipes != null ? recipes.size() : 0;
    }

    static class CardViewHolder extends RecyclerView.ViewHolder {
        final View cardRoot;
        final TextView tvFoodName;
        final ImageView ivFoodImage;
        final View btnStartCooking;

        CardViewHolder(@NonNull View itemView) {
            super(itemView);
            cardRoot = itemView.findViewById(R.id.cardRoot);
            tvFoodName = itemView.findViewById(R.id.tvFoodName);
            ivFoodImage = itemView.findViewById(R.id.ivFoodImage);
            btnStartCooking = itemView.findViewById(R.id.btnStartCooking);
        }
    }
}
