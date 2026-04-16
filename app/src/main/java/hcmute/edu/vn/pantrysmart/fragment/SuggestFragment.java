package hcmute.edu.vn.pantrysmart.fragment;

import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.graphics.drawable.PaintDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import java.util.List;

import hcmute.edu.vn.pantrysmart.R;
import hcmute.edu.vn.pantrysmart.adapter.RecipeCardAdapter;
import hcmute.edu.vn.pantrysmart.config.GeminiRecipeService;
import hcmute.edu.vn.pantrysmart.data.local.PantrySmartDatabase;
import hcmute.edu.vn.pantrysmart.data.local.dao.PantryItemDao;
import hcmute.edu.vn.pantrysmart.data.local.entity.PantryItem;
import hcmute.edu.vn.pantrysmart.model.RecipeSuggestion;

/**
 * SuggestFragment — Tab "Gợi ý" trong MainActivity.
 *
 * Smooth gradient transition khi swipe giữa các món ăn.
 * Background gradient được interpolate realtime theo vị trí swipe.
 */
public class SuggestFragment extends Fragment {

    private static final String TAG = "SuggestFragment";

    private ViewPager2 viewPager;
    private View bgGradient;
    private View loadingOverlay;
    private View errorOverlay;
    private TextView tvErrorMessage;
    private View btnRetry;

    private RecipeCardAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_suggest, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewPager = view.findViewById(R.id.viewPagerRecipes);
        bgGradient = view.findViewById(R.id.bgGradient);
        loadingOverlay = view.findViewById(R.id.loadingOverlay);
        errorOverlay = view.findViewById(R.id.errorOverlay);
        tvErrorMessage = view.findViewById(R.id.tvErrorMessage);
        btnRetry = view.findViewById(R.id.btnRetry);

        // Page transition effect: subtle scale + fade
        viewPager.setPageTransformer((page, position) -> {
            float absPos = Math.abs(position);
            page.setAlpha(1f - absPos * 0.25f);
            page.setScaleY(1f - absPos * 0.08f);
        });

        // ★ Smooth gradient color transition on page scroll
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                updateGradientBackground(position, positionOffset);
            }
        });

        btnRetry.setOnClickListener(v -> loadRecipeSuggestions());

        loadRecipeSuggestions();
    }

    /**
     * Interpolate gradient colors between current and next page.
     * Creates a smooth color transition as the user swipes.
     */
    private void updateGradientBackground(int position, float offset) {
        int[][] colors = RecipeCardAdapter.GRADIENT_COLORS;
        int count = colors.length;

        int currentIdx = position % count;
        int nextIdx = (position + 1) % count;

        // Lerp each of the 3 gradient stops between current and next
        int startColor = lerpColor(colors[currentIdx][0], colors[nextIdx][0], offset);
        int centerColor = lerpColor(colors[currentIdx][1], colors[nextIdx][1], offset);
        int endColor = lerpColor(colors[currentIdx][2], colors[nextIdx][2], offset);

        // Create gradient drawable and apply to background
        PaintDrawable drawable = new PaintDrawable();
        drawable.setShape(new RectShape());
        drawable.setShaderFactory(new ShapeDrawable.ShaderFactory() {
            @Override
            public Shader resize(int width, int height) {
                return new LinearGradient(
                        0, 0, width, height,
                        new int[]{startColor, centerColor, endColor},
                        new float[]{0f, 0.5f, 1f},
                        Shader.TileMode.CLAMP);
            }
        });

        bgGradient.setBackground(drawable);
    }

    /**
     * Linear interpolation between two ARGB colors.
     */
    private int lerpColor(int colorA, int colorB, float t) {
        int aA = Color.alpha(colorA), aB = Color.alpha(colorB);
        int rA = Color.red(colorA), rB = Color.red(colorB);
        int gA = Color.green(colorA), gB = Color.green(colorB);
        int bA = Color.blue(colorA), bB = Color.blue(colorB);

        return Color.argb(
                (int) (aA + (aB - aA) * t),
                (int) (rA + (rB - rA) * t),
                (int) (gA + (gB - gA) * t),
                (int) (bA + (bB - bA) * t)
        );
    }

    /**
     * Load gợi ý món ăn: DB → Gemini API → UI
     */
    private void loadRecipeSuggestions() {
        showLoading();

        PantrySmartDatabase db = PantrySmartDatabase.getInstance(requireContext());
        PantryItemDao dao = db.pantryItemDao();

        PantrySmartDatabase.databaseWriteExecutor.execute(() -> {
            List<PantryItem> items = dao.getAllActiveItems();

            if (items.isEmpty()) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() ->
                            showError(getString(R.string.suggest_empty)));
                }
                return;
            }

            GeminiRecipeService.suggestRecipes(items, new GeminiRecipeService.RecipeCallback() {
                @Override
                public void onSuccess(List<RecipeSuggestion> recipes) {
                    if (isAdded()) {
                        requireActivity().runOnUiThread(() -> showRecipes(recipes));
                    }
                }

                @Override
                public void onError(String errorMessage) {
                    Log.e(TAG, "Gemini API error: " + errorMessage);
                    List<RecipeSuggestion> demo = GeminiRecipeService.getDemoRecipes(items);
                    if (isAdded()) {
                        requireActivity().runOnUiThread(() -> showRecipes(demo));
                    }
                }
            });
        });
    }

    private void showLoading() {
        loadingOverlay.setVisibility(View.VISIBLE);
        viewPager.setVisibility(View.GONE);
        errorOverlay.setVisibility(View.GONE);
    }

    private void showRecipes(List<RecipeSuggestion> recipes) {
        loadingOverlay.setVisibility(View.GONE);
        errorOverlay.setVisibility(View.GONE);
        viewPager.setVisibility(View.VISIBLE);

        adapter = new RecipeCardAdapter(recipes, this::onStartCooking);
        viewPager.setAdapter(adapter);

        // Set initial gradient
        updateGradientBackground(0, 0f);
    }

    private void showError(String message) {
        loadingOverlay.setVisibility(View.GONE);
        viewPager.setVisibility(View.GONE);
        errorOverlay.setVisibility(View.VISIBLE);
        tvErrorMessage.setText(message);
    }

    private void onStartCooking(RecipeSuggestion recipe) {
        Toast.makeText(requireContext(),
                "Bắt đầu nấu: " + recipe.getDishName(),
                Toast.LENGTH_SHORT).show();
        // TODO: Navigate to cooking steps screen or log to CookingLog
    }
}
