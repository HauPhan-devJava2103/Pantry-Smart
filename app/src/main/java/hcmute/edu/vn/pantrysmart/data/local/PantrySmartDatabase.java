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
}, version = 1, exportSchema = false)
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
                                                        .addCallback(seedCallback)
                                                        .build();
                                }
                        }
                }
                return INSTANCE;
        }

        /**
         * Pre-populate dữ liệu mặc định khi database được tạo lần đầu.
         */
        private static final RoomDatabase.Callback seedCallback = new RoomDatabase.Callback() {
                @Override
                public void onCreate(@NonNull SupportSQLiteDatabase db) {
                        super.onCreate(db);
                        databaseWriteExecutor.execute(() -> {
                                // 4 danh mục chi tiêu mặc định
                                db.execSQL("INSERT INTO expense_categories VALUES ('SHOPPING','Mua sắm','🛒')");
                                db.execSQL("INSERT INTO expense_categories VALUES ('DELIVERY','Giao hàng','🛵')");
                                db.execSQL("INSERT INTO expense_categories VALUES ('SNACK','Đồ ăn vặt','🍿')");
                                db.execSQL("INSERT INTO expense_categories VALUES ('OTHER','Khác','📦')");

                                // Ngân sách mẫu tháng hiện tại
                                db.execSQL("INSERT INTO budgets (monthly_limit, month, year) VALUES (2000000, "
                                                + java.util.Calendar.getInstance().get(java.util.Calendar.MONTH) + 1
                                                + ", "
                                                + java.util.Calendar.getInstance().get(java.util.Calendar.YEAR) + ")");

                                // Thực phẩm mẫu trong tủ lạnh
                                long now = System.currentTimeMillis();
                                long oneDay = 86400000L;

                                db.execSQL("INSERT INTO pantry_items (name, emoji, quantity, unit, expiry_date, added_date, storage_zone, category, is_active) VALUES "
                                                +
                                                "('Trứng gà', '🥚', 5, 'quả', " + (now + 7 * oneDay) + ", " + now
                                                + ", 'MAIN', 'protein', 1)");
                                db.execSQL("INSERT INTO pantry_items (name, emoji, quantity, unit, expiry_date, added_date, storage_zone, category, is_active) VALUES "
                                                +
                                                "('Phô mai', '🧀', 1, 'hộp', " + (now + 14 * oneDay) + ", " + now
                                                + ", 'MAIN', 'sữa', 1)");
                                db.execSQL("INSERT INTO pantry_items (name, emoji, quantity, unit, expiry_date, added_date, storage_zone, category, is_active) VALUES "
                                                +
                                                "('Thịt heo', '🥩', 0.5, 'kg', " + (now + 3 * oneDay) + ", " + now
                                                + ", 'FREEZER', 'thịt', 1)");
                                db.execSQL("INSERT INTO pantry_items (name, emoji, quantity, unit, expiry_date, added_date, storage_zone, category, is_active) VALUES "
                                                +
                                                "('Cà rốt', '🥕', 3, 'củ', " + (now + 5 * oneDay) + ", " + now
                                                + ", 'MAIN', 'rau', 1)");
                                db.execSQL("INSERT INTO pantry_items (name, emoji, quantity, unit, expiry_date, added_date, storage_zone, category, is_active) VALUES "
                                                +
                                                "('Rau cải', '🥬', 1, 'bó', " + (now + 2 * oneDay) + ", " + now
                                                + ", 'MAIN', 'rau', 1)");
                                db.execSQL("INSERT INTO pantry_items (name, emoji, quantity, unit, expiry_date, added_date, storage_zone, category, is_active) VALUES "
                                                +
                                                "('Tôm', '🦐', 0.3, 'kg', " + (now + 4 * oneDay) + ", " + now
                                                + ", 'FREEZER', 'hải sản', 1)");
                                db.execSQL("INSERT INTO pantry_items (name, emoji, quantity, unit, expiry_date, added_date, storage_zone, category, is_active) VALUES "
                                                +
                                                "('Mì tôm', '🍜', 5, 'gói', " + (now + 90 * oneDay) + ", " + now
                                                + ", 'MAIN', 'khô', 1)");
                                db.execSQL("INSERT INTO pantry_items (name, emoji, quantity, unit, expiry_date, added_date, storage_zone, category, is_active) VALUES "
                                                +
                                                "('Sữa tươi', '🥛', 1, 'hộp', " + (now + 1 * oneDay) + ", " + now
                                                + ", 'MAIN', 'sữa', 1)");
                        });
                }
        };
}
