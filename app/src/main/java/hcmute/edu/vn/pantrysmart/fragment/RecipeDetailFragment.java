package hcmute.edu.vn.pantrysmart.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;

import java.util.List;

import hcmute.edu.vn.pantrysmart.R;
import hcmute.edu.vn.pantrysmart.config.PexelsImageService;
import hcmute.edu.vn.pantrysmart.data.local.PantrySmartDatabase;
import hcmute.edu.vn.pantrysmart.data.local.dao.PantryItemDao;
import hcmute.edu.vn.pantrysmart.data.local.entity.PantryItem;
import hcmute.edu.vn.pantrysmart.fragment.helper.CookingDialogHelper;
import hcmute.edu.vn.pantrysmart.fragment.helper.RecipeTagHelper;
import hcmute.edu.vn.pantrysmart.fragment.helper.RecipeViewHelper;
import hcmute.edu.vn.pantrysmart.model.RecipeSuggestion;

/**
 * Màn hình chi tiết công thức nấu ăn.
 * Hiển thị nguyên liệu, hướng dẫn từng bước,
 * và nút "Đã nấu xong — Trừ nguyên liệu".
 */
public class RecipeDetailFragment extends Fragment {

    private static final String ARG_DISH = "dish_name";
    private static final String ARG_DESC = "description";
    private static final String ARG_TIME = "cooking_time";
    private static final String ARG_DIFF = "difficulty";
    private static final String ARG_INGREDIENTS = "ingredients";
    private static final String ARG_STEPS = "steps";
    private static final String ARG_IMAGE_SEARCH = "image_search";

    private RecipeSuggestion recipe;
    private String currentImageUrl;

