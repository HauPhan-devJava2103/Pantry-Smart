package hcmute.edu.vn.pantrysmart;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.TimeUnit;

import hcmute.edu.vn.pantrysmart.adapter.NotificationAdapter;
import hcmute.edu.vn.pantrysmart.data.local.PantrySmartDatabase;
import hcmute.edu.vn.pantrysmart.data.local.dao.PantryItemDao;
import hcmute.edu.vn.pantrysmart.data.local.entity.PantryItem;
import hcmute.edu.vn.pantrysmart.fragment.BudgetFragment;
import hcmute.edu.vn.pantrysmart.fragment.FridgeFragment;
import hcmute.edu.vn.pantrysmart.fragment.SuggestFragment;
import hcmute.edu.vn.pantrysmart.notification.NotificationHelper;
import hcmute.edu.vn.pantrysmart.worker.ExpiryCheckWorker;

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
                .setOnClickListener(v -> showNotificationBottomSheet());
        findViewById(R.id.btnProfile)
                .setOnClickListener(v -> showProfileMenu());

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

        // 1. Tạo Notification Channel
        NotificationHelper.createChannel(this);

        // 2. Đăng ký Worker kiểm tra hết hạn — chạy mỗi 24h lúc 7h sáng
        long delayTo7AM = calculateDelayTo(7, 0);
        PeriodicWorkRequest expiryWork = new PeriodicWorkRequest.Builder(
                ExpiryCheckWorker.class,
                24, TimeUnit.HOURS)
                .setInitialDelay(delayTo7AM, TimeUnit.MILLISECONDS)
                .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "expiry_check",
                ExistingPeriodicWorkPolicy.KEEP,
                expiryWork);

        // 3. Xu ly nhan Back — canh bao truoc khi thoat
        getOnBackPressedDispatcher().addCallback(this,
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        showExitConfirmDialog();
                    }
                });
    }

    // Dialog xac nhan thoat ung dung
    private void showExitConfirmDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_exit_confirm, null);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        dialogView.findViewById(R.id.btnExitCancel).setOnClickListener(v -> dialog.dismiss());
        dialogView.findViewById(R.id.btnExitConfirm).setOnClickListener(v -> {
            dialog.dismiss();
            finish();
        });

        dialog.show();
    }

    /**
     * Tính khoảng thời gian (ms) từ bây giờ đến giờ mục tiêu.
     * Nếu giờ mục tiêu đã qua hôm nay → tính đến hôm sau.
     */
    private long calculateDelayTo(int targetHour, int targetMinute) {
        Calendar now = Calendar.getInstance();
        Calendar target = Calendar.getInstance();
        target.set(Calendar.HOUR_OF_DAY, targetHour);
        target.set(Calendar.MINUTE, targetMinute);
        target.set(Calendar.SECOND, 0);
        target.set(Calendar.MILLISECOND, 0);

        // Nếu 7h sáng đã qua: đợi đến 7h sáng mai
        if (target.before(now)) {
            target.add(Calendar.DAY_OF_YEAR, 1);
        }

        return target.getTimeInMillis() - now.getTimeInMillis();
    }

    // Menu tài khoản — BottomSheet hiện đại
    private void showProfileMenu() {
        com.google.android.material.bottomsheet.BottomSheetDialog sheet = new com.google.android.material.bottomsheet.BottomSheetDialog(
                this);
        View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_profile, null);
        sheet.setContentView(sheetView);

        // Background trong suốt để thấy bo góc
        if (sheet.getWindow() != null) {
            sheet.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        // Lịch sử nấu ăn
        sheetView.findViewById(R.id.menuCookingHistory).setOnClickListener(v -> {
            sheet.dismiss();
            startActivity(new android.content.Intent(this, CookingHistoryActivity.class));
        });

        sheet.show();
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
    public void updateHeaderStats() {
        PantrySmartDatabase.databaseWriteExecutor.execute(() -> {
            int totalMain = pantryDao.countItemsByZone("MAIN");
            int totalFreezer = pantryDao.countItemsByZone("FREEZER");
            long now = System.currentTimeMillis();
            long twoDays = TimeUnit.DAYS.toMillis(2);
            int expiringCount = pantryDao.getExpiringItems(now, twoDays).size();
            int expiredCount = pantryDao.getExpiredItems(now).size();
            int notifTotal = expiringCount + expiredCount;

            runOnUiThread(() -> {
                int total = totalMain + totalFreezer;
                tvHeaderTitle.setText(getString(R.string.pantry_item_count, total));

                // Expiry badge (thanh cảnh báo)
                if (expiringCount == 0 && expiredCount == 0) {
                    tvExpiryBadge.setText("An toàn");
                } else if (expiredCount > 0 && expiringCount > 0) {
                    tvExpiryBadge.setText(expiredCount + " hết hạn · " + expiringCount + " sắp hết");
                } else if (expiredCount > 0) {
                    tvExpiryBadge.setText(expiredCount + " hết hạn");
                } else {
                    tvExpiryBadge.setText(expiringCount + " sắp hết");
                }

                // Notification badge (số trên nút chuông)
                if (notifTotal == 0) {
                    tvNotificationBadge.setVisibility(View.GONE);
                } else {
                    tvNotificationBadge.setText(String.valueOf(notifTotal));
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

    /**
     * Hiển thị BottomSheet danh sách thông báo hết hạn / sắp hết hạn.
     */
    private void showNotificationBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_notifications, null);
        dialog.setContentView(sheetView);

        TextView tvCount = sheetView.findViewById(R.id.tvNotifCount);
        LinearLayout layoutEmpty = sheetView.findViewById(R.id.layoutNotifEmpty);
        RecyclerView rvNotifications = sheetView.findViewById(R.id.rvNotifications);
        sheetView.findViewById(R.id.btnCloseNotif).setOnClickListener(v -> dialog.dismiss());

        // Query dữ liệu trên background thread
        PantrySmartDatabase.databaseWriteExecutor.execute(() -> {
            long now = System.currentTimeMillis();
            long threshold = now + TimeUnit.DAYS.toMillis(2);

            // Lấy item đã hết hạn
            List<PantryItem> expiredItems = pantryDao.getExpiredItems(now);
            // Lấy item sắp hết hạn (2 ngày tới)
            List<PantryItem> expiringItems = pantryDao.getExpiringItems(now,
                    TimeUnit.DAYS.toMillis(2));

            // Gộp thành danh sách NotifItem
            List<NotificationAdapter.NotifItem> notifList = new ArrayList<>();

            for (PantryItem item : expiredItems) {
                long daysAgo = TimeUnit.MILLISECONDS.toDays(now - item.getExpiryDate());
                notifList.add(new NotificationAdapter.NotifItem(item, true, -daysAgo));
            }
            for (PantryItem item : expiringItems) {
                long daysLeft = TimeUnit.MILLISECONDS.toDays(
                        item.getExpiryDate() - now);
                notifList.add(new NotificationAdapter.NotifItem(item, false, daysLeft));
            }

            // Cập nhật UI trên main thread
            runOnUiThread(() -> {
                if (notifList.isEmpty()) {
                    layoutEmpty.setVisibility(View.VISIBLE);
                    rvNotifications.setVisibility(View.GONE);
                    tvCount.setText("0");
                } else {
                    layoutEmpty.setVisibility(View.GONE);
                    rvNotifications.setVisibility(View.VISIBLE);
                    tvCount.setText(String.valueOf(notifList.size()));

                    NotificationAdapter adapter = new NotificationAdapter(
                            MainActivity.this, notifList);
                    rvNotifications.setLayoutManager(
                            new LinearLayoutManager(MainActivity.this));
                    rvNotifications.setAdapter(adapter);
                }
            });
        });

        dialog.show();
    }
}