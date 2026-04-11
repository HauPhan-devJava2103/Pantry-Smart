package hcmute.edu.vn.pantrysmart.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.List;

import hcmute.edu.vn.pantrysmart.data.local.entity.CookingLog;
import hcmute.edu.vn.pantrysmart.data.local.entity.CookingLogItem;
import hcmute.edu.vn.pantrysmart.data.local.relation.CookingLogWithItems;

@Dao
public interface CookingLogDao {

    // Thêm một lịch sử nấu ăn mới
    @Insert
    long insertLog(CookingLog log);

    // Thêm danh sách chi tiết các nguyên liệu đã sử dụng cho lần nấu
    @Insert
    void insertLogItems(List<CookingLogItem> items);

    // Lấy danh sách lịch sử nấu ăn gần đây kèm theo chi tiết từng nguyên liệu đã dùng
    @Transaction
    @Query("SELECT * FROM cooking_logs ORDER BY cooked_at DESC LIMIT :limit")
    List<CookingLogWithItems> getRecentCookingLogs(int limit);

    // Đếm tổng số lần đã nấu ăn (số lượng bản ghi CookingLog)
    @Query("SELECT COUNT(*) FROM cooking_logs")
    int getTotalCookCount();
}
