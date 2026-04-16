package hcmute.edu.vn.pantrysmart.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

// Danh mục chi tiêu cố định: Mua sắm, Giao hàng, Đồ ăn vặt, Khác.
@Entity(tableName = "expense_categories")
public class ExpenseCategory {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "category_key")
    private String categoryKey;

    @ColumnInfo(name = "label")
    private String label;

    @ColumnInfo(name = "emoji")
    private String emoji;

    public ExpenseCategory() {
        this.categoryKey = "";
    }

    @NonNull
    public String getCategoryKey() {
        return categoryKey;
    }

    public void setCategoryKey(@NonNull String categoryKey) {
        this.categoryKey = categoryKey;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getEmoji() {
        return emoji;
    }

    public void setEmoji(String emoji) {
        this.emoji = emoji;
    }

    @Override
    public String toString() {
        return label;
    }
}
