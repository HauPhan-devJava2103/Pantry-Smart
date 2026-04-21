package hcmute.edu.vn.pantrysmart.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import hcmute.edu.vn.pantrysmart.R;
import hcmute.edu.vn.pantrysmart.config.FoodIconConfig;
import hcmute.edu.vn.pantrysmart.data.local.entity.Expense;

/**
 * Adapter cho danh sách giao dịch gần đây.
 * Hỗ trợ hai chế độ:
 * 1. Chỉnh sửa (BudgetFragment): Có nút Edit và Delete.
 * 2. Chỉ xem (ExpenseDetailActivity): Ẩn nút, cho phép chạm để xem chi tiết.
 * Layout: item_recent_transaction.xml
 */
public class RecentTransactionAdapter extends RecyclerView.Adapter<RecentTransactionAdapter.ViewHolder> {

    // ==== Interfaces ====

    /** Cung cấp emoji và label theo categoryKey từ bên ngoài adapter */
    public interface CategoryEmojiResolver {
        String getEmoji(String categoryKey);
        String getLabel(String categoryKey);
    }

    /** Callback khi người dùng nhấn nút Edit hoặc Delete */
    public interface OnActionListener {
        void onEdit(Expense expense);
        void onDelete(Expense expense);
        void onClick(Expense expense);
    }

    // ==== Fields ====

    private List<Expense>          expenses;
    private final CategoryEmojiResolver emojiResolver;
    private       OnActionListener actionListener;
    private       boolean          isReadOnly = false;

    // ==== Constructor ====

    public RecentTransactionAdapter(List<Expense> expenses,
                                    CategoryEmojiResolver emojiResolver) {
        this.expenses      = expenses;
        this.emojiResolver = emojiResolver;
    }

    public RecentTransactionAdapter(List<Expense> expenses,
                                    CategoryEmojiResolver emojiResolver,
                                    boolean isReadOnly) {
        this.expenses      = expenses;
        this.emojiResolver = emojiResolver;
        this.isReadOnly    = isReadOnly;
    }

    public void setOnActionListener(OnActionListener listener) {
        this.actionListener = listener;
    }

    public void setExpenses(List<Expense> expenses) {
        this.expenses = expenses;
        notifyDataSetChanged();
    }

    // ==== RecyclerView.Adapter ====

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recent_transaction, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Expense expense = expenses.get(position);

        // Icon danh mục — resolve emoji string → drawable resource
        String emoji = emojiResolver.getEmoji(expense.getCategoryKey());
        holder.imgIcon.setImageResource(FoodIconConfig.safeIcon(emoji));

        // Tên giao dịch
        holder.tvName.setText(expense.getName());

        // "Danh mục • dd/MM"
        String label   = emojiResolver.getLabel(expense.getCategoryKey());
        String dateStr = new SimpleDateFormat("dd/MM", Locale.getDefault())
                .format(new Date(expense.getExpenseDate()));
        holder.tvInfo.setText((label != null ? label : "Khác") + " • " + dateStr);

        // Số tiền
        holder.tvAmount.setText(formatAmount(expense.getAmount()));

        // Nút Edit
        holder.btnEdit.setVisibility(isReadOnly ? View.GONE : View.VISIBLE);
        holder.btnEdit.setOnClickListener(v -> {
            if (actionListener != null) actionListener.onEdit(expense);
        });

        // Nút Delete
        holder.btnDelete.setVisibility(isReadOnly ? View.GONE : View.VISIBLE);
        holder.btnDelete.setOnClickListener(v -> {
            if (actionListener != null) actionListener.onDelete(expense);
        });

        // Item Click
        holder.itemView.setOnClickListener(v -> {
            if (actionListener != null) actionListener.onClick(expense);
        });
    }

    @Override
    public int getItemCount() {
        return expenses != null ? expenses.size() : 0;
    }

    // ==== Helpers ====

    private String formatAmount(double amount) {
        return String.format(Locale.getDefault(), "-%,.0fđ", amount);
    }

    // ==== ViewHolder ====

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgIcon;
        TextView  tvName, tvInfo, tvAmount;
        ImageView btnEdit, btnDelete;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgIcon   = itemView.findViewById(R.id.imgTransactionIcon);
            tvName    = itemView.findViewById(R.id.tvTransactionName);
            tvInfo    = itemView.findViewById(R.id.tvTransactionInfo);
            tvAmount  = itemView.findViewById(R.id.tvTransactionAmount);
            btnEdit   = itemView.findViewById(R.id.btnEditTransaction);
            btnDelete = itemView.findViewById(R.id.btnDeleteTransaction);
        }
    }
}
