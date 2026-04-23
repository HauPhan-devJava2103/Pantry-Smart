package hcmute.edu.vn.pantrysmart.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.graphics.Color;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import hcmute.edu.vn.pantrysmart.R;
import hcmute.edu.vn.pantrysmart.adapter.PeriodAdapter;
import hcmute.edu.vn.pantrysmart.adapter.RecentTransactionAdapter;
import hcmute.edu.vn.pantrysmart.config.FoodIconConfig;
import hcmute.edu.vn.pantrysmart.data.local.PantrySmartDatabase;
import hcmute.edu.vn.pantrysmart.data.local.dao.ExpenseDao;
import hcmute.edu.vn.pantrysmart.data.local.entity.Expense;
import hcmute.edu.vn.pantrysmart.data.local.entity.ExpenseCategory;

public class ExpenseDetailActivity extends AppCompatActivity {

    private RecyclerView rvMonths;
    private RecyclerView rvExpenses;
    private TextView tvNoData;
    private ImageView btnBack;
    private ImageView btnFilterPeriod;
    private BarChart barChart;
    private BarChart barChartMonthly;
    private TextView tvChartTitle;

    private PeriodAdapter periodAdapter;
    private RecentTransactionAdapter transactionAdapter;

    private PantrySmartDatabase database;
    private ExpenseDao expenseDao;
    private ExecutorService executorService;
    private Map<String, ExpenseCategory> categoryMap = new HashMap<>();

