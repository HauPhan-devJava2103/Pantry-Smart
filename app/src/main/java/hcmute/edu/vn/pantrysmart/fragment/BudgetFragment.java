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
import hcmute.edu.vn.pantrysmart.data.local.PantrySmartDatabase;
import hcmute.edu.vn.pantrysmart.data.local.dao.BudgetDao;
import hcmute.edu.vn.pantrysmart.data.local.dao.ExpenseDao;
import hcmute.edu.vn.pantrysmart.data.local.entity.Budget;
import hcmute.edu.vn.pantrysmart.data.local.entity.Expense;
import hcmute.edu.vn.pantrysmart.data.local.entity.ExpenseCategory;

public class BudgetFragment extends Fragment {

    // ===== Views =====
    private TextView tvTotalBudget, tvTotalExpense, tvRemaining;
    private TextView tvSpentToday, tvTransactionCount;
    private ProgressBar progressBarMonthly;
    private FloatingActionButton fabAddExpense;
    private FrameLayout btnEditBudget;
    private RecyclerView rvRecentTransactions, rvCategoriesHorizontal;
    private BarChart barChart;
    private View rootView;

    // ===== Data =====
    private PantrySmartDatabase database;
    private BudgetDao budgetDao;
    private ExpenseDao expenseDao;
    private ExecutorService executorService;
    private int currentMonth, currentYear;

    // Category lookup map: key → ExpenseCategory
    private Map<String, ExpenseCategory> categoryMap = new HashMap<>();

    // Adapters
    private RecentTransactionAdapter transactionAdapter;
    private BudgetCategoryAdapter categoryAdapter;

    // Warning flag: tránh hiện nhiều cảnh báo cùng lúc trong 1 lần load
    private boolean hasShownWarning = false;

    // ===================================================================
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

        // --- Bind views ---
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

        // --- Init DB ---
        database = PantrySmartDatabase.getInstance(getContext());
        budgetDao = database.budgetDao();
        expenseDao = database.expenseDao();
        executorService = Executors.newSingleThreadExecutor();

        // --- Thời điểm hiện tại ---
        Calendar now = Calendar.getInstance();
        currentMonth = now.get(Calendar.MONTH) + 1;
        currentYear = now.get(Calendar.YEAR);

        // --- RecyclerView: Giao dịch gần đây ---
        transactionAdapter = new RecentTransactionAdapter(new ArrayList<>(),
                new RecentTransactionAdapter.CategoryEmojiResolver() {
                    @Override
                    public String getEmoji(String key) {
                        ExpenseCategory c = categoryMap.get(key);
                        return c != null ? c.getEmoji() : "💰";
                    }

                    @Override
                    public String getLabel(String key) {
                        ExpenseCategory c = categoryMap.get(key);
                        return c != null ? c.getLabel() : "Khác";
                    }
                });
        // Gắn callback xóa / sửa cho từng item giao dịch
        transactionAdapter.setOnActionListener(new RecentTransactionAdapter.OnActionListener() {
            @Override
            public void onEdit(Expense expense) {
                showEditExpenseBottomSheet(expense);
            }

            @Override
            public void onDelete(Expense expense) {
                showDeleteConfirmDialog(expense);
            }
        });

        rvRecentTransactions.setLayoutManager(new LinearLayoutManager(getContext()));
        rvRecentTransactions.setAdapter(transactionAdapter);

        // --- RecyclerView: Theo danh mục ---
        categoryAdapter = new BudgetCategoryAdapter(new ArrayList<>(), new ArrayList<>(), 0);
        rvCategoriesHorizontal.setLayoutManager(new LinearLayoutManager(getContext()));
        rvCategoriesHorizontal.setAdapter(categoryAdapter);

        // --- Click listeners ---
        fabAddExpense.setOnClickListener(v -> showAddExpenseBottomSheet());
        btnEditBudget.setOnClickListener(v -> showSetBudgetBottomSheet());

