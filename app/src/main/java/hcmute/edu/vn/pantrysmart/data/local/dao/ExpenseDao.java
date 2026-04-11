package hcmute.edu.vn.pantrysmart.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

import hcmute.edu.vn.pantrysmart.data.local.entity.Expense;
import hcmute.edu.vn.pantrysmart.data.local.entity.ExpenseCategory;

@Dao
public interface ExpenseDao {

    // Thêm một giao dịch chi tiêu mới
    @Insert
    long insertExpense(Expense expense);

    // Thêm một danh mục chi tiêu mới
    @Insert
    void insertCategory(ExpenseCategory category);

    // Lấy danh sách các khoản chi tiêu gần đây nhất, giới hạn số lượng trả về
    @Query("SELECT * FROM expenses ORDER BY expense_date DESC LIMIT :limit")
    List<Expense> getRecentExpenses(int limit);

    // Tính tổng số tiền đã chi tiêu trong một khoảng thời gian cụ thể (ví dụ: trong 1 ngày)
    @Query("SELECT COALESCE(SUM(amount), 0) FROM expenses " +
           "WHERE expense_date >= :dayStart AND expense_date < :dayEnd")
    double getTotalSpentForDay(long dayStart, long dayEnd);

    // Đếm tổng số giao dịch chi tiêu trong một khoảng thời gian (thường dùng cho thống kê tháng)
    @Query("SELECT COUNT(*) FROM expenses " +
           "WHERE expense_date >= :monthStart AND expense_date < :monthEnd")
    int getTransactionCountForMonth(long monthStart, long monthEnd);

    // Lấy danh sách toàn bộ các danh mục chi tiêu khả dụng
    @Query("SELECT * FROM expense_categories")
    List<ExpenseCategory> getAllCategories();
}
