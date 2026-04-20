package hcmute.edu.vn.pantrysmart.fragment;

import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.graphics.drawable.PaintDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import android.widget.Toast;

import com.airbnb.lottie.LottieAnimationView;

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
    private LottieAnimationView ivLoadingIcon;
    private TextView tvLoadingStep;

    private RecipeCardAdapter adapter;
    private Handler loadingHandler;
    private int loadingStepIndex = 0;

    private static final String[] LOADING_STEPS = {
            "Đang kiểm tra tủ lạnh...",
            "Phân tích nguyên liệu có sẵn...",
            "AI đang nghĩ món ngon cho bạn...",
            "Tính toán công thức chi tiết...",
            "Sắp xong rồi, chờ chút nhé..."
    };

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

        // Hiệu ứng swipe trang
        viewPager.setPageTransformer((page, position) -> {
            float absPos = Math.abs(position);
            page.setAlpha(1f - absPos * 0.25f);
            page.setScaleY(1f - absPos * 0.08f);
        });

        // Gradient chuyển màu khi swipe
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                updateGradientBackground(position, positionOffset);
            }
        });

        btnRetry.setOnClickListener(v -> loadRecipeSuggestions());

        loadRecipeSuggestions();
    }

    // Gradient chuyển màu khi swipe
    private void updateGradientBackground(int position, float offset) {
        // Lấy màu
        int[][] colors = RecipeCardAdapter.GRADIENT_COLORS;
        int count = colors.length;

        int currentIdx = position % count;
        int nextIdx = (position + 1) % count;

        // Xử lý màu
        int startColor = lerpColor(colors[currentIdx][0], colors[nextIdx][0], offset);
        int centerColor = lerpColor(colors[currentIdx][1], colors[nextIdx][1], offset);
        int endColor = lerpColor(colors[currentIdx][2], colors[nextIdx][2], offset);

        // Tạo gradient và áp dụng vào background
        PaintDrawable drawable = new PaintDrawable();
        drawable.setShape(new RectShape());
        drawable.setShaderFactory(new ShapeDrawable.ShaderFactory() {
            @Override
            public Shader resize(int width, int height) {
                return new LinearGradient(
                        0, 0, width, height,
                        new int[] { startColor, centerColor, endColor },
                        new float[] { 0f, 0.5f, 1f },
                        Shader.TileMode.CLAMP);
            }
        });

        bgGradient.setBackground(drawable);
    }

    // Trộn màu
    private int lerpColor(int colorA, int colorB, float t) {
        int aA = Color.alpha(colorA), aB = Color.alpha(colorB);
        int rA = Color.red(colorA), rB = Color.red(colorB);
        int gA = Color.green(colorA), gB = Color.green(colorB);
        int bA = Color.blue(colorA), bB = Color.blue(colorB);

        return Color.argb(
                (int) (aA + (aB - aA) * t),
                (int) (rA + (rB - rA) * t),
                (int) (gA + (gB - gA) * t),
                (int) (bA + (bB - bA) * t));
    }

    // Load gợi ý món ăn
    private void loadRecipeSuggestions() {
        showLoading();

        // Lấy database
        PantrySmartDatabase db = PantrySmartDatabase.getInstance(requireContext());
        PantryItemDao dao = db.pantryItemDao();

        // Lấy danh sách thực phẩm
        PantrySmartDatabase.databaseWriteExecutor.execute(() -> {
            List<PantryItem> items = dao.getAllActiveItems();

            // Nếu không có thực phẩm
            if (items.isEmpty()) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> showError(getString(R.string.suggest_empty)));
                }
                return;
            }

            // Gọi Gemini API
            GeminiRecipeService.suggestRecipes(items, new GeminiRecipeService.RecipeCallback() {
                @Override
                // Thành công
                public void onSuccess(List<RecipeSuggestion> recipes) {
                    if (isAdded()) {
                        requireActivity().runOnUiThread(() -> showRecipes(recipes));
                    }
                }

                @Override
                // Lỗi
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

    // Hiển thị màn hình chờ với animation
    private void showLoading() {
        loadingOverlay.setVisibility(View.VISIBLE);
        viewPager.setVisibility(View.GONE);
        errorOverlay.setVisibility(View.GONE);

        // Gán view cho text update
        tvLoadingStep = loadingOverlay.findViewById(R.id.tvLoadingStep);
        ivLoadingIcon = loadingOverlay.findViewById(R.id.ivLoadingIcon);

        // Chạy lại anim khi load lại
        if (ivLoadingIcon != null) {
            ivLoadingIcon.playAnimation();
        }

        // Text step xoay vòng mỗi 2.5 giây
        loadingStepIndex = 0;
        loadingHandler = new Handler(Looper.getMainLooper());
        startLoadingStepCycle();
    }

    private void startLoadingStepCycle() {
        if (loadingHandler == null) return;
        loadingHandler.postDelayed(() -> {
            if (!isAdded() || tvLoadingStep == null) return;

            loadingStepIndex = (loadingStepIndex + 1) % LOADING_STEPS.length;
            tvLoadingStep.setText(LOADING_STEPS[loadingStepIndex]);

            // Hiệu ứng fade in
            tvLoadingStep.startAnimation(
                    AnimationUtils.loadAnimation(requireContext(), R.anim.fade_in_up));

            startLoadingStepCycle();
        }, 2500);
    }

    private void stopLoadingAnimation() {
        if (loadingHandler != null) {
            loadingHandler.removeCallbacksAndMessages(null);
            loadingHandler = null;
        }
        if (ivLoadingIcon != null) {
            ivLoadingIcon.cancelAnimation();
        }
    }

    // Hiển thị danh sách món ăn
    private void showRecipes(List<RecipeSuggestion> recipes) {
        stopLoadingAnimation();
        loadingOverlay.setVisibility(View.GONE);
        errorOverlay.setVisibility(View.GONE);
        viewPager.setVisibility(View.VISIBLE);

        adapter = new RecipeCardAdapter(recipes, this::onStartCooking);
        viewPager.setAdapter(adapter);

        updateGradientBackground(0, 0f);
    }

    // Hiển thị màn hình lỗi
    private void showError(String message) {
        stopLoadingAnimation();
        loadingOverlay.setVisibility(View.GONE);
        viewPager.setVisibility(View.GONE);
        errorOverlay.setVisibility(View.VISIBLE);
        tvErrorMessage.setText(message);
    }

    // Xử lý khi bắt đầu nấu
    // Mở màn hình chi tiết công thức
    private void onStartCooking(RecipeSuggestion recipe) {
        RecipeDetailFragment detail = RecipeDetailFragment.newInstance(recipe);
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(
                        android.R.anim.slide_in_left,
                        android.R.anim.slide_out_right,
                        android.R.anim.slide_in_left,
                        android.R.anim.slide_out_right)
                .replace(android.R.id.content, detail)
                .addToBackStack("recipe_detail")
                .commit();
    }
}