        // --- Load tất cả dữ liệu ---
        loadBudgetData();
    }

    // ===================================================================
    // UC8 — Tải và hiển thị thống kê chi tiêu (tháng, tuần, ngày)
    // ===================================================================
    private void loadBudgetData() {
        hasShownWarning = false;

        executorService.execute(() -> {
            Budget currentBudget = budgetDao.getBudgetForMonth(currentMonth, currentYear);

            // --- Bounds: tháng ---
            Calendar mCal = Calendar.getInstance();
            mCal.set(Calendar.DAY_OF_MONTH, 1);
            mCal.set(Calendar.HOUR_OF_DAY, 0);
            mCal.set(Calendar.MINUTE, 0);
            mCal.set(Calendar.SECOND, 0);
            mCal.set(Calendar.MILLISECOND, 0);
            long monthStart = mCal.getTimeInMillis();
            mCal.add(Calendar.MONTH, 1);
            long monthEnd = mCal.getTimeInMillis();

            // --- Bounds: tuần (Thứ Hai → Chủ Nhật) ---
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

            // --- Bounds: hôm nay ---
            Calendar dayCal = Calendar.getInstance();
            dayCal.set(Calendar.HOUR_OF_DAY, 0);
            dayCal.set(Calendar.MINUTE, 0);
            dayCal.set(Calendar.SECOND, 0);
            dayCal.set(Calendar.MILLISECOND, 0);
            long dayStart = dayCal.getTimeInMillis();
            dayCal.add(Calendar.DAY_OF_MONTH, 1);
            long dayEnd = dayCal.getTimeInMillis();

            // --- Query chi tiêu (timestamp-based, không JOIN) ---
            double totalMonth = expenseDao.getTotalSpentForPeriod(monthStart, monthEnd);
            double totalWeek = expenseDao.getTotalSpentForPeriod(weekStart, weekEnd);
            double spentToday = expenseDao.getTotalSpentForPeriod(dayStart, dayEnd);
            int txCount = expenseDao.getTransactionCountForMonth(monthStart, monthEnd);

            double monthlyLimit = (currentBudget != null) ? currentBudget.getMonthlyLimit() : 0;
            double weeklyLimit = (currentBudget != null) ? currentBudget.getWeeklyLimit() : 0;

            // --- Query giao dịch gần đây (10 gần nhất) ---
            List<Expense> recentExpenses = expenseDao.getRecentExpenses(10);

            // --- Query thống kê theo danh mục ---
            List<ExpenseDao.CategoryStat> categoryStats = expenseDao.getSpentByCategory(monthStart, monthEnd);

            // --- Query danh sách danh mục (để lấy emoji/label) ---
            List<ExpenseCategory> allCategories = expenseDao.getAllCategories();

            // --- Query chi tiêu 7 ngày cho BarChart ---
            List<ExpenseDao.DailyStat> dailyStats = expenseDao.getDailySpentForWeek(weekStart, weekEnd);

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
                    categoryAdapter.update(allCategories, categoryStats, totalMonth);

                    // BarChart: Chi tiêu tuần này
                    updateBarChart(dailyStats, weekStart);

                    // UC10 — Cảnh báo
                    checkBudgetWarnings(totalMonth, monthlyLimit, totalWeek, weeklyLimit);
                });
            }
        });
    }

    // ===================================================================
    // BarChart — Chi tiêu 7 ngày
    // ===================================================================
    private void updateBarChart(List<ExpenseDao.DailyStat> dailyStats, long weekStart) {
        // Tạo map ngày → tổng để tra cứu nhanh
        Map<String, Double> statMap = new HashMap<>();
        SimpleDateFormat dayFmt = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        if (dailyStats != null) {
            for (ExpenseDao.DailyStat stat : dailyStats) {
                statMap.put(dayFmt.format(stat.expense_date), stat.total);
            }
        }

        // Tạo 7 entries cho 7 ngày (Thứ 2 → CN)
        final String[] dayLabels = { "T2", "T3", "T4", "T5", "T6", "T7", "CN" };
        List<BarEntry> entries = new ArrayList<>();

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(weekStart);

        for (int i = 0; i < 7; i++) {
            String key = dayFmt.format(cal.getTime());
            double val = statMap.containsKey(key) ? statMap.get(key) : 0;
            entries.add(new BarEntry(i, (float) val));
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }

        BarDataSet dataSet = new BarDataSet(entries, "");
        dataSet.setColor(Color.parseColor("#00BC7D"));
        dataSet.setDrawValues(false); // Tắt nhãn giá trị trên cột

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.5f);
        barChart.setData(barData);

        // Cấu hình trục X
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

        // Ẩn các thành phần thừa
        barChart.getAxisLeft().setDrawGridLines(false);
        barChart.getAxisLeft().setTextColor(Color.parseColor("#99A1AF"));
        barChart.getAxisLeft().setTextSize(9f);
        barChart.getAxisLeft().setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                if (value == 0)
                    return "0";
                if (value >= 1_000_000)
                    return (int) (value / 1_000_000) + "tr";
                if (value >= 1_000)
                    return (int) (value / 1_000) + "k";
                return String.valueOf((int) value);
            }
        });
        barChart.getAxisRight().setEnabled(false);
        barChart.getDescription().setEnabled(false);
        barChart.getLegend().setEnabled(false);
        barChart.setTouchEnabled(false);
        barChart.setDrawGridBackground(false);
        barChart.setExtraBottomOffset(4f);
        barChart.animateY(400);
        barChart.invalidate();
    }

    // ===================================================================
    // Progress bar ngân sách tháng
    // ===================================================================
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

    // ===================================================================
    // UC10 — Cảnh báo vượt ngân sách
    // ===================================================================
    private void checkBudgetWarnings(double monthSpent, double monthLimit,
            double weekSpent, double weekLimit) {
        if (hasShownWarning)
            return;

        if (monthLimit > 0 && monthSpent >= monthLimit) {
            hasShownWarning = true;
            showOverBudgetAlert("🚨 Vượt ngân sách tháng!",
                    "Đã chi: " + formatCurrency(monthSpent)
                            + "\nGiới hạn: " + formatCurrency(monthLimit)
                            + "\n\nHãy kiểm soát chi tiêu ngay!");

        } else if (weekLimit > 0 && weekSpent >= weekLimit) {
            hasShownWarning = true;
            showOverBudgetAlert("⚠️ Vượt ngân sách tuần!",
                    "Chi tiêu tuần: " + formatCurrency(weekSpent)
                            + "\nGiới hạn tuần: " + formatCurrency(weekLimit));

        } else if (monthLimit > 0 && monthSpent >= monthLimit * 0.8) {
            hasShownWarning = true;
            showWarningSnackbar("⚡ Đã dùng "
                    + (int) ((monthSpent / monthLimit) * 100) + "% ngân sách tháng!", false);

        } else if (weekLimit > 0 && weekSpent >= weekLimit * 0.8) {
            hasShownWarning = true;
            showWarningSnackbar("⚡ Đã dùng "
                    + (int) ((weekSpent / weekLimit) * 100) + "% ngân sách tuần!", false);
        }
    }

    private void showOverBudgetAlert(String title, String message) {
        if (getContext() == null)
            return;
        new AlertDialog.Builder(getContext())
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Đã hiểu", null)
                .setNegativeButton("Điều chỉnh ngân sách", (d, w) -> showSetBudgetBottomSheet())
                .show();
    }

    private void showWarningSnackbar(String message, boolean isError) {
        if (rootView == null)
            return;
        int color = isError ? Color.parseColor("#C62828") : Color.parseColor("#F57C00");
        Snackbar sb = Snackbar.make(rootView, message, Snackbar.LENGTH_LONG);
        sb.getView().setBackgroundColor(color);
        sb.show();
    }

    // ===================================================================
    // UC9 — Thiết lập ngân sách tuần / tháng
    // ===================================================================
    private void showSetBudgetBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View sheetView = LayoutInflater.from(getContext())
                .inflate(R.layout.bottom_sheet_set_budget, null);
        dialog.setContentView(sheetView);

        EditText etMonthly = sheetView.findViewById(R.id.etMonthlyLimit);
        EditText etWeekly = sheetView.findViewById(R.id.etWeeklyLimit);
        Button btnSave = sheetView.findViewById(R.id.btnSaveBudget);
        TextView tvMonthLabel = sheetView.findViewById(R.id.tvBudgetMonthLabel);

        tvMonthLabel.setText("Tháng " + currentMonth + " / " + currentYear);

        // Quick chips tháng
        long[] mAmts = { 1_000_000, 2_000_000, 3_000_000, 5_000_000, 10_000_000 };
        int[] mIds = { R.id.mChip1tr, R.id.mChip2tr, R.id.mChip3tr, R.id.mChip5tr, R.id.mChip10tr };
        for (int i = 0; i < mIds.length; i++) {
            final long val = mAmts[i];
            sheetView.findViewById(mIds[i]).setOnClickListener(v -> etMonthly.setText(String.valueOf(val)));
        }

        // Quick chips tuần
        long[] wAmts = { 200_000, 300_000, 500_000, 750_000, 1_000_000 };
        int[] wIds = { R.id.wChip200k, R.id.wChip300k, R.id.wChip500k, R.id.wChip750k, R.id.wChip1tr };
        for (int i = 0; i < wIds.length; i++) {
            final long val = wAmts[i];
            sheetView.findViewById(wIds[i]).setOnClickListener(v -> etWeekly.setText(String.valueOf(val)));
        }

        // Pre-populate
        executorService.execute(() -> {
            Budget existing = budgetDao.getBudgetForMonth(currentMonth, currentYear);
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (existing != null) {
                        if (existing.getMonthlyLimit() > 0)
                            etMonthly.setText(String.valueOf((long) existing.getMonthlyLimit()));
                        if (existing.getWeeklyLimit() > 0)
                            etWeekly.setText(String.valueOf((long) existing.getWeeklyLimit()));
                    }
                });
            }
        });

        btnSave.setOnClickListener(v -> {
            String mStr = etMonthly.getText().toString().trim();
            String wStr = etWeekly.getText().toString().trim();

            if (TextUtils.isEmpty(mStr)) {
                etMonthly.setError("Vui lòng nhập ngân sách tháng");
                return;
            }
            double monthlyLimit;
            try {
                monthlyLimit = Double.parseDouble(mStr);
                if (monthlyLimit <= 0)
                    throw new NumberFormatException();
            } catch (NumberFormatException e) {
                etMonthly.setError("Số tiền không hợp lệ");
                return;
            }

            double weeklyLimit = 0;
            if (!TextUtils.isEmpty(wStr)) {
                try {
                    weeklyLimit = Double.parseDouble(wStr);
                    if (weeklyLimit < 0)
                        throw new NumberFormatException();
                } catch (NumberFormatException e) {
                    etWeekly.setError("Số tiền không hợp lệ");
                    return;
                }
                if (weeklyLimit > monthlyLimit) {
                    etWeekly.setError("Không được lớn hơn ngân sách tháng");
                    return;
                }
            }

            btnSave.setEnabled(false);
            btnSave.setText("Đang lưu...");
            final double finalWeekly = weeklyLimit;

            executorService.execute(() -> {
                Budget existing = budgetDao.getBudgetForMonth(currentMonth, currentYear);
                if (existing != null) {
                    existing.setMonthlyLimit(monthlyLimit);
                    existing.setWeeklyLimit(finalWeekly);
                    budgetDao.update(existing);
                } else {
                    Budget b = new Budget();
                    b.setMonth(currentMonth);
                    b.setYear(currentYear);
                    b.setMonthlyLimit(monthlyLimit);
                    b.setWeeklyLimit(finalWeekly);
                    budgetDao.insert(b);
                }
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "✅ Đã lưu ngân sách!", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        loadBudgetData();
                    });
                }
            });
        });

        dialog.show();
    }

    // ===================================================================
    // Thêm chi tiêu
    // ===================================================================
    private void showAddExpenseBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View sheetView = LayoutInflater.from(getContext())
                .inflate(R.layout.bottom_sheet_add_expense, null);
        dialog.setContentView(sheetView);

        EditText etExpenseName = sheetView.findViewById(R.id.etExpenseName);
        EditText etExpenseAmount = sheetView.findViewById(R.id.etExpenseAmount);
        TextView tvSelectDate = sheetView.findViewById(R.id.tvSelectDate);
        Button btnSaveExpense = sheetView.findViewById(R.id.btnSaveExpense);

        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        tvSelectDate.setText(dateFormat.format(calendar.getTime()));

        tvSelectDate.setOnClickListener(v -> new DatePickerDialog(requireContext(), (vw, year, month, day) -> {
            calendar.set(year, month, day);
            tvSelectDate.setText(dateFormat.format(calendar.getTime()));
        }, calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)).show());

        int[] chipIds = { R.id.chip20k, R.id.chip50k, R.id.chip100k, R.id.chip150k, R.id.chip200k, R.id.chip300k };
        int[] chipAmounts = { 20000, 50000, 100000, 150000, 200000, 300000 };
        for (int i = 0; i < chipIds.length; i++) {
            final int amount = chipAmounts[i];
            sheetView.findViewById(chipIds[i])
                    .setOnClickListener(v -> etExpenseAmount.setText(String.valueOf(amount)));
        }

        final String[] selectedCategory = { "SHOPPING" };
        LinearLayout btnShopping = sheetView.findViewById(R.id.btnCatShopping);
        LinearLayout btnDelivery = sheetView.findViewById(R.id.btnCatDelivery);
        LinearLayout btnSnacks = sheetView.findViewById(R.id.btnCatSnacks);
        LinearLayout btnOthers = sheetView.findViewById(R.id.btnCatOthers);
        LinearLayout[] catBtns = { btnShopping, btnDelivery, btnSnacks, btnOthers };
        String[] catKeys = { "SHOPPING", "DELIVERY", "SNACK", "OTHER" };

        View.OnClickListener catClick = v -> {
            for (int i = 0; i < catBtns.length; i++) {
                LinearLayout btn = catBtns[i];
                TextView txt = (TextView) btn.getChildAt(1);
                if (btn == v) {
                    selectedCategory[0] = catKeys[i];
                    btn.setBackgroundResource(R.drawable.bg_category_selected);
                    txt.setTextColor(Color.parseColor("#00BC7D"));
                } else {
                    btn.setBackgroundResource(R.drawable.bg_category_unselected);
                    txt.setTextColor(Color.parseColor("#4A5565"));
                }
            }
        };
        for (LinearLayout b : catBtns)
            b.setOnClickListener(catClick);

        btnSaveExpense.setOnClickListener(v -> {
            String name = etExpenseName.getText().toString().trim();
            String amountStr = etExpenseAmount.getText().toString().trim();

            if (TextUtils.isEmpty(name)) {
                etExpenseName.setError("Vui lòng nhập tên chi tiêu");
                return;
            }
            if (TextUtils.isEmpty(amountStr)) {
                etExpenseAmount.setError("Vui lòng nhập số tiền");
                return;
            }
            double amount;
            try {
                amount = Double.parseDouble(amountStr);
                if (amount <= 0)
                    throw new NumberFormatException();
            } catch (NumberFormatException e) {
                etExpenseAmount.setError("Số tiền không hợp lệ");
                return;
            }

            btnSaveExpense.setEnabled(false);
            btnSaveExpense.setText("Đang lưu...");

            executorService.execute(() -> {
                Expense expense = new Expense();
                expense.setName(name);
                expense.setAmount(amount);
                expense.setCategoryKey(selectedCategory[0]);
                expense.setExpenseDate(calendar.getTimeInMillis());

                Budget currentBudget = budgetDao.getBudgetForMonth(currentMonth, currentYear);
                if (currentBudget != null)
                    expense.setBudgetId(currentBudget.getId());

                expenseDao.insertExpense(expense);

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "✅ Đã thêm khoản chi!", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        loadBudgetData();
                    });
                }
            });
        });

        dialog.show();
    }

    // ===================================================================
    // Sửa giao dịch — pre-fill bottom sheet với dữ liệu expense cũ
    // ===================================================================
    private void showEditExpenseBottomSheet(Expense expense) {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View sheetView = LayoutInflater.from(getContext())
                .inflate(R.layout.bottom_sheet_add_expense, null);
        dialog.setContentView(sheetView);

        // Override tiêu đề thành "Sửa chi tiêu"
        TextView tvTitle = sheetView.findViewWithTag("titleText");
        // Tìm TextView đầu tiên là tiêu đề (layout_marginStart=20dp, text=Thêm chi
        // tiêu)
        // Dùng cách trực tiếp tìm qua thứ tự view — thay text thủ công
        LinearLayout root = (LinearLayout) sheetView;
        if (root.getChildCount() >= 2 && root.getChildAt(1) instanceof TextView) {
            ((TextView) root.getChildAt(1)).setText("Sửa chi tiêu");
        }

        EditText etExpenseName = sheetView.findViewById(R.id.etExpenseName);
        EditText etExpenseAmount = sheetView.findViewById(R.id.etExpenseAmount);
        TextView tvSelectDate = sheetView.findViewById(R.id.tvSelectDate);
        Button btnSave = sheetView.findViewById(R.id.btnSaveExpense);

        // Pre-fill dữ liệu cũ
        etExpenseName.setText(expense.getName());
        etExpenseAmount.setText(String.valueOf((long) expense.getAmount()));
        btnSave.setText("Lưu thay đổi");

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(expense.getExpenseDate());
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        tvSelectDate.setText(dateFormat.format(calendar.getTime()));

        tvSelectDate.setOnClickListener(v -> new DatePickerDialog(requireContext(), (vw, year, month, day) -> {
            calendar.set(year, month, day);
            tvSelectDate.setText(dateFormat.format(calendar.getTime()));
        }, calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)).show());

        // Quick chips số tiền
        int[] chipIds = { R.id.chip20k, R.id.chip50k, R.id.chip100k, R.id.chip150k, R.id.chip200k, R.id.chip300k };
        int[] chipAmounts = { 20000, 50000, 100000, 150000, 200000, 300000 };
        for (int i = 0; i < chipIds.length; i++) {
            final int amount = chipAmounts[i];
            sheetView.findViewById(chipIds[i])
                    .setOnClickListener(v -> etExpenseAmount.setText(String.valueOf(amount)));
        }

        // Chọn danh mục — pre-select danh mục cũ
        final String[] selectedCategory = { expense.getCategoryKey() };
        LinearLayout btnShopping = sheetView.findViewById(R.id.btnCatShopping);
        LinearLayout btnDelivery = sheetView.findViewById(R.id.btnCatDelivery);
        LinearLayout btnSnacks = sheetView.findViewById(R.id.btnCatSnacks);
        LinearLayout btnOthers = sheetView.findViewById(R.id.btnCatOthers);
        LinearLayout[] catBtns = { btnShopping, btnDelivery, btnSnacks, btnOthers };
        String[] catKeys = { "SHOPPING", "DELIVERY", "SNACK", "OTHER" };

        // Áp dụng trạng thái ban đầu dựa trên danh mục của expense cũ
        for (int i = 0; i < catBtns.length; i++) {
            LinearLayout btn = catBtns[i];
            TextView txt = (TextView) btn.getChildAt(1);
            if (catKeys[i].equals(expense.getCategoryKey())) {
                btn.setBackgroundResource(R.drawable.bg_category_selected);
                txt.setTextColor(Color.parseColor("#00BC7D"));
            } else {
                btn.setBackgroundResource(R.drawable.bg_category_unselected);
                txt.setTextColor(Color.parseColor("#4A5565"));
            }
        }

        View.OnClickListener catClick = v -> {
            for (int i = 0; i < catBtns.length; i++) {
                LinearLayout btn = catBtns[i];
                TextView txt = (TextView) btn.getChildAt(1);
                if (btn == v) {
                    selectedCategory[0] = catKeys[i];
                    btn.setBackgroundResource(R.drawable.bg_category_selected);
                    txt.setTextColor(Color.parseColor("#00BC7D"));
                } else {
                    btn.setBackgroundResource(R.drawable.bg_category_unselected);
                    txt.setTextColor(Color.parseColor("#4A5565"));
                }
            }
        };
        for (LinearLayout b : catBtns)
            b.setOnClickListener(catClick);

        // Lưu thay đổi
        btnSave.setOnClickListener(v -> {
            String name = etExpenseName.getText().toString().trim();
            String amountStr = etExpenseAmount.getText().toString().trim();

            if (TextUtils.isEmpty(name)) {
                etExpenseName.setError("Vui lòng nhập tên chi tiêu");
                return;
            }
            if (TextUtils.isEmpty(amountStr)) {
                etExpenseAmount.setError("Vui lòng nhập số tiền");
                return;
            }
            double amount;
            try {
                amount = Double.parseDouble(amountStr);
                if (amount <= 0)
                    throw new NumberFormatException();
            } catch (NumberFormatException e) {
                etExpenseAmount.setError("Số tiền không hợp lệ");
                return;
            }

            btnSave.setEnabled(false);
            btnSave.setText("Đang lưu...");

            executorService.execute(() -> {
                // Cập nhật trực tiếp vào đối tượng expense (giữ nguyên id, budget_id)
                expense.setName(name);
                expense.setAmount(amount);
                expense.setCategoryKey(selectedCategory[0]);
                expense.setExpenseDate(calendar.getTimeInMillis());
                expenseDao.updateExpense(expense);

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "✅ Đã cập nhật!", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        loadBudgetData();
                    });
                }
            });
        });

        dialog.show();
    }

    // ===================================================================
    // Xóa giao dịch — xác nhận trước khi xóa
    // ===================================================================
    private void showDeleteConfirmDialog(Expense expense) {
        if (getContext() == null)
            return;
        new AlertDialog.Builder(getContext())
                .setTitle("Xóa giao dịch")
                .setMessage("Bạn có chắc muốn xóa khoản chi\n\""
                        + expense.getName() + "\" — "
                        + formatCurrency(expense.getAmount()) + " không?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    executorService.execute(() -> {
                        expenseDao.deleteExpense(expense);
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                Toast.makeText(getContext(),
                                        "🗑️ Đã xóa giao dịch", Toast.LENGTH_SHORT).show();
                                loadBudgetData();
                            });
                        }
                    });
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    // ===================================================================
    // Helpers
    // ===================================================================
    private String formatCurrency(double amount) {
        return String.format(Locale.getDefault(), "%,.0f đ", amount);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}