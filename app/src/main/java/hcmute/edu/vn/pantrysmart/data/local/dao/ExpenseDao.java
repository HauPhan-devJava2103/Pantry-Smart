package hcmute.edu.vn.pantrysmart.data.local.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import hcmute.edu.vn.pantrysmart.data.local.entity.Expense;
import hcmute.edu.vn.pantrysmart.data.local.entity.ExpenseCategory;

@Dao
public interface ExpenseDao {

       // Thêm một giao dịch chi tiêu mới
       @Insert
       long insertExpense(Expense expense);

       // Cập nhật thông tin giao dịch chi tiêu
       @Update
       void updateExpense(Expense expense);

       // Xóa một giao dịch chi tiêu
       @Delete
       void deleteExpense(Expense expense);

       // Thêm một danh mục chi tiêu mới
       @Insert
       void insertCategory(ExpenseCategory category);

       // Lấy danh sách các khoản chi tiêu gần đây nhất, giới hạn số lượng trả về
       @Query("SELECT * FROM expenses ORDER BY expense_date DESC LIMIT :limit")
       List<Expense> getRecentExpenses(int limit);

       // Tính tổng số tiền đã chi tiêu trong một khoảng thời gian cụ thể (ví dụ: trong
       // 1 ngày)
       @Query("SELECT COALESCE(SUM(amount), 0) FROM expenses " +
                     "WHERE expense_date >= :dayStart AND expense_date < :dayEnd")
       double getTotalSpentForDay(long dayStart, long dayEnd);

       // Tính tổng chi tiêu theo khoảng thời gian tổng quát (tuần / tháng)
       // Không phụ thuộc budget_id — đảm bảo ghi nhận đủ ngay cả khi expense không có
       // budget
       @Query("SELECT COALESCE(SUM(amount), 0) FROM expenses " +
                     "WHERE expense_date >= :start AND expense_date < :end")
       double getTotalSpentForPeriod(long start, long end);

       // Lấy danh sách toàn bộ các danh mục chi tiêu khả dụng
       @Query("SELECT * FROM expense_categories")
       List<ExpenseCategory> getAllCategories();

       // Thống kê tổng chi tiêu theo danh mục trong khoảng thời gian (tháng)
       // Trả về mảng categoryKey + tổng amount, chỉ lấy các danh mục có chi tiêu > 0
       @Query("SELECT category_key, SUM(amount) as total FROM expenses " +
                     "WHERE expense_date >= :start AND expense_date < :end " +
                     "GROUP BY category_key " +
                     "ORDER BY total DESC")
       List<CategoryStat> getSpentByCategory(long start, long end);

       // Tổng chi tiêu theo từng ngày trong 7 ngày gần nhất (cho BarChart)
       @Query("SELECT expense_date, SUM(amount) as total FROM expenses " +
                     "WHERE expense_date >= :weekStart AND expense_date < :weekEnd " +
                     "GROUP BY strftime('%Y-%m-%d', expense_date / 1000, 'unixepoch', 'localtime') " +
                     "ORDER BY expense_date ASC")
       List<DailyStat> getDailySpentForWeek(long weekStart, long weekEnd);

       // DTO nhúng — thống kê theo danh mục
       class CategoryStat {
              public String category_key;
              public double total;
       }

       // DTO nhúng — thống kê chi tiêu hàng ngày
       class DailyStat {
              public long expense_date;
              public double total;
       }

       // Tổng chi tiêu theo tuần trong tháng (cho BarChart tháng — 4~5 tuần)
       @Query("SELECT ((expense_date - :monthStart) / 604800000) as week_index, " +
                     "SUM(amount) as total FROM expenses " +
                     "WHERE expense_date >= :monthStart AND expense_date < :monthEnd " +
                     "GROUP BY week_index " +
                     "ORDER BY week_index ASC")
       List<WeeklyStat> getWeeklySpentForMonth(long monthStart, long monthEnd);

       // Lấy danh sách giao dịch trong khoảng thời gian cụ thể (tuần hoặc tháng)
       @Query("SELECT * FROM expenses " +
                     "WHERE expense_date >= :start AND expense_date < :end " +
                     "ORDER BY expense_date DESC")
       List<Expense> getExpensesForPeriod(long start, long end);

       // Đếm giao dịch theo khoảng thời gian tổng quát
       @Query("SELECT COUNT(*) FROM expenses " +
                     "WHERE expense_date >= :start AND expense_date < :end")
       int getTransactionCountForPeriod(long start, long end);

       // DTO nhúng — thống kê chi tiêu theo tuần
       class WeeklyStat {
              public int week_index;
              public double total;
       }
}
