package hcmute.edu.vn.pantrysmart.fragment.helper;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.FragmentActivity;

import com.google.android.material.snackbar.Snackbar;

import java.util.Locale;
import java.util.concurrent.ExecutorService;

import hcmute.edu.vn.pantrysmart.R;
import hcmute.edu.vn.pantrysmart.data.local.dao.ExpenseDao;
import hcmute.edu.vn.pantrysmart.data.local.dao.PantryItemDao;
import hcmute.edu.vn.pantrysmart.data.local.entity.Expense;

public class BudgetDialogHelper {

    private final Context context;
    private final FragmentActivity activity;
    private final View rootView;
    private final PantryItemDao pantryItemDao;
    private final ExpenseDao expenseDao;
    private final ExecutorService executorService;
    private final Runnable onDataChanged;
    private final Runnable onEditBudgetClicked;

    private boolean hasShownWarning = false;

    public BudgetDialogHelper(Context context, FragmentActivity activity, View rootView,
            PantryItemDao pantryItemDao, ExpenseDao expenseDao, ExecutorService executorService,
            Runnable onDataChanged, Runnable onEditBudgetClicked) {
        this.context = context;
        this.activity = activity;
        this.rootView = rootView;
        this.pantryItemDao = pantryItemDao;
        this.expenseDao = expenseDao;
        this.executorService = executorService;
        this.onDataChanged = onDataChanged;
        this.onEditBudgetClicked = onEditBudgetClicked;
    }

    public void setHasShownWarning(boolean hasShownWarning) {
        this.hasShownWarning = hasShownWarning;
    }

    // Cảnh báo vượt ngân sách
    public void checkBudgetWarnings(double monthSpent, double monthLimit,
            double weekSpent, double weekLimit) {
        if (hasShownWarning)
            return;

        if (monthLimit > 0 && monthSpent >= monthLimit) {
            hasShownWarning = true;
            showOverBudgetAlert("Vượt ngân sách tháng!",
                    "Đã chi: " + formatCurrency(monthSpent)
                            + "\nGiới hạn: " + formatCurrency(monthLimit)
                            + "\n\nHãy kiểm soát chi tiêu ngay!",
                    R.drawable.ic_budget_danger);

        } else if (weekLimit > 0 && weekSpent >= weekLimit) {
            hasShownWarning = true;
            showOverBudgetAlert("Vượt ngân sách tuần!",
                    "Chi tiêu tuần: " + formatCurrency(weekSpent)
                            + "\nGiới hạn tuần: " + formatCurrency(weekLimit),
                    R.drawable.ic_budget_warning);

        } else if (monthLimit > 0 && monthSpent >= monthLimit * 0.8) {
            hasShownWarning = true;
            showWarningSnackbar("Đã dùng "
                    + (int) ((monthSpent / monthLimit) * 100) + "% ngân sách tháng!", false);

        } else if (weekLimit > 0 && weekSpent >= weekLimit * 0.8) {
            hasShownWarning = true;
            showWarningSnackbar("Đã dùng "
                    + (int) ((weekSpent / weekLimit) * 100) + "% ngân sách tuần!", false);
        }
    }

    private void showOverBudgetAlert(String title, String message, int iconResId) {
        if (context == null)
            return;
        View dialogView = LayoutInflater.from(context)
                .inflate(R.layout.dialog_budget_alert, null);

        ((ImageView) dialogView.findViewById(R.id.imgDialogIcon)).setImageResource(iconResId);
        ((TextView) dialogView.findViewById(R.id.tvDialogTitle)).setText(title);
        ((TextView) dialogView.findViewById(R.id.tvDialogMessage)).setText(message);

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(dialogView)
                .setCancelable(true)
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        dialogView.findViewById(R.id.btnDialogPrimary).setOnClickListener(v -> dialog.dismiss());
        dialogView.findViewById(R.id.btnDialogSecondary).setOnClickListener(v -> {
            dialog.dismiss();
            if (onEditBudgetClicked != null)
                onEditBudgetClicked.run();
        });

        dialog.show();
    }

    private void showWarningSnackbar(String message, boolean isError) {
        if (rootView == null)
            return;
        int color = isError ? Color.parseColor("#C62828") : Color.parseColor("#F57C00");
        Snackbar sb = Snackbar.make(rootView, message, Snackbar.LENGTH_LONG);
        sb.getView().setBackgroundColor(color);
        sb.show();
    }

    // Xóa giao dịch — xác nhận trước khi xóa

