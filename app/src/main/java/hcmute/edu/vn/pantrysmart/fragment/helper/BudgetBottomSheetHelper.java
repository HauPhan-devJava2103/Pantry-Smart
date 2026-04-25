package hcmute.edu.vn.pantrysmart.fragment.helper;

import android.app.DatePickerDialog;
import android.content.Context;
import android.graphics.Color;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.fragment.app.FragmentActivity;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.ExecutorService;

import hcmute.edu.vn.pantrysmart.R;
import hcmute.edu.vn.pantrysmart.data.local.dao.BudgetDao;
import hcmute.edu.vn.pantrysmart.data.local.dao.ExpenseDao;
import hcmute.edu.vn.pantrysmart.data.local.entity.Budget;
import hcmute.edu.vn.pantrysmart.data.local.entity.Expense;

public class BudgetBottomSheetHelper {

    private final Context context;
    private final FragmentActivity activity;
    private final BudgetDao budgetDao;
    private final ExpenseDao expenseDao;
    private final ExecutorService executorService;
    private final BudgetDialogHelper dialogHelper;
    private final Runnable onDataChanged;

    public BudgetBottomSheetHelper(Context context, FragmentActivity activity,
            BudgetDao budgetDao, ExpenseDao expenseDao, ExecutorService executorService,
            BudgetDialogHelper dialogHelper, Runnable onDataChanged) {
        this.context = context;
        this.activity = activity;
        this.budgetDao = budgetDao;
        this.expenseDao = expenseDao;
        this.executorService = executorService;
        this.dialogHelper = dialogHelper;
        this.onDataChanged = onDataChanged;
    }


    // UC9 — Thiết lập ngân sách tuần / tháng
    public void showSetBudgetBottomSheet(int currentMonth, int currentYear) {
        BottomSheetDialog dialog = new BottomSheetDialog(context);
        View sheetView = LayoutInflater.from(context)
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
            if (activity != null) {
                activity.runOnUiThread(() -> {
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
                if (activity != null) {
                    activity.runOnUiThread(() -> {
                        dialogHelper.showCustomToast("Đã lưu ngân sách!", R.drawable.ic_toast_check);
                        dialog.dismiss();
                        dialogHelper.setHasShownWarning(false); // Reset cảnh báo
                        if (onDataChanged != null) onDataChanged.run();
                    });
                }
            });
        });

        dialog.show();
    }

    // Thêm chi tiêu
    public void showAddExpenseBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(context);
        View sheetView = LayoutInflater.from(context)
                .inflate(R.layout.bottom_sheet_add_expense, null);
        dialog.setContentView(sheetView);

        EditText etExpenseName = sheetView.findViewById(R.id.etExpenseName);
        EditText etExpenseAmount = sheetView.findViewById(R.id.etExpenseAmount);
        TextView tvSelectDate = sheetView.findViewById(R.id.tvSelectDate);
        Button btnSaveExpense = sheetView.findViewById(R.id.btnSaveExpense);

        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        tvSelectDate.setText(dateFormat.format(calendar.getTime()));

        tvSelectDate.setOnClickListener(v -> new DatePickerDialog(context, (vw, year, month, day) -> {
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

                // Gắn budget_id theo tháng/năm của ngày expense, không phải tháng hiện tại
                int expMonth = calendar.get(Calendar.MONTH) + 1;
                int expYear = calendar.get(Calendar.YEAR);
                Budget targetBudget = budgetDao.getBudgetForMonth(expMonth, expYear);
                if (targetBudget != null)
                    expense.setBudgetId(targetBudget.getId());

                expenseDao.insertExpense(expense);

                if (activity != null) {
                    activity.runOnUiThread(() -> {
                        dialogHelper.showCustomToast("Đã thêm khoản chi!", R.drawable.ic_toast_check);
                        dialog.dismiss();
                        dialogHelper.setHasShownWarning(false);
                        if (onDataChanged != null) onDataChanged.run();
                    });
                }
            });
        });

        dialog.show();
    }

    // Sửa giao dịch — pre-fill bottom sheet với dữ liệu expense cũ
    public void showEditExpenseBottomSheet(Expense expense) {
        BottomSheetDialog dialog = new BottomSheetDialog(context);
        View sheetView = LayoutInflater.from(context)
                .inflate(R.layout.bottom_sheet_add_expense, null);
        dialog.setContentView(sheetView);

        // Override tiêu đề thành "Sửa chi tiêu"
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

        tvSelectDate.setOnClickListener(v -> new DatePickerDialog(context, (vw, year, month, day) -> {
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
                // Cập nhật trực tiếp vào đối tượng expense (giữ nguyên id)
                expense.setName(name);
                expense.setAmount(amount);
                expense.setCategoryKey(selectedCategory[0]);
                expense.setExpenseDate(calendar.getTimeInMillis());

                int expMonth = calendar.get(Calendar.MONTH) + 1;
                int expYear = calendar.get(Calendar.YEAR);
                Budget targetBudget = budgetDao.getBudgetForMonth(expMonth, expYear);
                expense.setBudgetId(targetBudget != null ? targetBudget.getId() : null);
                expenseDao.updateExpense(expense);

                if (activity != null) {
                    activity.runOnUiThread(() -> {
                        dialogHelper.showCustomToast("Đã cập nhật!", R.drawable.ic_toast_check);
                        dialog.dismiss();
                        dialogHelper.setHasShownWarning(false);
                        if (onDataChanged != null) onDataChanged.run();
                    });
                }
            });
        });

        dialog.show();
    }
}