    public static RecipeDetailFragment newInstance(RecipeSuggestion recipe) {
        RecipeDetailFragment f = new RecipeDetailFragment();
        Bundle args = new Bundle();
        args.putString(ARG_DISH, recipe.getDishName());
        args.putString(ARG_DESC, recipe.getDescription());
        args.putString(ARG_TIME, recipe.getCookingTime());
        args.putString(ARG_DIFF, recipe.getDifficulty());
        args.putString(ARG_IMAGE_SEARCH, recipe.getImageSearch());

        // Ingredients
        List<String> ingredients = recipe.getMatchedIngredients();
        if (ingredients != null) {
            args.putStringArray(ARG_INGREDIENTS, ingredients.toArray(new String[0]));
        }

        // Steps
        List<String> steps = recipe.getSteps();
        if (steps != null) {
            args.putStringArray(ARG_STEPS, steps.toArray(new String[0]));
        }

        f.setArguments(args);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
            @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_recipe_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args = getArguments();
        if (args == null)
            return;

        String dishName = args.getString(ARG_DISH, "");
        String description = args.getString(ARG_DESC, "");
        String cookingTime = args.getString(ARG_TIME, "");
        String difficulty = args.getString(ARG_DIFF, "");
        String[] ingredients = args.getStringArray(ARG_INGREDIENTS);
        String[] steps = args.getStringArray(ARG_STEPS);
        String imageSearch = args.getString(ARG_IMAGE_SEARCH, "");

        // Header
        TextView tvDishName = view.findViewById(R.id.tvDishName);
        TextView tvCookingTime = view.findViewById(R.id.tvCookingTime);
        TextView tvDifficulty = view.findViewById(R.id.tvDifficulty);
        TextView tvDescription = view.findViewById(R.id.tvDescription);
        ImageView ivHeaderFood = view.findViewById(R.id.ivHeaderFood);

        tvDishName.setText(dishName);
        tvCookingTime.setText(cookingTime);
        tvDifficulty.setText(difficulty);
        tvDescription.setText(description);

        // Load ảnh Pexels vào header
        loadHeaderImage(ivHeaderFood, dishName, imageSearch);

        // Tags — tự động từ dữ liệu recipe
        LinearLayout tagsContainer = view.findViewById(R.id.tagsContainer);
        RecipeViewHelper.addTag(requireContext(), tagsContainer, RecipeTagHelper.getMealType(dishName));

        // Tag "Nhanh" nếu thời gian ≤ 15 phút
        int minutes = RecipeTagHelper.parseMinutes(cookingTime);
        if (minutes > 0 && minutes <= 15) {
            RecipeViewHelper.addTag(requireContext(), tagsContainer, "Nhanh");
        }

        // Tag chay/mặn dựa vào nguyên liệu
        if (ingredients != null) {
            RecipeViewHelper.addTag(requireContext(), tagsContainer, RecipeTagHelper.getDietTag(ingredients));
        }

        // Ingredients (khẩu phần 1 người)
        LinearLayout ingredientsContainer = view.findViewById(R.id.ingredientsContainer);
        if (ingredients != null) {
            loadIngredients(ingredientsContainer, ingredients);
        }

        // Steps
        LinearLayout stepsContainer = view.findViewById(R.id.stepsContainer);
        if (steps != null && steps.length > 0) {
            for (int i = 0; i < steps.length; i++) {
                RecipeViewHelper.addStep(requireContext(), stepsContainer, i + 1, steps[i]);
            }
        } else {
            RecipeViewHelper.addStep(requireContext(), stepsContainer, 1, "Chuẩn bị nguyên liệu");
            RecipeViewHelper.addStep(requireContext(), stepsContainer, 2, "Sơ chế nguyên liệu");
            RecipeViewHelper.addStep(requireContext(), stepsContainer, 3, "Nấu theo hướng dẫn");
        }

        // Bottom buttons
        final String[] dialogIngredients = ingredients;
        final String[] dialogSteps = steps;

        view.findViewById(R.id.btnFinishCooking).setOnClickListener(v -> {
            if (dialogIngredients != null && dialogIngredients.length > 0) {
                // Build recipe JSON để lưu lịch sử
                String recipeJson = buildRecipeJson(dishName, description,
                        cookingTime, difficulty, imageSearch,
                        dialogIngredients, dialogSteps);

                CookingDialogHelper.showDeductDialog(
                        requireContext(), dishName, dialogIngredients,
                        currentImageUrl, recipeJson,
                        () -> requireActivity().getSupportFragmentManager().popBackStack());
            } else {
                Toast.makeText(requireContext(),
                        "Không có nguyên liệu để trừ", Toast.LENGTH_SHORT).show();
            }
        });

        view.findViewById(R.id.btnClose).setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().popBackStack();
        });
    }

    // Load ảnh món ăn từ Pexels vào header.
    private void loadHeaderImage(ImageView imageView, String dishName, String imageSearch) {
        PantrySmartDatabase.databaseWriteExecutor.execute(() -> {
            String query = (imageSearch != null && !imageSearch.isEmpty())
                    ? imageSearch
                    : dishName;
            String imageUrl = PexelsImageService.searchFoodImage(query);
            currentImageUrl = imageUrl;

            if (isAdded() && imageUrl != null) {
                requireActivity().runOnUiThread(() -> Glide.with(requireContext())
                        .load(imageUrl)
                        .centerCrop()
                        .into(imageView));
            }
        });
    }

    /**
     * Load nguyên liệu và kiểm tra có sẵn trong tủ lạnh.
     * Hiển thị khẩu phần cho 1 người ăn.
     */
    private void loadIngredients(LinearLayout container, String[] ingredients) {
        PantrySmartDatabase db = PantrySmartDatabase.getInstance(requireContext());
        PantryItemDao dao = db.pantryItemDao();

        PantrySmartDatabase.databaseWriteExecutor.execute(() -> {
            for (String ingredientStr : ingredients) {
                String lookupName = RecipeViewHelper.extractIngredientName(ingredientStr);
                PantryItem item = dao.findByName(lookupName);
                boolean available = item != null && item.getQuantity() > 0;

                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> RecipeViewHelper.addIngredientRow(requireContext(), container,
                            ingredientStr, available, 1));
                }
            }
        });
    }

    // Build JSON chứa thông tin recipe để lưu vào lịch sử
    private String buildRecipeJson(String dishName, String description,
            String cookingTime, String difficulty, String imageSearch,
            String[] ingredients, String[] steps) {
        try {
            org.json.JSONObject json = new org.json.JSONObject();
            json.put("dishName", dishName);
            json.put("description", description);
            json.put("cookingTime", cookingTime);
            json.put("difficulty", difficulty);
            json.put("imageSearch", imageSearch);

            org.json.JSONArray ingredArr = new org.json.JSONArray();
            if (ingredients != null) {
                for (String ing : ingredients) ingredArr.put(ing);
            }
            json.put("ingredients", ingredArr);

            org.json.JSONArray stepArr = new org.json.JSONArray();
            if (steps != null) {
                for (String step : steps) stepArr.put(step);
            }
            json.put("steps", stepArr);

            return json.toString();
        } catch (Exception e) {
            return null;
        }
    }
}