    private boolean isWeeklyMode = false; // false = Month, true = Week

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_expense_detail);

        if (getIntent() != null) {
            isWeeklyMode = getIntent().getBooleanExtra("IS_WEEKLY_MODE", false);
        }

        rvMonths = findViewById(R.id.rvMonths);
        rvExpenses = findViewById(R.id.rvExpenses);
        tvNoData = findViewById(R.id.tvNoData);
        btnBack = findViewById(R.id.btnBack);
        btnFilterPeriod = findViewById(R.id.btnFilterPeriod);
        barChart = findViewById(R.id.detailBarChart);
        barChartMonthly = findViewById(R.id.detailBarChartMonthly);
        tvChartTitle = findViewById(R.id.tvDetailChartTitle);

        btnBack.setOnClickListener(v -> finish());
        
        btnFilterPeriod.setOnClickListener(v -> showFilterMenu());

        database = PantrySmartDatabase.getInstance(this);
        expenseDao = database.expenseDao();
        executorService = Executors.newSingleThreadExecutor();

        setupTransactionAdapter();
        setupPeriodAdapter();
        loadCategoriesAndInitData();
    }

    private void showFilterMenu() {
        com.google.android.material.bottomsheet.BottomSheetDialog dialog = new com.google.android.material.bottomsheet.BottomSheetDialog(this);
        View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_period_mode, null);
        dialog.setContentView(sheetView);

        View btnModeMonth = sheetView.findViewById(R.id.btnModeMonth);
        View btnModeWeek = sheetView.findViewById(R.id.btnModeWeek);
        ImageView iconCheckMonth = sheetView.findViewById(R.id.iconCheckMonth);
        ImageView iconCheckWeek = sheetView.findViewById(R.id.iconCheckWeek);
        TextView tvModeMonth = sheetView.findViewById(R.id.tvModeMonth);
        TextView tvModeWeek = sheetView.findViewById(R.id.tvModeWeek);

        if (isWeeklyMode) {
            btnModeWeek.setBackgroundResource(R.drawable.bg_category_selected);
            tvModeWeek.setTextColor(android.graphics.Color.parseColor("#00BC7D"));
            iconCheckWeek.setVisibility(View.VISIBLE);
        } else {
            btnModeMonth.setBackgroundResource(R.drawable.bg_category_selected);
            tvModeMonth.setTextColor(android.graphics.Color.parseColor("#00BC7D"));
            iconCheckMonth.setVisibility(View.VISIBLE);
        }

        btnModeMonth.setOnClickListener(v -> {
            if (isWeeklyMode) {
                isWeeklyMode = false;
                updatePeriods();
            }
            dialog.dismiss();
        });

        btnModeWeek.setOnClickListener(v -> {
            if (!isWeeklyMode) {
                isWeeklyMode = true;
                updatePeriods();
            }
            dialog.dismiss();
        });

        dialog.show();
    }

    private void setupTransactionAdapter() {
        transactionAdapter = new RecentTransactionAdapter(new ArrayList<>(),
                new RecentTransactionAdapter.CategoryEmojiResolver() {
                    @Override
                    public String getEmoji(String key) {
                        ExpenseCategory c = categoryMap.get(key);
                        return c != null ? c.getEmoji() : FoodIconConfig.DEFAULT_ICON_NAME;
                    }

                    @Override
                    public String getLabel(String key) {
                        ExpenseCategory c = categoryMap.get(key);
                        return c != null ? c.getLabel() : "Khác";
                    }
                }, true); // Pass true for isReadOnly
        
        transactionAdapter.setOnActionListener(new RecentTransactionAdapter.OnActionListener() {
            @Override
            public void onEdit(Expense expense) {}

            @Override
            public void onDelete(Expense expense) {}

            @Override
            public void onClick(Expense expense) {
                showViewExpenseBottomSheet(expense);
            }
        });
        
        rvExpenses.setLayoutManager(new LinearLayoutManager(this));
        rvExpenses.setAdapter(transactionAdapter);
    }

    private void showViewExpenseBottomSheet(Expense expense) {
        com.google.android.material.bottomsheet.BottomSheetDialog dialog = new com.google.android.material.bottomsheet.BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_view_expense, null);
        dialog.setContentView(view);

        ImageView imgIcon = view.findViewById(R.id.imgDetailIcon);
        TextView tvName = view.findViewById(R.id.tvDetailName);
        TextView tvCategory = view.findViewById(R.id.tvDetailCategory);
        TextView tvDate = view.findViewById(R.id.tvDetailDate);
        TextView tvAmount = view.findViewById(R.id.tvDetailAmount);
        Button btnClose = view.findViewById(R.id.btnCloseDetail);

        ExpenseCategory cat = categoryMap.get(expense.getCategoryKey());
        String emoji = cat != null ? cat.getEmoji() : FoodIconConfig.DEFAULT_ICON_NAME;
        String label = cat != null ? cat.getLabel() : "Khác";

        imgIcon.setImageResource(FoodIconConfig.safeIcon(emoji));
        tvName.setText(expense.getName());
        tvCategory.setText(label);
        
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        tvDate.setText(sdf.format(new Date(expense.getExpenseDate())));
        
        tvAmount.setText(String.format(Locale.getDefault(), "-%,.0fđ", expense.getAmount()));

        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void setupPeriodAdapter() {
        periodAdapter = new PeriodAdapter(new ArrayList<>(), 0, periodItem -> {
            loadExpensesForPeriod(periodItem.startTime, periodItem.endTime);
        });
        rvMonths.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvMonths.setAdapter(periodAdapter);
    }

    private void updatePeriods() {
        List<PeriodAdapter.PeriodItem> periodItems = new ArrayList<>();
        int selectedIndex = 0;
        
        if (isWeeklyMode) {
            // Generate past 12 weeks
            Calendar cal = Calendar.getInstance();
            // Start of current week (Monday)
            int dow = cal.get(Calendar.DAY_OF_WEEK);
            int daysFromMon = (dow == Calendar.SUNDAY) ? 6 : dow - Calendar.MONDAY;
            cal.add(Calendar.DAY_OF_MONTH, -daysFromMon);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            
            SimpleDateFormat df = new SimpleDateFormat("dd/MM/yy", Locale.getDefault());
            for (int i = 0; i < 12; i++) {
                long weekStart = cal.getTimeInMillis();
                cal.add(Calendar.DAY_OF_MONTH, 7);
                long weekEnd = cal.getTimeInMillis();
                cal.add(Calendar.DAY_OF_MONTH, -7);

                String label;
                if (i == 0) {
                    label = "Tuần này";
                } else if (i == 1) {
                    label = "Tuần trước";
                } else {
                    Calendar endCal = Calendar.getInstance();
                    endCal.setTimeInMillis(weekEnd);
                    endCal.add(Calendar.DAY_OF_MONTH, -1); // Sunday
                    label = df.format(weekStart) + " - " + df.format(endCal.getTime());
                }
                
                periodItems.add(new PeriodAdapter.PeriodItem(label, weekStart, weekEnd));
                cal.add(Calendar.DAY_OF_MONTH, -7); // go back one week
            }
        } else {
            // Generate past 12 months
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.DAY_OF_MONTH, 1);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            
            SimpleDateFormat df = new SimpleDateFormat("MM/yyyy", Locale.getDefault());
            for (int i = 0; i < 12; i++) {
                long monthStart = cal.getTimeInMillis();
                cal.add(Calendar.MONTH, 1);
                long monthEnd = cal.getTimeInMillis();
                cal.add(Calendar.MONTH, -1);

                String label;
                if (i == 0) {
                    label = "Tháng này";
                } else if (i == 1) {
                    label = "Tháng trước";
                } else {
                    label = df.format(monthStart);
                }
                
                periodItems.add(new PeriodAdapter.PeriodItem(label, monthStart, monthEnd));
                cal.add(Calendar.MONTH, -1); // go back one month
            }
        }
        
        // Reverse so that the oldest is first, "this week/month" is at the end
        Collections.reverse(periodItems);
        selectedIndex = periodItems.size() - 1;

        periodAdapter.updateData(periodItems, selectedIndex);
        rvMonths.scrollToPosition(selectedIndex);

        if (!periodItems.isEmpty()) {
            PeriodAdapter.PeriodItem current = periodItems.get(selectedIndex);
            loadExpensesForPeriod(current.startTime, current.endTime);
        }
    }

    private void loadCategoriesAndInitData() {
        executorService.execute(() -> {
            List<ExpenseCategory> allCategories = expenseDao.getAllCategories();
            categoryMap.clear();
            for (ExpenseCategory cat : allCategories) {
                categoryMap.put(cat.getCategoryKey(), cat);
            }

            runOnUiThread(this::updatePeriods);
        });
    }

    private void loadExpensesForPeriod(long startTime, long endTime) {
        executorService.execute(() -> {
            List<Expense> expenses = expenseDao.getExpensesForPeriod(startTime, endTime);
            
            // Fetch chart data
            if (isWeeklyMode) {
                List<ExpenseDao.DailyStat> dailyStats = expenseDao.getDailySpentForWeek(startTime, endTime);
                runOnUiThread(() -> {
                    barChart.setVisibility(View.VISIBLE);
                    barChartMonthly.setVisibility(View.GONE);
                    tvChartTitle.setText("Chi tiêu hàng ngày");
                    updateBarChart(dailyStats, startTime);
                });
            } else {
                List<ExpenseDao.WeeklyStat> weeklyStats = expenseDao.getWeeklySpentForMonth(startTime, endTime);
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(startTime);
                int weeksInMonth = (int) Math.ceil(cal.getActualMaximum(Calendar.DAY_OF_MONTH) / 7.0);
                
                runOnUiThread(() -> {
                    barChart.setVisibility(View.GONE);
                    barChartMonthly.setVisibility(View.VISIBLE);
                    tvChartTitle.setText("Chi tiêu hàng tuần");
                    updateBarChartMonthly(weeklyStats, weeksInMonth, startTime);
                });
            }

            runOnUiThread(() -> {
                if (expenses == null || expenses.isEmpty()) {
                    transactionAdapter.setExpenses(new ArrayList<>());
                    rvExpenses.setVisibility(View.GONE);
                    tvNoData.setVisibility(View.VISIBLE);
                } else {
                    transactionAdapter.setExpenses(expenses);
                    rvExpenses.setVisibility(View.VISIBLE);
                    tvNoData.setVisibility(View.GONE);
                }
            });
        });
    }

    // ===================================================================
    // Chart Methods (Ported from BudgetFragment)
    // ===================================================================

    private void updateBarChart(List<ExpenseDao.DailyStat> dailyStats, long weekStart) {
        Map<String, Double> statMap = new HashMap<>();
        SimpleDateFormat dayFmt = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        if (dailyStats != null) {
            for (ExpenseDao.DailyStat stat : dailyStats) {
                statMap.put(dayFmt.format(stat.expense_date), stat.total);
            }
        }

        final String[] dayLabels = { "T2", "T3", "T4", "T5", "T6", "T7", "CN" };
        List<BarEntry> entries = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(weekStart);

        for (int i = 0; i < 7; i++) {
            String key = dayFmt.format(cal.getTime());
            double val = statMap.getOrDefault(key, 0.0);
            entries.add(new BarEntry(i, (float) val));
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }

        BarDataSet dataSet = new BarDataSet(entries, "");
        dataSet.setColor(Color.parseColor("#00BC7D"));
        dataSet.setDrawValues(false);

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.5f);
        barChart.setData(barData);

        XAxis xAxis = barChart.getXAxis();
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int idx = (int) value;
                return (idx >= 0 && idx < 7) ? dayLabels[idx] : "";
            }
        });
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setTextColor(Color.parseColor("#99A1AF"));
        xAxis.setTextSize(10f);
        xAxis.setAxisMinimum(-0.5f);
        xAxis.setAxisMaximum(6.5f);

        configureChartCommon(barChart);
        barChart.animateY(400);
        barChart.invalidate();
    }

    private void updateBarChartMonthly(List<ExpenseDao.WeeklyStat> weeklyStats, int weeksInMonth, long monthStart) {
        weeksInMonth = Math.max(4, Math.min(weeksInMonth, 5));
        Map<Integer, Double> statMap = new HashMap<>();
        if (weeklyStats != null) {
            for (ExpenseDao.WeeklyStat stat : weeklyStats) {
                statMap.put(stat.week_index, stat.total);
            }
        }

        final int finalWeeks = weeksInMonth;
        final String[] weekLabels = new String[finalWeeks];
        List<BarEntry> entries = new ArrayList<>();
        
        Calendar now = Calendar.getInstance();
        int currentWeekIdx = -1;
        // Only highlight if the selected month is the actual current month
        Calendar sel = Calendar.getInstance();
        sel.setTimeInMillis(monthStart);
        if (sel.get(Calendar.MONTH) == now.get(Calendar.MONTH) && sel.get(Calendar.YEAR) == now.get(Calendar.YEAR)) {
            currentWeekIdx = (int) ((now.getTimeInMillis() - monthStart) / 604800000);
            currentWeekIdx = Math.max(0, Math.min(currentWeekIdx, finalWeeks - 1));
        }

        for (int i = 0; i < finalWeeks; i++) {
            weekLabels[i] = "Tuần " + (i + 1);
            double val = statMap.getOrDefault(i, 0.0);
            entries.add(new BarEntry(i, (float) val));
        }

        BarDataSet dataSet = new BarDataSet(entries, "");
        dataSet.setDrawValues(false);

        // Màu sắc: Highlight tuần này nếu là tháng hiện tại
        int[] colors = new int[finalWeeks];
        for (int i = 0; i < finalWeeks; i++) {
            if (currentWeekIdx == -1) {
                colors[i] = Color.parseColor("#00BC7D"); // Pastel green for past months
            } else {
                colors[i] = (i == currentWeekIdx) ? Color.parseColor("#00BC7D") : Color.parseColor("#86EFAC");
            }
        }
        dataSet.setColors(colors);

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.7f); // Thicker bars for better visibility
        barChartMonthly.setData(barData);

        XAxis xAxis = barChartMonthly.getXAxis();
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int idx = (int) value;
                return (idx >= 0 && idx < finalWeeks) ? weekLabels[idx] : "";
            }
        });
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setTextColor(Color.parseColor("#99A1AF"));
        xAxis.setTextSize(10f);
        xAxis.setAxisMinimum(-0.5f);
        xAxis.setAxisMaximum(finalWeeks - 0.5f);

        configureChartCommon(barChartMonthly);
        barChartMonthly.animateY(600);
        barChartMonthly.invalidate();
    }

    private void configureChartCommon(BarChart chart) {
        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.getAxisRight().setEnabled(false);
        chart.getAxisLeft().setDrawGridLines(false);
        chart.getAxisLeft().setTextColor(Color.parseColor("#99A1AF"));
        chart.getAxisLeft().setTextSize(9f);
        chart.getAxisLeft().setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                if (value == 0) return "0";
                if (value >= 1_000_000) return (int) (value / 1_000_000) + "tr";
                if (value >= 1_000) return (int) (value / 1_000) + "k";
                return String.valueOf((int) value);
            }
        });
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.stay, R.anim.slide_out_bottom);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}
