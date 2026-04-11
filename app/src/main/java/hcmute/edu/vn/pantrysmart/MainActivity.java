package hcmute.edu.vn.pantrysmart;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

import hcmute.edu.vn.pantrysmart.data.local.PantrySmartDatabase;
import hcmute.edu.vn.pantrysmart.data.local.dao.PantryItemDao;
import hcmute.edu.vn.pantrysmart.data.local.entity.PantryItem;
import hcmute.edu.vn.pantrysmart.fragment.FridgeFragment;

/**
 * MainActivity — Navigation Shell.
 * Quản lý Header, Bottom Navigation, và FAB.
 * Chuyển đổi Fragment cho từng tab:
 * - Tab 0: FridgeFragment (Tủ lạnh)
 * - Tab 1: SuggestFragment (Gợi ý)
 * - Tab 2: BudgetFragment (Ngân sách)
 */
public class MainActivity extends AppCompatActivity {

    private PantrySmartDatabase db;
    private PantryItemDao pantryDao;

    // Header Views
    private TextView tvGreeting, tvHeaderTitle, tvExpiryBadge, tvNotificationBadge;

    // Current Fragment & Tab
    private int currentTab = 0;
    private FridgeFragment fridgeFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, systemBars.top, 0, systemBars.bottom);
            return insets;
        });

        // Init database
        db = PantrySmartDatabase.getInstance(this);
        pantryDao = db.pantryItemDao();

        // Header
        tvGreeting = findViewById(R.id.tvGreeting);
        tvHeaderTitle = findViewById(R.id.tvHeaderTitle);
        tvExpiryBadge = findViewById(R.id.tvExpiryBadge);
        tvNotificationBadge = findViewById(R.id.tvNotificationBadge);

        updateGreeting();
        updateHeaderStats();

        // Header Actions
        findViewById(R.id.searchBar)
                .setOnClickListener(v -> Toast.makeText(this, "Tìm kiếm thực phẩm", Toast.LENGTH_SHORT).show());
        findViewById(R.id.btnNotification)
                .setOnClickListener(v -> Toast.makeText(this, "Thông báo", Toast.LENGTH_SHORT).show());
        findViewById(R.id.btnProfile)
                .setOnClickListener(v -> Toast.makeText(this, "Tài khoản", Toast.LENGTH_SHORT).show());

        // Bottom Navigation
        findViewById(R.id.navTabFridge).setOnClickListener(v -> switchTab(0));
        findViewById(R.id.navTabSuggest).setOnClickListener(v -> {
            switchTab(1);
            Toast.makeText(this, "Gợi ý công thức - Sắp ra mắt", Toast.LENGTH_SHORT).show();
        });
        findViewById(R.id.navTabBudget).setOnClickListener(v -> {
            switchTab(2);
            Toast.makeText(this, "Ngân sách - Sắp ra mắt", Toast.LENGTH_SHORT).show();
        });

        // FAB
        findViewById(R.id.fabAddItem)
                .setOnClickListener(v -> Toast.makeText(this, "Thêm thực phẩm", Toast.LENGTH_SHORT).show());

        // Load default tab
        if (savedInstanceState == null) {
            fridgeFragment = new FridgeFragment();
            switchTab(0);
        }
    }

    /**
     * Switch tab — load Fragment + update Bottom Nav active state.
     */
    private void switchTab(int tabIndex) {
        currentTab = tabIndex;
        setActiveTab(tabIndex);

        Fragment fragment;
        switch (tabIndex) {
            case 1:
                // TODO: Create SuggestFragment
                return;
            case 2:
                // TODO: Create BudgetFragment
                return;
            default:
                if (fridgeFragment == null)
                    fridgeFragment = new FridgeFragment();
                fragment = fridgeFragment;
                break;
        }

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }

    /**
     * Update header stats from database (item count + expiry badge).
     */
    private void updateHeaderStats() {
        PantrySmartDatabase.databaseWriteExecutor.execute(() -> {
            int totalMain = pantryDao.countItemsByZone("MAIN");
            int totalFreezer = pantryDao.countItemsByZone("FREEZER");
            long now = System.currentTimeMillis();
            long twoDays = TimeUnit.DAYS.toMillis(2);
            int expiringCount = pantryDao.getExpiringItems(now, twoDays).size();

            runOnUiThread(() -> {
                int total = totalMain + totalFreezer;
                tvHeaderTitle.setText(getString(R.string.pantry_item_count, total));

                if (expiringCount == 0) {
                    tvExpiryBadge.setText("An toàn");
                    tvNotificationBadge.setVisibility(View.GONE);
                } else {
                    tvExpiryBadge.setText(expiringCount + " sắp hết");
                    tvNotificationBadge.setText(String.valueOf(expiringCount));
                    tvNotificationBadge.setVisibility(View.VISIBLE);
                }
            });
        });
    }

    /**
     * Update greeting text + icon based on time of day.
     */
    private void updateGreeting() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        int greetingResId, iconResId;

        if (hour >= 5 && hour < 12) {
            greetingResId = R.string.greeting_morning;
            iconResId = R.drawable.ic_sun;
        } else if (hour >= 12 && hour < 18) {
            greetingResId = R.string.greeting_afternoon;
            iconResId = R.drawable.ic_cloud;
        } else {
            greetingResId = R.string.greeting_evening;
            iconResId = R.drawable.ic_moon;
        }

        tvGreeting.setText(greetingResId);
        tvGreeting.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, iconResId, 0);
    }

    /**
     * Update Bottom Navigation active state (Figma spec).
     */
    private void setActiveTab(int tabIndex) {
        View[] indicators = {
                findViewById(R.id.navIndicatorFridge),
                findViewById(R.id.navIndicatorSuggest),
                findViewById(R.id.navIndicatorBudget)
        };
        ImageView[] icons = {
                findViewById(R.id.navIconFridge),
                findViewById(R.id.navIconSuggest),
                findViewById(R.id.navIconBudget)
        };
        TextView[] labels = {
                findViewById(R.id.navLabelFridge),
                findViewById(R.id.navLabelSuggest),
                findViewById(R.id.navLabelBudget)
        };

        int activeColor = getColor(R.color.primary_dark);
        int inactiveColor = getColor(R.color.text_hint);

        for (int i = 0; i < 3; i++) {
            boolean isActive = (i == tabIndex);
            indicators[i].setVisibility(isActive ? View.VISIBLE : View.GONE);
            icons[i].setColorFilter(isActive ? activeColor : inactiveColor);
            labels[i].setTextColor(isActive ? activeColor : inactiveColor);
        }
    }
}