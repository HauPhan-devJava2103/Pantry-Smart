package hcmute.edu.vn.pantrysmart.data.local;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import hcmute.edu.vn.pantrysmart.data.local.dao.BudgetDao;
import hcmute.edu.vn.pantrysmart.data.local.dao.CookingLogDao;
import hcmute.edu.vn.pantrysmart.data.local.dao.ExpenseDao;
import hcmute.edu.vn.pantrysmart.data.local.dao.PantryItemDao;
import hcmute.edu.vn.pantrysmart.data.local.entity.Budget;
import hcmute.edu.vn.pantrysmart.data.local.entity.CookingLog;
import hcmute.edu.vn.pantrysmart.data.local.entity.CookingLogItem;
import hcmute.edu.vn.pantrysmart.data.local.entity.Expense;
import hcmute.edu.vn.pantrysmart.data.local.entity.ExpenseCategory;
import hcmute.edu.vn.pantrysmart.data.local.entity.PantryItem;

@Database(entities = {
                PantryItem.class,
                CookingLog.class,
                CookingLogItem.class,
                Budget.class,
                Expense.class,
                ExpenseCategory.class
}, version = 6, exportSchema = false)
public abstract class PantrySmartDatabase extends RoomDatabase {

        // DAOs
        public abstract PantryItemDao pantryItemDao();

        public abstract CookingLogDao cookingLogDao();

        public abstract BudgetDao budgetDao();

        public abstract ExpenseDao expenseDao();

        // Singleton
        private static volatile PantrySmartDatabase INSTANCE;

        public static final ExecutorService databaseWriteExecutor = Executors.newFixedThreadPool(4);

        public static PantrySmartDatabase getInstance(Context context) {
                if (INSTANCE == null) {
                        synchronized (PantrySmartDatabase.class) {
                                if (INSTANCE == null) {
                                        INSTANCE = Room.databaseBuilder(
                                                        context.getApplicationContext(),
                                                        PantrySmartDatabase.class,
                                                        "pantry_smart_db")
                                                        .fallbackToDestructiveMigration()
                                                        .addCallback(seedCallback)
                                                        .build();
                                }
                        }
                }
                return INSTANCE;
        }

        /**
         * Pre-populate dữ liệu mặc định.
         * Sử dụng onOpen + kiểm tra bảng trống để đảm bảo seed luôn chạy
         * bất kể tạo mới hay destructive migration.
         */
        private static final RoomDatabase.Callback seedCallback = new RoomDatabase.Callback() {
                @Override
                public void onOpen(@NonNull SupportSQLiteDatabase db) {
                        super.onOpen(db);
                        // Kiểm tra xem đã có dữ liệu chưa
                        android.database.Cursor cursor = db.query("SELECT COUNT(*) FROM expense_categories");
                        cursor.moveToFirst();
                        int count = cursor.getInt(0);
                        cursor.close();

                        if (count == 0) {
                                seedData(db);
                        }
                }

                private void seedData(SupportSQLiteDatabase db) {
                        // 4 danh mục chi tiêu mặc định
                        db.execSQL("INSERT INTO expense_categories VALUES ('SHOPPING','Mua sắm','🛒')");
                        db.execSQL("INSERT INTO expense_categories VALUES ('DELIVERY','Giao hàng','🛵')");
                        db.execSQL("INSERT INTO expense_categories VALUES ('SNACK','Đồ ăn vặt','🍿')");
                        db.execSQL("INSERT INTO expense_categories VALUES ('OTHER','Khác','📦')");

                        // Ngân sách mẫu tháng hiện tại
                        int currentMonth = java.util.Calendar.getInstance().get(java.util.Calendar.MONTH) + 1;
                        int currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR);
                        db.execSQL("INSERT INTO budgets (monthly_limit, month, year) VALUES (2000000, "
                                        + currentMonth + ", " + currentYear + ")");

                        // Thực phẩm mẫu trong tủ lạnh
                        long now = System.currentTimeMillis();
                        long oneDay = 86400000L;

                        // Các sản phẩm có ảnh thật từ internet (dùng Unsplash để test Glide)
                        db.execSQL("INSERT INTO pantry_items (name, emoji, image_path, quantity, unit, expiry_date, added_date, storage_zone, category, is_active) VALUES "
                                        + "('Trứng gà', '🥚', 'https://images.unsplash.com/photo-1582722872445-44dc5f7e3c8f?w=400', 5, 'quả', " + (now + 7 * oneDay) + ", " + now + ", 'MAIN', 'protein', 1)");
                        
                        db.execSQL("INSERT INTO pantry_items (name, emoji, image_path, quantity, unit, expiry_date, added_date, storage_zone, category, is_active) VALUES "
                                        + "('Thịt heo', '🥩', 'https://images.unsplash.com/photo-1627997089456-e9f90f230da3?w=400', 0.5, 'kg', " + (now + 3 * oneDay) + ", " + now + ", 'FREEZER', 'thịt', 1)");

                        db.execSQL("INSERT INTO pantry_items (name, emoji, image_path, quantity, unit, expiry_date, added_date, storage_zone, category, is_active) VALUES "
                                        + "('Cà rốt', '🥕', 'https://images.unsplash.com/photo-1598170845058-32b9d6a5da37?w=400', 3, 'củ', " + (now + 5 * oneDay) + ", " + now + ", 'MAIN', 'rau', 1)");

                        // Các sản phẩm thiếu ảnh (sẽ tự động fallback hiển thị Emoji do image_path = NULL)
                        db.execSQL("INSERT INTO pantry_items (name, emoji, image_path, quantity, unit, expiry_date, added_date, storage_zone, category, is_active) VALUES "
                                        + "('Phô mai', '🧀', NULL, 1, 'hộp', " + (now + 14 * oneDay) + ", " + now + ", 'MAIN', 'sữa', 1)");

                        db.execSQL("INSERT INTO pantry_items (name, emoji, image_path, quantity, unit, expiry_date, added_date, storage_zone, category, is_active) VALUES "
                                        + "('Rau cải', '🥬', NULL, 1, 'bó', " + (now + 2 * oneDay) + ", " + now + ", 'MAIN', 'rau', 1)");

                        db.execSQL("INSERT INTO pantry_items (name, emoji, image_path, quantity, unit, expiry_date, added_date, storage_zone, category, is_active) VALUES "
                                        + "('Tôm', '🦐', NULL, 0.3, 'kg', " + (now + 4 * oneDay) + ", " + now + ", 'FREEZER', 'hải sản', 1)");

                        db.execSQL("INSERT INTO pantry_items (name, emoji, image_path, quantity, unit, expiry_date, added_date, storage_zone, category, is_active) VALUES "
                                        + "('Mì tôm', '🍜', NULL, 5, 'gói', " + (now + 90 * oneDay) + ", " + now + ", 'MAIN', 'khô', 1)");

                        db.execSQL("INSERT INTO pantry_items (name, emoji, image_path, quantity, unit, expiry_date, added_date, storage_zone, category, is_active) VALUES "
                                        + "('Sữa tươi', '🥛', NULL, 1, 'hộp', " + (now + 1 * oneDay) + ", " + now + ", 'MAIN', 'sữa', 1)");
                }
        };
}
