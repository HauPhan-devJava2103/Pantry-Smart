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
}, version = 10, exportSchema = false)
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

                        // Migrate: cập nhật emoji Unicode cũ → drawable name
                        migrateEmojiToDrawable(db);
                }

                /**
                 * Chuyển đổi emoji Unicode sang drawable name cho dữ liệu cũ.
                 * Chạy mỗi lần mở DB nhưng chỉ ảnh hưởng row còn dùng emoji cũ.
                 */
                private void migrateEmojiToDrawable(SupportSQLiteDatabase db) {
                        // Cập nhật emoji Unicode cũ sang drawable name.
                        // Dùng UPDATE ... WHERE emoji NOT LIKE 'ic_%' để bắt tất cả
                        // row chưa migrate (bất kể emoji nào), rồi gán default icon.
                        // Sau đó dùng bind param tránh lỗi encoding.

                        // expense_categories — chỉ có 4 key cố định
                        db.execSQL("UPDATE expense_categories SET emoji = 'ic_shopping_cart' WHERE category_key = 'SHOPPING'");
                        db.execSQL("UPDATE expense_categories SET emoji = 'ic_delivery'      WHERE category_key = 'DELIVERY'");
                        db.execSQL("UPDATE expense_categories SET emoji = 'ic_snack'         WHERE category_key = 'SNACK'");
                        db.execSQL("UPDATE expense_categories SET emoji = 'ic_food_package'  WHERE category_key = 'OTHER'");

                        // pantry_items — gán icon mặc định cho tất cả row chưa có drawable name
                        db.execSQL("UPDATE pantry_items SET emoji = 'ic_food_package' WHERE emoji IS NOT NULL AND emoji != '' AND emoji NOT LIKE 'ic_%'");
                }

                private void seedData(SupportSQLiteDatabase db) {
                        // 4 danh mục chi tiêu mặc định (emoji = drawable name)
                        db.execSQL("INSERT INTO expense_categories VALUES ('SHOPPING','Mua sắm','ic_shopping_cart')");
                        db.execSQL("INSERT INTO expense_categories VALUES ('DELIVERY','Giao hàng','ic_delivery')");
                        db.execSQL("INSERT INTO expense_categories VALUES ('SNACK','Đồ ăn vặt','ic_snack')");
                        db.execSQL("INSERT INTO expense_categories VALUES ('OTHER','Khác','ic_food_package')");

                        // Ngân sách mẫu tháng hiện tại (monthly: 2tr, weekly: 500k)
                        int currentMonth = java.util.Calendar.getInstance().get(java.util.Calendar.MONTH) + 1;
                        int currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR);
                        db.execSQL("INSERT INTO budgets (monthly_limit, weekly_limit, month, year) VALUES (2000000, 500000, "
                                        + currentMonth + ", " + currentYear + ")");

                        // Thực phẩm mẫu trong tủ lạnh — đa dạng để test Gemini AI
                        long now = System.currentTimeMillis();
                        long oneDay = 86400000L;

                        // Ngăn chính (MAIN)
                        db.execSQL("INSERT INTO pantry_items (name, emoji, image_path, quantity, unit, expiry_date, added_date, storage_zone, category, is_active) VALUES "
                                        + "('Gạo', 'ic_food_bread', NULL, 5, 'kg', " + (now + 90 * oneDay) + ", " + now
                                        + ", 'MAIN', 'khô', 1)");

                        db.execSQL("INSERT INTO pantry_items (name, emoji, image_path, quantity, unit, expiry_date, added_date, storage_zone, category, is_active) VALUES "
                                        + "('Hành tây', 'ic_food_onion', NULL, 3, 'củ', " + (now + 10 * oneDay) + ", "
                                        + now + ", 'MAIN', 'rau', 1)");

                        db.execSQL("INSERT INTO pantry_items (name, emoji, image_path, quantity, unit, expiry_date, added_date, storage_zone, category, is_active) VALUES "
                                        + "('Tỏi', 'ic_food_garlic', NULL, 1, 'củ', " + (now + 14 * oneDay) + ", " + now
                                        + ", 'MAIN', 'rau', 1)");

                        db.execSQL("INSERT INTO pantry_items (name, emoji, image_path, quantity, unit, expiry_date, added_date, storage_zone, category, is_active) VALUES "
                                        + "('Cà chua', 'ic_food_tomato', NULL, 4, 'quả', " + (now + 5 * oneDay) + ", "
                                        + now + ", 'MAIN', 'rau', 1)");

                        db.execSQL("INSERT INTO pantry_items (name, emoji, image_path, quantity, unit, expiry_date, added_date, storage_zone, category, is_active) VALUES "
                                        + "('Nước mắm', 'ic_food_sauce', NULL, 1, 'chai', " + (now + 180 * oneDay)
                                        + ", " + now + ", 'MAIN', 'gia vị', 1)");

                        db.execSQL("INSERT INTO pantry_items (name, emoji, image_path, quantity, unit, expiry_date, added_date, storage_zone, category, is_active) VALUES "
                                        + "('Trứng gà', 'ic_food_egg', NULL, 10, 'quả', " + (now + 14 * oneDay) + ", "
                                        + now + ", 'MAIN', 'protein', 1)");

                        db.execSQL("INSERT INTO pantry_items (name, emoji, image_path, quantity, unit, expiry_date, added_date, storage_zone, category, is_active) VALUES "
                                        + "('Đậu phụ', 'ic_food_cheese', NULL, 2, 'miếng', " + (now + 3 * oneDay) + ", "
                                        + now + ", 'MAIN', 'protein', 1)");

                        // Ngăn đông (FREEZER)
                        db.execSQL("INSERT INTO pantry_items (name, emoji, image_path, quantity, unit, expiry_date, added_date, storage_zone, category, is_active) VALUES "
                                        + "('Thịt bò', 'ic_food_steak', NULL, 0.5, 'kg', " + (now + 30 * oneDay) + ", "
                                        + now + ", 'FREEZER', 'thịt', 1)");

                        db.execSQL("INSERT INTO pantry_items (name, emoji, image_path, quantity, unit, expiry_date, added_date, storage_zone, category, is_active) VALUES "
                                        + "('Cá basa', 'ic_food_fish', NULL, 0.4, 'kg', " + (now + 20 * oneDay) + ", "
                                        + now + ", 'FREEZER', 'hải sản', 1)");

                        db.execSQL("INSERT INTO pantry_items (name, emoji, image_path, quantity, unit, expiry_date, added_date, storage_zone, category, is_active) VALUES "
                                        + "('Tôm sú', 'ic_food_shrimp', NULL, 0.3, 'kg', " + (now + 15 * oneDay) + ", "
                                        + now + ", 'FREEZER', 'hải sản', 1)");
                        // Chi tiêu mẫu trong tuần hiện tại (để test biểu đồ & giao dịch gần đây)
                        // budget_id = 1 vì vừa insert budget ở trên
                        long today = System.currentTimeMillis();
                        long d = 86400000L; // 1 ngày
                        db.execSQL("INSERT INTO expenses (budget_id, name, amount, category_key, expense_date, source) VALUES (1,'Chợ Long Biên',"
                                        + 85000 + ",'SHOPPING'," + (today - 0 * d) + ",'MANUAL')");
                        db.execSQL("INSERT INTO expenses (budget_id, name, amount, category_key, expense_date, source) VALUES (1,'Baemin cơm trưa',"
                                        + 45000 + ",'DELIVERY'," + (today - 1 * d) + ",'MANUAL')");
                        db.execSQL("INSERT INTO expenses (budget_id, name, amount, category_key, expense_date, source) VALUES (1,'Siêu thị VinMart',"
                                        + 185000 + ",'SHOPPING'," + (today - 2 * d) + ",'MANUAL')");
                        db.execSQL("INSERT INTO expenses (budget_id, name, amount, category_key, expense_date, source) VALUES (1,'Snack Oreo',"
                                        + 25000 + ",'SNACK'," + (today - 2 * d) + ",'MANUAL')");
                        db.execSQL("INSERT INTO expenses (budget_id, name, amount, category_key, expense_date, source) VALUES (1,'Grab Food phở',"
                                        + 65000 + ",'DELIVERY'," + (today - 3 * d) + ",'MANUAL')");
                        db.execSQL("INSERT INTO expenses (budget_id, name, amount, category_key, expense_date, source) VALUES (1,'Rau củ quả',"
                                        + 75000 + ",'SHOPPING'," + (today - 4 * d) + ",'MANUAL')");
                        db.execSQL("INSERT INTO expenses (budget_id, name, amount, category_key, expense_date, source) VALUES (1,'Phí ship hàng online',"
                                        + 30000 + ",'OTHER'," + (today - 5 * d) + ",'MANUAL')");
                }

        };
}