    public void showDeleteConfirmDialog(Expense expense) {
        if (context == null)
            return;

        // Kiểm tra nếu expense từ quét hóa đơn → hỏi xóa luôn đồ trong tủ lạnh
        if ("SCAN".equals(expense.getSource())) {
            executorService.execute(() -> {
                int pantryCount = pantryItemDao.countActiveByExpenseId(expense.getId());
                if (activity == null)
                    return;

                activity.runOnUiThread(() -> {
                    if (pantryCount > 0) {
                        showScanDeleteDialog(expense, pantryCount);
                    } else {
                        showSimpleDeleteDialog(expense);
                    }
                });
            });
        } else {
            showSimpleDeleteDialog(expense);
        }
    }

    private void showScanDeleteDialog(Expense expense, int pantryCount) {
        View dialogView = LayoutInflater.from(context)
                .inflate(R.layout.dialog_confirm, null);

        ((ImageView) dialogView.findViewById(R.id.imgDialogIcon))
                .setImageResource(R.drawable.ic_budget_warning);
        ((TextView) dialogView.findViewById(R.id.tvDialogTitle)).setText("Xóa giao dịch quét hóa đơn");
        ((TextView) dialogView.findViewById(R.id.tvDialogMessage)).setText(
                "Giao dịch \"" + expense.getName() + "\" — "
                        + formatCurrency(expense.getAmount())
                        + "\n\nCó " + pantryCount + " mặt hàng trong tủ lạnh liên kết."
                        + "\nBạn muốn xóa luôn đồ trong tủ lạnh không?");

        TextView btnDanger = dialogView.findViewById(R.id.btnDialogDanger);
        btnDanger.setText("Xóa tất cả");

        TextView btnNeutral = dialogView.findViewById(R.id.btnDialogNeutral);
        btnNeutral.setVisibility(View.VISIBLE);
        btnNeutral.setText("Chỉ xóa giao dịch");

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(dialogView)
                .setCancelable(true)
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnDanger.setOnClickListener(v -> {
            dialog.dismiss();
            deleteExpenseWithPantry(expense, true);
        });
        btnNeutral.setOnClickListener(v -> {
            dialog.dismiss();
            deleteExpenseWithPantry(expense, false);
        });
        dialogView.findViewById(R.id.btnDialogCancel).setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void showSimpleDeleteDialog(Expense expense) {
        View dialogView = LayoutInflater.from(context)
                .inflate(R.layout.dialog_confirm, null);

        ((ImageView) dialogView.findViewById(R.id.imgDialogIcon))
                .setImageResource(R.drawable.ic_toast_alert);
        ((TextView) dialogView.findViewById(R.id.tvDialogTitle)).setText("Xóa giao dịch");
        ((TextView) dialogView.findViewById(R.id.tvDialogMessage)).setText(
                "Bạn có chắc muốn xóa khoản chi\n\""
                        + expense.getName() + "\" — "
                        + formatCurrency(expense.getAmount()) + " không?");

        TextView btnDanger = dialogView.findViewById(R.id.btnDialogDanger);
        btnDanger.setText("Xóa");

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(dialogView)
                .setCancelable(true)
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnDanger.setOnClickListener(v -> {
            dialog.dismiss();
            deleteExpenseWithPantry(expense, false);
        });
        dialogView.findViewById(R.id.btnDialogCancel).setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void deleteExpenseWithPantry(Expense expense, boolean alsoDeletePantry) {
        executorService.execute(() -> {
            if (alsoDeletePantry) {
                pantryItemDao.deactivateByExpenseId(expense.getId());
            }
            expenseDao.deleteExpense(expense);
            if (activity != null) {
                activity.runOnUiThread(() -> {
                    String msg = alsoDeletePantry
                            ? "Đã xóa giao dịch và đồ trong tủ lạnh"
                            : "Đã xóa giao dịch";
                    showCustomToast(msg, R.drawable.ic_toast_delete);
                    hasShownWarning = false;
                    if (onDataChanged != null)
                        onDataChanged.run();
                });
            }
        });
    }

    // Helpers

    public void showCustomToast(String message, int iconResId) {
        if (context == null)
            return;
        View layout = LayoutInflater.from(context).inflate(R.layout.custom_toast, null);
        TextView text = layout.findViewById(R.id.toastText);
        text.setText(message);
        android.widget.ImageView icon = layout.findViewById(R.id.toastIcon);
        icon.setImageResource(iconResId);

        Toast toast = new Toast(context);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(layout);
        toast.show();
    }

    private String formatCurrency(double amount) {
        return String.format(Locale.getDefault(), "%,.0f đ", amount);
    }
}
