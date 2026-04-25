package hcmute.edu.vn.pantrysmart.fragment;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import hcmute.edu.vn.pantrysmart.R;
import hcmute.edu.vn.pantrysmart.adapter.BudgetCategoryAdapter;
import hcmute.edu.vn.pantrysmart.adapter.RecentTransactionAdapter;
import hcmute.edu.vn.pantrysmart.config.FoodIconConfig;
import hcmute.edu.vn.pantrysmart.data.local.PantrySmartDatabase;
import hcmute.edu.vn.pantrysmart.data.local.dao.BudgetDao;
import hcmute.edu.vn.pantrysmart.data.local.dao.ExpenseDao;
import hcmute.edu.vn.pantrysmart.data.local.dao.PantryItemDao;
import hcmute.edu.vn.pantrysmart.data.local.entity.Budget;
import hcmute.edu.vn.pantrysmart.data.local.entity.Expense;
import hcmute.edu.vn.pantrysmart.data.local.entity.ExpenseCategory;
import hcmute.edu.vn.pantrysmart.fragment.helper.BudgetBottomSheetHelper;
import hcmute.edu.vn.pantrysmart.fragment.helper.BudgetChartHelper;
import hcmute.edu.vn.pantrysmart.fragment.helper.BudgetDialogHelper;

public class BudgetFragment extends Fragment {

    // ===== Views =====
    private TextView tvTotalBudget, tvTotalExpense, tvRemaining;
    private TextView tvSpentToday, tvTransactionCount;
    private ProgressBar progressBarMonthly;
    private FloatingActionButton fabAddExpense;
    private FrameLayout btnEditBudget;
    private RecyclerView rvRecentTransactions, rvCategoriesHorizontal;
    private BarChart barChart, barChartMonthly;
    private TextView tvChartTitle;
    private TextView btnViewAllTransactions;
    private View rootView;

    // Tab switcher
    private TextView tvTabWeek, tvTabMonth;
    private boolean isWeeklyMode = false; // Mặc định: Tháng

    // ===== Data =====
    private PantrySmartDatabase database;
    private BudgetDao budgetDao;
    private ExpenseDao expenseDao;
    private PantryItemDao pantryItemDao;
    private ExecutorService executorService;
    private int currentMonth, currentYear;

    // Category lookup map: key → ExpenseCategory
    private Map<String, ExpenseCategory> categoryMap = new HashMap<>();

    // Adapters
    private RecentTransactionAdapter transactionAdapter;
    private BudgetCategoryAdapter categoryAdapter;

