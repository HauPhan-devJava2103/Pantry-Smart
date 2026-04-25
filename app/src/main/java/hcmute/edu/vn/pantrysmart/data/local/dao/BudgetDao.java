package hcmute.edu.vn.pantrysmart.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import hcmute.edu.vn.pantrysmart.data.local.entity.Budget;
import hcmute.edu.vn.pantrysmart.data.local.relation.BudgetWithExpenses;

@Dao
public interface BudgetDao {

    // Thêm một ngân sách mới
    @Insert
    long insert(Budget budget);

    // Cập nhật thông tin ngân sách
    @Update
    void update(Budget budget);

    // Lấy thông tin ngân sách theo tháng và năm cụ thể
    @Query("SELECT * FROM budgets WHERE month = :month AND year = :year LIMIT 1")
    Budget getBudgetForMonth(int month, int year);

    // Lấy thông tin ngân sách từ tháng/năm, bao gồm danh sách các khoản chi tiêu
    // liên quan
    @Transaction
    @Query("SELECT * FROM budgets WHERE month = :month AND year = :year LIMIT 1")
    BudgetWithExpenses getBudgetWithExpenses(int month, int year);

    // DEPRECATED: Dùng JOIN nên bỏ sót expense có budget_id = NULL.
    // Sử dụng ExpenseDao.getTotalSpentForPeriod() thay thế.
    @Deprecated
    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM expenses e " +
            "JOIN budgets b ON e.budget_id = b.id " +
            "WHERE b.month = :month AND b.year = :year")
    double getTotalSpentForMonth(int month, int year);
}
