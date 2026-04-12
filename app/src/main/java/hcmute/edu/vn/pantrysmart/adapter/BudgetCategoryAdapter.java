package hcmute.edu.vn.pantrysmart.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.List;
import java.util.Locale;

import hcmute.edu.vn.pantrysmart.R;
import hcmute.edu.vn.pantrysmart.config.FoodEmojiConfig;
import hcmute.edu.vn.pantrysmart.data.local.dao.ExpenseDao;
import hcmute.edu.vn.pantrysmart.data.local.entity.ExpenseCategory;

/**
 * Adapter cho phần "Theo danh mục" trong BudgetFragment.
 * Mỗi item hiển thị: emoji, tên danh mục, tổng chi tiêu, progress bar tỉ lệ.
 * Layout: item_budget_category.xml
 */
public class BudgetCategoryAdapter extends RecyclerView.Adapter<BudgetCategoryAdapter.ViewHolder> {

    private List<ExpenseCategory>       categories;
    private List<ExpenseDao.CategoryStat> stats;
    private double                      totalMonthSpent;

    public BudgetCategoryAdapter(List<ExpenseCategory> categories,
                                  List<ExpenseDao.CategoryStat> stats,
                                  double totalMonthSpent) {
        this.categories      = categories;
        this.stats           = stats;
        this.totalMonthSpent = totalMonthSpent;
    }

    public void update(List<ExpenseCategory> categories,
                       List<ExpenseDao.CategoryStat> stats,
                       double totalMonthSpent) {
        this.categories      = categories;
        this.stats           = stats;
        this.totalMonthSpent = totalMonthSpent;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_budget_category, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ExpenseCategory cat = categories.get(position);

        // Lấy tổng chi tiêu của danh mục này từ stats
        double spent = 0;
        if (stats != null) {
            for (ExpenseDao.CategoryStat s : stats) {
                if (cat.getCategoryKey().equals(s.category_key)) {
                    spent = s.total;
                    break;
                }
            }
        }

        holder.tvEmoji.setText(FoodEmojiConfig.safeEmoji(cat.getEmoji()));
        holder.tvName.setText(cat.getLabel());
        holder.tvSpent.setText(formatCurrency(spent));

        // Tỉ lệ % so với tổng chi tiêu tháng
        int progress = (totalMonthSpent > 0)
                ? (int) Math.round((spent / totalMonthSpent) * 100)
                : 0;
        holder.progressBar.setProgress(Math.min(progress, 100));
    }

    @Override
    public int getItemCount() {
        return categories != null ? categories.size() : 0;
    }

    private String formatCurrency(double amount) {
        return String.format(Locale.getDefault(), "%,.0f đ", amount);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView                tvEmoji, tvName, tvSpent;
        LinearProgressIndicator progressBar;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvEmoji     = itemView.findViewById(R.id.tvCategoryEmoji);
            tvName      = itemView.findViewById(R.id.tvCategoryName);
            tvSpent     = itemView.findViewById(R.id.tvCategorySpent);
            progressBar = itemView.findViewById(R.id.progressBarCategory);
        }
    }
}
