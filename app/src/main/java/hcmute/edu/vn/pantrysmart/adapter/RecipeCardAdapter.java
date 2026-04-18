package hcmute.edu.vn.pantrysmart.adapter;

import android.os.Handler;
import android.os.Looper;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import hcmute.edu.vn.pantrysmart.R;
import hcmute.edu.vn.pantrysmart.config.PexelsImageService;
import hcmute.edu.vn.pantrysmart.model.RecipeSuggestion;

/**
 * Adapter cho ViewPager2 — Hiển thị từng gợi ý món ăn dạng card full-screen.
 * Ảnh được tìm từ Pexels API dựa trên tên món.
 */
public class RecipeCardAdapter extends RecyclerView.Adapter<RecipeCardAdapter.CardViewHolder> {

    private final List<RecipeSuggestion> recipes;
    private final OnCookClickListener listener;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService imageExecutor = Executors.newSingleThreadExecutor();

    // Cache URL ảnh đã tìm
    private final Map<String, String> imageCache = new HashMap<>();

    // Gradient colors for smooth transition
    public static final int[][] GRADIENT_COLORS = {
            {0xFFA78BFA, 0xFFB07BFC, 0xFFD946EF},
            {0xFFFB923C, 0xFFF97316, 0xFFEC4899},
            {0xFF5EEAD4, 0xFF14B8A6, 0xFF06B6D4},
            {0xFFFDA4AF, 0xFFF43F5E, 0xFFA855F7},
            {0xFF93C5FD, 0xFF3B82F6, 0xFF6366F1},
    };

    private static final String PLACEHOLDER_IMAGE =
            "https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=400&h=400&fit=crop";

    public interface OnCookClickListener {
        void onCookClick(RecipeSuggestion recipe);
    }

    public RecipeCardAdapter(List<RecipeSuggestion> recipes, OnCookClickListener listener) {
        this.recipes = recipes;
        this.listener = listener;
        preloadAllImages();
    }

    /**
     * Tìm ảnh cho tất cả món ăn tuần tự qua Pexels.
     */
    private void preloadAllImages() {
        imageExecutor.execute(() -> {
            for (int i = 0; i < recipes.size(); i++) {
                RecipeSuggestion recipe = recipes.get(i);
                String key = recipe.getDishName();

                // Tìm ảnh: tiếng Việt trước tìm tiếng Anh sau trả fallback
                String imageUrl = PexelsImageService.searchFoodImage(
                        recipe.getImageSearch()
                );
                if (imageUrl != null) {
                    imageCache.put(key, imageUrl);
                    final int pos = i;
                    mainHandler.post(() -> notifyItemChanged(pos));
                }
            }
        });
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
        String dishName = recipe.getDishName();

        holder.tvFoodName.setText(dishName);

        // Ảnh từ cache hoặc placeholder
        String imageUrl = imageCache.containsKey(dishName)
                ? imageCache.get(dishName)
                : PLACEHOLDER_IMAGE;

        Glide.with(holder.itemView.getContext())
                .load(imageUrl)
                .transform(new CenterCrop(), new RoundedCorners(48))
                .into(holder.ivFoodImage);

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