    // Helpers
    private BudgetChartHelper chartHelper;
    private BudgetDialogHelper dialogHelper;
    private BudgetBottomSheetHelper bottomSheetHelper;

    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_budget, container, false);
        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Bug #7: Khôi phục trạng thái tab sau khi xoay màn hình
        if (savedInstanceState != null) {
            isWeeklyMode = savedInstanceState.getBoolean("IS_WEEKLY_MODE", false);
        }

        //  Bind views 
        tvTotalBudget = view.findViewById(R.id.tvTotalBudget);
        tvTotalExpense = view.findViewById(R.id.tvTotalExpense);
        tvRemaining = view.findViewById(R.id.tvRemaining);
        progressBarMonthly = view.findViewById(R.id.progressBarMonthly);
        tvSpentToday = view.findViewById(R.id.tvSpentToday);
        tvTransactionCount = view.findViewById(R.id.tvTransactionCount);
        fabAddExpense = view.findViewById(R.id.fabAddExpense);
        btnEditBudget = view.findViewById(R.id.btnEditBudget);
        rvRecentTransactions = view.findViewById(R.id.rvRecentTransactions);
        rvCategoriesHorizontal = view.findViewById(R.id.rvCategoriesHorizontal);
        barChart = view.findViewById(R.id.barChart);
        barChartMonthly = view.findViewById(R.id.barChartMonthly);
        tvChartTitle = view.findViewById(R.id.tvChartTitle);
        btnViewAllTransactions = view.findViewById(R.id.btnViewAllTransactions);

        tvTabWeek = view.findViewById(R.id.tvTabWeek);
        tvTabMonth = view.findViewById(R.id.tvTabMonth);

        //  Init DB 
        database = PantrySmartDatabase.getInstance(getContext());
        budgetDao = database.budgetDao();
        expenseDao = database.expenseDao();
        pantryItemDao = database.pantryItemDao();
        if (executorService == null || executorService.isShutdown()) {
            executorService = Executors.newSingleThreadExecutor();
        }

        //  Lấy thời điểm hiện tại để truyền cho helper 
        Calendar now = Calendar.getInstance();
        currentMonth = now.get(Calendar.MONTH) + 1;
        currentYear = now.get(Calendar.YEAR);

        //  Init Helpers 
        chartHelper = new BudgetChartHelper(barChart, barChartMonthly);
        dialogHelper = new BudgetDialogHelper(getContext(), getActivity(), rootView, pantryItemDao, expenseDao, executorService,
                () -> loadBudgetData(),
                () -> { if (bottomSheetHelper != null) bottomSheetHelper.showSetBudgetBottomSheet(currentMonth, currentYear); });
        bottomSheetHelper = new BudgetBottomSheetHelper(getContext(), getActivity(), budgetDao, expenseDao, executorService, dialogHelper, () -> loadBudgetData());



        //  RecyclerView: Giao dich gan day 
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
                        return c != null ? c.getLabel() : "Kh\u00E1c";
                    }
                });
        // Gắn callback xóa / sửa cho từng item giao dịch
        transactionAdapter.setOnActionListener(new RecentTransactionAdapter.OnActionListener() {
            @Override
            public void onEdit(Expense expense) {
                bottomSheetHelper.showEditExpenseBottomSheet(expense);
            }

            @Override
            public void onDelete(Expense expense) {
                dialogHelper.showDeleteConfirmDialog(expense);
            }

            @Override
            public void onClick(Expense expense) {
                // Do nothing or show detail in BudgetFragment
            }
        });

        rvRecentTransactions.setLayoutManager(new LinearLayoutManager(getContext()));
        rvRecentTransactions.setAdapter(transactionAdapter);

        //  RecyclerView: Theo danh mục 
        categoryAdapter = new BudgetCategoryAdapter(new ArrayList<>(), new ArrayList<>(), 0);
        rvCategoriesHorizontal.setLayoutManager(new LinearLayoutManager(getContext()));
        rvCategoriesHorizontal.setAdapter(categoryAdapter);

        //  Click listeners 
        fabAddExpense.setOnClickListener(v -> bottomSheetHelper.showAddExpenseBottomSheet());
        btnEditBudget.setOnClickListener(v -> bottomSheetHelper.showSetBudgetBottomSheet(currentMonth, currentYear));
        btnViewAllTransactions.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(getContext(),
                    hcmute.edu.vn.pantrysmart.activity.ExpenseDetailActivity.class);
            intent.putExtra("IS_WEEKLY_MODE", isWeeklyMode);
            startActivity(intent);
            if (getActivity() != null) {
                getActivity().overridePendingTransition(R.anim.slide_in_bottom, R.anim.stay);
            }
        });

        //  Tab Switcher 
        setupTabSwitcher();

        //  Load tất cả dữ liệu 
        loadBudgetData();
    }

    
    // Tab Switcher
    private void setupTabSwitcher() {
        tvTabWeek.setOnClickListener(v -> {
            if (!isWeeklyMode) {
                isWeeklyMode = true;
                updateTabUI();
                loadBudgetData();
            }
        });

        tvTabMonth.setOnClickListener(v -> {
            if (isWeeklyMode) {
                isWeeklyMode = false;
                updateTabUI();
                loadBudgetData();
            }
        });

        updateTabUI();
    }

    private void updateTabUI() {
        if (isWeeklyMode) {
            tvTabWeek.setBackgroundResource(R.drawable.bg_tab_switcher_selected);
            tvTabWeek.setTextColor(Color.parseColor("#1E2939"));
            tvTabWeek.setElevation(4f);
            tvTabMonth.setBackgroundResource(R.drawable.bg_tab_switcher_unselected);
            tvTabMonth.setTextColor(Color.parseColor("#99A1AF"));
            tvTabMonth.setElevation(0f);

            tvChartTitle.setText("Chi tiêu tuần này");
        } else {
            tvTabMonth.setBackgroundResource(R.drawable.bg_tab_switcher_selected);
            tvTabMonth.setTextColor(Color.parseColor("#1E2939"));
            tvTabMonth.setElevation(4f);
            tvTabWeek.setBackgroundResource(R.drawable.bg_tab_switcher_unselected);
            tvTabWeek.setTextColor(Color.parseColor("#99A1AF"));
            tvTabWeek.setElevation(0f);

            tvChartTitle.setText("Chi tiêu tháng này");
        }
    }

    
    // UC8 — Tải và hiển thị thống kê chi tiêu (tháng, tuần, ngày)
    private void loadBudgetData() {
        executorService.execute(() -> {
            Budget currentBudget = budgetDao.getBudgetForMonth(currentMonth, currentYear);

            //  Bounds: tháng 
            Calendar mCal = Calendar.getInstance();
            mCal.set(Calendar.DAY_OF_MONTH, 1);
            mCal.set(Calendar.HOUR_OF_DAY, 0);
            mCal.set(Calendar.MINUTE, 0);
            mCal.set(Calendar.SECOND, 0);
            mCal.set(Calendar.MILLISECOND, 0);
            long monthStart = mCal.getTimeInMillis();
            mCal.add(Calendar.MONTH, 1);
            long monthEnd = mCal.getTimeInMillis();

            //  Bounds: tuần (Thứ Hai -> Chủ Nhật) 
            Calendar wCal = Calendar.getInstance();
            int dow = wCal.get(Calendar.DAY_OF_WEEK);
            int daysFromMon = (dow == Calendar.SUNDAY) ? 6 : dow - Calendar.MONDAY;
            wCal.add(Calendar.DAY_OF_MONTH, -daysFromMon);
            wCal.set(Calendar.HOUR_OF_DAY, 0);
            wCal.set(Calendar.MINUTE, 0);
            wCal.set(Calendar.SECOND, 0);
            wCal.set(Calendar.MILLISECOND, 0);
            long weekStart = wCal.getTimeInMillis();
            wCal.add(Calendar.DAY_OF_MONTH, 7);
            long weekEnd = wCal.getTimeInMillis();

            //  Bounds: hôm nay 
            Calendar dayCal = Calendar.getInstance();
            dayCal.set(Calendar.HOUR_OF_DAY, 0);
            dayCal.set(Calendar.MINUTE, 0);
            dayCal.set(Calendar.SECOND, 0);
            dayCal.set(Calendar.MILLISECOND, 0);
            long dayStart = dayCal.getTimeInMillis();
            dayCal.add(Calendar.DAY_OF_MONTH, 1);
            long dayEnd = dayCal.getTimeInMillis();

            // Xác định period hiện tại để lọc giao dịch / danh mục
            long periodStart = isWeeklyMode ? weekStart : monthStart;
            long periodEnd = isWeeklyMode ? weekEnd : monthEnd;

            // Query chi tiêu (timestamp-based, không JOIN)
            double totalMonth = expenseDao.getTotalSpentForPeriod(monthStart, monthEnd);
            double totalWeek = expenseDao.getTotalSpentForPeriod(weekStart, weekEnd);
            double spentToday = expenseDao.getTotalSpentForPeriod(dayStart, dayEnd);
            double totalPeriod = isWeeklyMode ? totalWeek : totalMonth;

            int txCount = expenseDao.getTransactionCountForPeriod(periodStart, periodEnd);

            double monthlyLimit = (currentBudget != null) ? currentBudget.getMonthlyLimit() : 0;
            double weeklyLimit = (currentBudget != null) ? currentBudget.getWeeklyLimit() : 0;

            // Query giao dịch gần đây
            List<Expense> recentExpenses;
            if (isWeeklyMode) {
                recentExpenses = expenseDao.getExpensesForPeriod(weekStart, weekEnd);
            } else {
                recentExpenses = expenseDao.getRecentExpenses(10);
            }

            // Query thống kê theo danh mục
            List<ExpenseDao.CategoryStat> categoryStats = expenseDao.getSpentByCategory(periodStart, periodEnd);

            // Query danh sách danh mục (để lấy emoji/label)
            List<ExpenseCategory> allCategories = expenseDao.getAllCategories();

            // Query biểu đồ
            List<ExpenseDao.DailyStat> dailyStats = expenseDao.getDailySpentForWeek(weekStart, weekEnd);
            List<ExpenseDao.WeeklyStat> weeklyStats = expenseDao.getWeeklySpentForMonth(monthStart, monthEnd);

            // Tính số tuần trong tháng
            Calendar tmpCal = Calendar.getInstance();
            tmpCal.setTimeInMillis(monthStart);
            int maxDay = tmpCal.getActualMaximum(Calendar.DAY_OF_MONTH);
            int weeksInMonth = (int) Math.ceil(maxDay / 7.0);

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    // Cập nhật category map cho adapter
                    categoryMap.clear();
                    for (ExpenseCategory cat : allCategories) {
                        categoryMap.put(cat.getCategoryKey(), cat);
                    }

                    // Card: Ngân sách tháng
                    tvTotalBudget.setText(monthlyLimit > 0
                            ? formatCurrency(monthlyLimit)
                            : "Chưa đặt");
                    updateProgressUI(totalMonth, monthlyLimit);

                    // Card: Hôm nay + Giao dịch
                    tvSpentToday.setText(formatCurrency(spentToday));
                    tvTransactionCount.setText(txCount + " lần");

                    // RecyclerView: Giao dịch gần đây
                    transactionAdapter.setExpenses(recentExpenses);

                    // RecyclerView: Theo danh mục
                    categoryAdapter.update(allCategories, categoryStats, totalPeriod);

                    // Charts: Toggle visibility và update
                    if (isWeeklyMode) {
                        barChart.setVisibility(View.VISIBLE);
                        barChartMonthly.setVisibility(View.GONE);
                        chartHelper.updateBarChart(dailyStats, weekStart);
                    } else {
                        barChart.setVisibility(View.GONE);
                        barChartMonthly.setVisibility(View.VISIBLE);
                        chartHelper.updateBarChartMonthly(weeklyStats, weeksInMonth);
                    }

                    // UC10 — Cảnh báo
                    dialogHelper.checkBudgetWarnings(totalMonth, monthlyLimit, totalWeek, weeklyLimit);
                });
            }
        });
    }



    // Progress bar ngân sách tháng
    private void updateProgressUI(double totalSpent, double limit) {
        tvTotalExpense.setText(formatCurrency(totalSpent));

        if (limit > 0) {
            double remaining = limit - totalSpent;
            tvRemaining.setText(formatCurrency(remaining));
            int progress = (int) ((totalSpent / limit) * 100);
            progressBarMonthly.setProgress(Math.min(progress, 100));

            if (totalSpent >= limit) {
                progressBarMonthly.setProgressTintList(
                        ColorStateList.valueOf(Color.parseColor("#C62828")));
                tvRemaining.setTextColor(Color.parseColor("#FF6B6B"));
            } else if (totalSpent >= limit * 0.8) {
                progressBarMonthly.setProgressTintList(
                        ColorStateList.valueOf(Color.parseColor("#F57C00")));
                tvRemaining.setTextColor(Color.WHITE);
            } else {
                progressBarMonthly.setProgressTintList(
                        ColorStateList.valueOf(Color.parseColor("#4CAF50")));
                tvRemaining.setTextColor(Color.WHITE);
            }
        } else {
            tvRemaining.setText("—");
            progressBarMonthly.setProgress(0);
        }
    }

    private String formatCurrency(double amount) {
        return String.format(Locale.getDefault(), "%,.0f đ", amount);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("IS_WEEKLY_MODE", isWeeklyMode);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}