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
import hcmute.edu.vn.pantrysmart.fragment.BudgetFragment;
import hcmute.edu.vn.pantrysmart.fragment.FridgeFragment;
import hcmute.edu.vn.pantrysmart.fragment.SuggestFragment;

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
    private View headerRow2;
    private View headerContainer, bottomNavigation;

    // Current Fragment & Tab
    private int currentTab = 0;
    private FridgeFragment fridgeFragment;
    private SuggestFragment suggestFragment;
    private BudgetFragment budgetFragment;

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
        headerRow2 = findViewById(R.id.headerRow2);
        headerContainer = findViewById(R.id.headerContainer);
        bottomNavigation = findViewById(R.id.bottomNavigation);

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
        findViewById(R.id.navTabSuggest).setOnClickListener(v -> switchTab(1));
        findViewById(R.id.navTabBudget).setOnClickListener(v -> {
            switchTab(2);
            Toast.makeText(this, "Ngân sách - Sắp ra mắt", Toast.LENGTH_SHORT).show();
        });

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
        updateHeaderForTab(tabIndex);

        Fragment fragment = null;

        switch (tabIndex) {
            case 1:
                if (suggestFragment == null)
                    suggestFragment = new SuggestFragment();
                fragment = suggestFragment;
                break;
            case 2:
                // TODO: Create BudgetFragment
                if (budgetFragment == null) {
                    budgetFragment = new BudgetFragment();
                }
                fragment = budgetFragment;
                break;
            default:
                if (fridgeFragment == null)
                    fridgeFragment = new FridgeFragment();
                fragment = fridgeFragment;
                break;
        }
        if (fragment != null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, fragment)
                    .commit();
        }
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

    private void updateHeaderForTab(int tabIndex) {
        View bottomNavWrapper = (bottomNavigation != null && bottomNavigation.getParent() instanceof View)
                ? (View) bottomNavigation.getParent()
                : null;

        switch (tabIndex) {
            case 1: // Gợi ý — full-screen immersive, ẩn header + footer
                if (headerContainer != null)
                    headerContainer.setVisibility(View.GONE);
                if (bottomNavWrapper != null)
                    bottomNavWrapper.setVisibility(View.VISIBLE);
                break;
            case 2: // Ngân sách
                if (headerContainer != null)
                    headerContainer.setVisibility(View.VISIBLE);
                if (bottomNavWrapper != null)
                    bottomNavWrapper.setVisibility(View.VISIBLE);
                tvHeaderTitle.setText("Quản lý ngân sách");
                if (headerRow2 != null)
                    headerRow2.setVisibility(View.GONE);
                break;
            default: // Tủ lạnh
                if (headerContainer != null)
                    headerContainer.setVisibility(View.VISIBLE);
                if (bottomNavWrapper != null)
                    bottomNavWrapper.setVisibility(View.VISIBLE);
                updateHeaderStats();
                if (headerRow2 != null)
                    headerRow2.setVisibility(View.VISIBLE);
                break;
        }
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