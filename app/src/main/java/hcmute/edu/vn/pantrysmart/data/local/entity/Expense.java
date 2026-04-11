package hcmute.edu.vn.pantrysmart.data.local.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

// Giao dịch chi tiêu
@Entity(tableName = "expenses", foreignKeys = {
        @ForeignKey(entity = Budget.class, parentColumns = "id", childColumns = "budget_id", onDelete = ForeignKey.SET_NULL),
        @ForeignKey(entity = ExpenseCategory.class, parentColumns = "category_key", childColumns = "category_key", onDelete = ForeignKey.SET_NULL)
})
public class Expense {

    @PrimaryKey(autoGenerate = true)
    private int id;

    @ColumnInfo(name = "budget_id")
    private Integer budgetId;

    @ColumnInfo(name = "name")
    private String name;

    @ColumnInfo(name = "amount")
    private double amount;

    @ColumnInfo(name = "category_key")
    private String categoryKey;

    @ColumnInfo(name = "expense_date")
    private long expenseDate;

    @ColumnInfo(name = "source", defaultValue = "MANUAL")
    private String source;

    public Expense() {
        this.source = "MANUAL";
        this.expenseDate = System.currentTimeMillis();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Integer getBudgetId() {
        return budgetId;
    }

    public void setBudgetId(Integer budgetId) {
        this.budgetId = budgetId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getCategoryKey() {
        return categoryKey;
    }

    public void setCategoryKey(String categoryKey) {
        this.categoryKey = categoryKey;
    }

    public long getExpenseDate() {
        return expenseDate;
    }

    public void setExpenseDate(long expenseDate) {
        this.expenseDate = expenseDate;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }
}
