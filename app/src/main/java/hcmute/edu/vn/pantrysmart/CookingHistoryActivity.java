package hcmute.edu.vn.pantrysmart;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import hcmute.edu.vn.pantrysmart.adapter.CookingHistoryAdapter;
import hcmute.edu.vn.pantrysmart.data.local.PantrySmartDatabase;
import hcmute.edu.vn.pantrysmart.data.local.dao.CookingLogDao;
import hcmute.edu.vn.pantrysmart.data.local.relation.CookingLogWithItems;
import hcmute.edu.vn.pantrysmart.fragment.RecipeDetailFragment;
import hcmute.edu.vn.pantrysmart.model.RecipeSuggestion;

// Màn hình xem lịch sử nấu ăn
public class CookingHistoryActivity extends AppCompatActivity {

    private RecyclerView rvHistory;
    private View layoutEmpty;
    private TextView tvTotalCount;
    private CookingHistoryAdapter adapter;

    // Chip filter views
    private TextView chipAll, chipToday, chipWeek, chipMonth;
    private TextView activeChip;

    // Filter: 0 = tất cả, mốc timestamp nếu lọc
    private long filterFrom = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cooking_history);

        // Bind views
        rvHistory = findViewById(R.id.rvCookingHistory);
        layoutEmpty = findViewById(R.id.layoutEmpty);
        tvTotalCount = findViewById(R.id.tvTotalCount);

        // Chips
        chipAll = findViewById(R.id.chipAll);
        chipToday = findViewById(R.id.chipToday);
        chipWeek = findViewById(R.id.chipWeek);
        chipMonth = findViewById(R.id.chipMonth);
        activeChip = chipAll;

        // Back button
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Setup RecyclerView
        rvHistory.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CookingHistoryAdapter(null, this::openRecipeDetail);
        rvHistory.setAdapter(adapter);

        // Chip click listeners
        chipAll.setOnClickListener(v -> applyFilter(chipAll, 0));
        chipToday.setOnClickListener(v -> applyFilter(chipToday, getStartOfToday()));
        chipWeek.setOnClickListener(v -> applyFilter(chipWeek, getStartOfWeek()));
        chipMonth.setOnClickListener(v -> applyFilter(chipMonth, getStartOfMonth()));

        // Load data
        loadHistory();
    }

    // Áp dụng filter + cập nhật UI chip
    private void applyFilter(TextView chip, long from) {
        if (activeChip == chip) return;

        // Đổi trạng thái chip cũ → inactive
        activeChip.setBackgroundResource(R.drawable.bg_chip_inactive);
        activeChip.setTextColor(Color.parseColor("#6B7280"));

        // Chip mới → active
        chip.setBackgroundResource(R.drawable.bg_chip_active);
        chip.setTextColor(Color.WHITE);
        activeChip = chip;

        filterFrom = from;
        loadHistory();
    }

    private void loadHistory() {
        PantrySmartDatabase.databaseWriteExecutor.execute(() -> {
            CookingLogDao dao = PantrySmartDatabase.getInstance(this).cookingLogDao();

            List<CookingLogWithItems> logs;
            int total;

            if (filterFrom > 0) {
                logs = dao.getLogsAfter(filterFrom);
                total = dao.getCountAfter(filterFrom);
            } else {
                logs = dao.getRecentCookingLogs(50);
                total = dao.getTotalCookCount();
            }

            runOnUiThread(() -> {
                if (logs == null || logs.isEmpty()) {
                    rvHistory.setVisibility(View.GONE);
                    layoutEmpty.setVisibility(View.VISIBLE);
                    tvTotalCount.setText("");
                } else {
                    rvHistory.setVisibility(View.VISIBLE);
                    layoutEmpty.setVisibility(View.GONE);
                    adapter.updateData(logs);
                    tvTotalCount.setText(getString(R.string.cooking_history_count, total));
                }
            });
        });
    }

    // Mở lại RecipeDetailFragment từ recipeJson đã lưu
    private void openRecipeDetail(CookingLogWithItems log) {
        String recipeJson = log.cookingLog.getRecipeJson();
        if (recipeJson == null || recipeJson.isEmpty()) return;

        try {
            JSONObject json = new JSONObject(recipeJson);

            RecipeSuggestion recipe = new RecipeSuggestion();
            recipe.setDishName(json.optString("dishName", ""));
            recipe.setDescription(json.optString("description", ""));
            recipe.setCookingTime(json.optString("cookingTime", ""));
            recipe.setDifficulty(json.optString("difficulty", ""));
            recipe.setImageSearch(json.optString("imageSearch", ""));

            JSONArray ingredArr = json.optJSONArray("ingredients");
            if (ingredArr != null) {
                List<String> ingredients = new ArrayList<>();
                for (int i = 0; i < ingredArr.length(); i++) {
                    ingredients.add(ingredArr.getString(i));
                }
                recipe.setMatchedIngredients(ingredients);
            }

            JSONArray stepArr = json.optJSONArray("steps");
            if (stepArr != null) {
                List<String> steps = new ArrayList<>();
                for (int i = 0; i < stepArr.length(); i++) {
                    steps.add(stepArr.getString(i));
                }
                recipe.setSteps(steps);
            }

            getSupportFragmentManager().beginTransaction()
                    .replace(android.R.id.content,
                            RecipeDetailFragment.newInstance(recipe))
                    .addToBackStack(null)
                    .commit();

        } catch (Exception e) {
            android.widget.Toast.makeText(this,
                    "Không thể mở lại công thức", android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    // === Tính mốc thời gian ===

    // Đầu ngày hôm nay (00:00)
    private long getStartOfToday() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    // Đầu tuần (Thứ 2, 00:00)
    private long getStartOfWeek() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        if (cal.getTimeInMillis() > System.currentTimeMillis()) {
            cal.add(Calendar.WEEK_OF_YEAR, -1);
        }
        return cal.getTimeInMillis();
    }

    // Đầu tháng (ngày 1, 00:00)
    private long getStartOfMonth() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }
}
