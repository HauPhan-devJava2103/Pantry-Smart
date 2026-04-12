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
import hcmute.edu.vn.pantrysmart.data.local.entity.Expense;

/**
 * Adapter cho danh sách giao dịch gần đây trong BudgetFragment.
 * Mỗi item có nút Edit (✏️) và Delete (🗑️).
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
    }

    // ==== Fields ====

    private List<Expense>          expenses;
    private final CategoryEmojiResolver emojiResolver;
    private       OnActionListener actionListener;

    // ==== Constructor ====

    public RecentTransactionAdapter(List<Expense> expenses,
                                    CategoryEmojiResolver emojiResolver) {
        this.expenses      = expenses;
        this.emojiResolver = emojiResolver;
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

        // Emoji danh mục
        String emoji = emojiResolver.getEmoji(expense.getCategoryKey());
        holder.tvEmoji.setText(emoji != null ? emoji : "💰");

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
        holder.btnEdit.setOnClickListener(v -> {
            if (actionListener != null) actionListener.onEdit(expense);
        });

        // Nút Delete
        holder.btnDelete.setOnClickListener(v -> {
            if (actionListener != null) actionListener.onDelete(expense);
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
        TextView  tvEmoji, tvName, tvInfo, tvAmount;
        ImageView btnEdit, btnDelete;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvEmoji   = itemView.findViewById(R.id.tvTransactionEmoji);
            tvName    = itemView.findViewById(R.id.tvTransactionName);
            tvInfo    = itemView.findViewById(R.id.tvTransactionInfo);
            tvAmount  = itemView.findViewById(R.id.tvTransactionAmount);
            btnEdit   = itemView.findViewById(R.id.btnEditTransaction);
            btnDelete = itemView.findViewById(R.id.btnDeleteTransaction);
        }
    }
}
