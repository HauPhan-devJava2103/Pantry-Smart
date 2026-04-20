package hcmute.edu.vn.pantrysmart.data.local.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import hcmute.edu.vn.pantrysmart.data.local.entity.PantryItem;

@Dao
public interface PantryItemDao {

    // Thêm một thực phẩm mới vào tủ lạnh
    @Insert
    long insert(PantryItem item);

    // Cập nhật thông tin thực phẩm
    @Update
    void update(PantryItem item);

    // Xóa vật lý một thực phẩm khỏi cơ sở dữ liệu
    @Delete
    void delete(PantryItem item);

    // Lấy tất cả các thực phẩm đang hoạt động (chưa bị xóa mềm), sắp xếp theo ngày hết hạn tăng dần
    @Query("SELECT * FROM pantry_items WHERE is_active = 1 ORDER BY expiry_date ASC")
    List<PantryItem> getAllActiveItems();

    // Lấy các thực phẩm đang hoạt động dựa trên khu vực lưu trữ (ví dụ: ngăn mát, ngăn đông)
    @Query("SELECT * FROM pantry_items WHERE is_active = 1 AND storage_zone = :zone")
    List<PantryItem> getItemsByZone(String zone);

    // Đếm số lượng thực phẩm đang có trong một khu vực lưu trữ cụ thể
    @Query("SELECT COUNT(*) FROM pantry_items WHERE is_active = 1 AND storage_zone = :zone")
    int countItemsByZone(String zone);

    // Lấy danh sách thực phẩm đang hoạt động chuẩn bị hết hạn trong khoảng `daysInMillis` tính từ `now`
    @Query("SELECT * FROM pantry_items " +
           "WHERE is_active = 1 AND expiry_date IS NOT NULL " +
           "AND expiry_date BETWEEN :now AND :now + :daysInMillis " +
           "ORDER BY expiry_date ASC")
    List<PantryItem> getExpiringItems(long now, long daysInMillis);

    // Đếm tổng số thực phẩm đang thực tế có trong tủ lạnh (active)
    @Query("SELECT COUNT(*) FROM pantry_items WHERE is_active = 1")
    int countActiveItems();

    // Tìm kiếm một thực phẩm theo tên khớp hoàn toàn
    @Query("SELECT * FROM pantry_items WHERE is_active = 1 AND name = :name LIMIT 1")
    PantryItem findByName(String name);

    // Trừ đi một lượng nguyên liệu của một mặt hàng cụ thể (dùng khi nấu ăn)
    @Query("UPDATE pantry_items SET quantity = quantity - :amount WHERE id = :itemId")
    void deductQuantity(int itemId, double amount);

    // Vô hiệu hóa/Xóa mềm một thực phẩm (đặt is_active = 0)
    @Query("UPDATE pantry_items SET is_active = 0 WHERE id = :itemId")
    void deactivateItem(int itemId);

    // Lấy thực phẩm đã hết hạn (expiryDate < now)
    @Query("SELECT * FROM pantry_items WHERE is_active = 1 AND expiry_date IS NOT NULL AND expiry_date < :now ORDER BY expiry_date ASC")
    List<PantryItem> getExpiredItems(long now);
}
