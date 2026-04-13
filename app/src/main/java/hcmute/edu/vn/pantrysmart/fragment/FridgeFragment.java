package hcmute.edu.vn.pantrysmart.fragment;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import hcmute.edu.vn.pantrysmart.adapter.EmojiPickerDialog;
import hcmute.edu.vn.pantrysmart.adapter.PantryItemAdapter;
import hcmute.edu.vn.pantrysmart.config.FoodEmojiConfig;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import hcmute.edu.vn.pantrysmart.R;
import hcmute.edu.vn.pantrysmart.data.local.PantrySmartDatabase;
import hcmute.edu.vn.pantrysmart.data.local.dao.PantryItemDao;
import hcmute.edu.vn.pantrysmart.data.local.entity.PantryItem;

/**
 * FridgeFragment — Tab "Tủ lạnh"
 * Fridge Visual 3D (Ngăn đông + Ngăn chính + mở cửa xem kệ thực phẩm)
 * Stats Row (Tổng / Sắp hết hạn / Đã hết hạn)
 */
public class FridgeFragment extends Fragment {

    private PantrySmartDatabase db;
    private PantryItemDao pantryDao;

    // Views — Stats
    private TextView tvStatTotal, tvStatExpiring, tvStatExpired;

    // Views — Fridge doors & badges
    private TextView badgeFreezerCount, badgeFreezerWarning;
    private TextView badgeMainCount, badgeMainWarning;
    private LinearLayout doorFreezer, doorMain;

    // Views — Fridge interior (shelves)
    private LinearLayout interiorFreezer, interiorMain;
    private LinearLayout freezerShelf1, freezerShelf2;
    private LinearLayout mainShelf1, mainShelf2, mainShelf3;
    private TextView tvFreezerEmpty, tvMainEmpty;
    private TextView btnCloseFreezer, btnCloseMain;
    private TextView btnViewAllFreezer, btnViewAllMain;

    private boolean freezerOpen = false;
    private boolean mainOpen = false;
    private boolean fabMenuOpen = false;

    // Views — FAB
    private FrameLayout fabMain;
    private TextView fabMainIcon;
    private LinearLayout fabMenuItems;
    private View fabOverlay;
    private LinearLayout fabItemAI, fabItemScan, fabItemManual;

    // Cached items for shelf population
    private List<PantryItem> cachedFreezerItems = new ArrayList<>();
    private List<PantryItem> cachedMainItems = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_fridge, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = PantrySmartDatabase.getInstance(requireContext());
        pantryDao = db.pantryItemDao();

        // Bind views
        tvStatTotal = view.findViewById(R.id.tvStatTotal);
        tvStatExpiring = view.findViewById(R.id.tvStatExpiring);
        tvStatExpired = view.findViewById(R.id.tvStatExpired);

        // Badges
        badgeFreezerCount = view.findViewById(R.id.badgeFreezerCount);
        badgeFreezerWarning = view.findViewById(R.id.badgeFreezerWarning);
        badgeMainCount = view.findViewById(R.id.badgeMainCount);
        badgeMainWarning = view.findViewById(R.id.badgeMainWarning);

        // Doors
        doorFreezer = view.findViewById(R.id.doorFreezer);
        doorMain = view.findViewById(R.id.doorMain);

        // Interior views
        interiorFreezer = view.findViewById(R.id.interiorFreezer);
        interiorMain = view.findViewById(R.id.interiorMain);
        freezerShelf1 = view.findViewById(R.id.freezerShelf1);
        freezerShelf2 = view.findViewById(R.id.freezerShelf2);
        mainShelf1 = view.findViewById(R.id.mainShelf1);
        mainShelf2 = view.findViewById(R.id.mainShelf2);
        mainShelf3 = view.findViewById(R.id.mainShelf3);
        tvFreezerEmpty = view.findViewById(R.id.tvFreezerEmpty);
        tvMainEmpty = view.findViewById(R.id.tvMainEmpty);
        btnCloseFreezer = view.findViewById(R.id.btnCloseFreezer);
        btnCloseMain = view.findViewById(R.id.btnCloseMain);

        // View All buttons
        btnViewAllFreezer = view.findViewById(R.id.btnViewAllFreezer);
        btnViewAllMain = view.findViewById(R.id.btnViewAllMain);

        // Door tap → open interior
        doorFreezer.setOnClickListener(v -> toggleFreezerDoor());
        doorMain.setOnClickListener(v -> toggleMainDoor());

        // Close buttons
        btnCloseFreezer.setOnClickListener(v -> toggleFreezerDoor());
        btnCloseMain.setOnClickListener(v -> toggleMainDoor());

        // View All listeners
        btnViewAllFreezer.setOnClickListener(v -> showAllItemsDialog(cachedFreezerItems, "Ngăn Đông", "#3A7AB5"));
        btnViewAllMain.setOnClickListener(v -> showAllItemsDialog(cachedMainItems, "Ngăn Chính", "#A06828"));

        // FAB
        setupFab(view);

        loadItems();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadItems();
    }

    // Hàm xử lý khi nhấn vào cửa Ngăn Đông: Animation ẩn cửa, hiện nội dung kệ và
    // ngược lại
    private void toggleFreezerDoor() {
        freezerOpen = !freezerOpen;
        if (freezerOpen) {
            animateViewOut(doorFreezer, () -> {
                doorFreezer.setVisibility(View.GONE);
                populateShelves(cachedFreezerItems, freezerShelf1, freezerShelf2, null, tvFreezerEmpty);
                btnViewAllFreezer.setVisibility(!cachedFreezerItems.isEmpty() ? View.VISIBLE : View.GONE);
                interiorFreezer.setAlpha(0f);
                interiorFreezer.setVisibility(View.VISIBLE);
                animateViewIn(interiorFreezer);
            });
        } else {
            animateViewOut(interiorFreezer, () -> {
                interiorFreezer.setVisibility(View.GONE);
                btnViewAllFreezer.setVisibility(View.GONE);
                doorFreezer.setAlpha(0f);
                doorFreezer.setVisibility(View.VISIBLE);
                animateViewIn(doorFreezer);
            });
        }
    }

    // Hàm xử lý khi nhấn vào cửa Ngăn Chính: Animation ẩn cửa, hiện nội dung kệ và
    // ngược lại
    private void toggleMainDoor() {
        mainOpen = !mainOpen;
        if (mainOpen) {
            animateViewOut(doorMain, () -> {
                doorMain.setVisibility(View.GONE);
                populateShelves(cachedMainItems, mainShelf1, mainShelf2, mainShelf3, tvMainEmpty);
                btnViewAllMain.setVisibility(!cachedMainItems.isEmpty() ? View.VISIBLE : View.GONE);
                interiorMain.setAlpha(0f);
                interiorMain.setVisibility(View.VISIBLE);
                animateViewIn(interiorMain);
            });
        } else {
            animateViewOut(interiorMain, () -> {
                interiorMain.setVisibility(View.GONE);
                btnViewAllMain.setVisibility(View.GONE);
                doorMain.setAlpha(0f);
                doorMain.setVisibility(View.VISIBLE);
                animateViewIn(doorMain);
            });
        }
    }

    // Hàm phân bổ danh sách thực phẩm vào các kệ của tủ lạnh (tối đa 4-5 món mỗi
    // kệ)
    private int populateShelves(List<PantryItem> items,
            LinearLayout shelf1, LinearLayout shelf2,
            LinearLayout shelf3, TextView emptyView) {
        shelf1.removeAllViews();
        shelf2.removeAllViews();
        if (shelf3 != null)
            shelf3.removeAllViews();

        if (items.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            addEmptySlots(shelf1, 4);
            addEmptySlots(shelf2, 4);
            if (shelf3 != null)
                addEmptySlots(shelf3, 4);
            return 0;
        }

        emptyView.setVisibility(View.GONE);
        int maxPerShelf = shelf3 != null ? 5 : 4;
        int maxTotal = shelf3 != null ? maxPerShelf * 3 : maxPerShelf * 2;
        long now = System.currentTimeMillis();

        for (int i = 0; i < items.size(); i++) {
            PantryItem item = items.get(i);
            View foodCircle = createFoodCircle(item, now);

            LinearLayout targetShelf;
            if (shelf3 != null) {
                if (i < maxPerShelf)
                    targetShelf = shelf1;
                else if (i < maxPerShelf * 2)
                    targetShelf = shelf2;
                else
                    targetShelf = shelf3;
            } else {
                if (i < maxPerShelf)
                    targetShelf = shelf1;
                else
                    targetShelf = shelf2;
            }

            if (targetShelf.getChildCount() < maxPerShelf) {
                targetShelf.addView(foodCircle);
            }
        }

        fillEmptySlots(shelf1, maxPerShelf);
        fillEmptySlots(shelf2, maxPerShelf);
        if (shelf3 != null)
            fillEmptySlots(shelf3, maxPerShelf);

        return Math.min(items.size(), maxTotal);
    }

    // Hàm tạo view hiển thị thực phẩm (vòng tròn chứa Emoji), đổi màu viền
    private View createFoodCircle(PantryItem item, long now) {
        boolean isExpiring = false;
        if (item.getExpiryDate() != null) {
            long daysLeft = TimeUnit.MILLISECONDS.toDays(item.getExpiryDate() - now);
            isExpiring = daysLeft <= 2;
        }

        LinearLayout container = new LinearLayout(requireContext());
        container.setOrientation(LinearLayout.VERTICAL);
        container.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams containerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        containerParams.setMargins(dp(4), 0, dp(4), 0);
        container.setLayoutParams(containerParams);

        // Emoji circle
        TextView emojiView = new TextView(requireContext());
        emojiView.setLayoutParams(new LinearLayout.LayoutParams(dp(42), dp(42)));
        emojiView.setText(FoodEmojiConfig.safeEmoji(item.getEmoji()));
        emojiView.setTextSize(22);
        emojiView.setGravity(Gravity.CENTER);
        emojiView.setElevation(dp(2));
        emojiView.setBackgroundResource(isExpiring ? R.drawable.bg_food_on_shelf_warning : R.drawable.bg_food_on_shelf);

        emojiView.setOnClickListener(v -> Toast.makeText(requireContext(),
                item.getEmoji() + " " + item.getName()
                        + " — " + formatQuantity(item.getQuantity()) + " " + item.getUnit(),
                Toast.LENGTH_SHORT).show());
        container.addView(emojiView);

        // Name label
        TextView label = new TextView(requireContext());
        label.setText(truncate(item.getName(), 6));
        label.setTextSize(8);
        label.setTextColor(isExpiring ? Color.parseColor("#EA580C") : Color.parseColor("#6B7280"));
        label.setGravity(Gravity.CENTER);
        label.setPadding(0, dp(2), 0, 0);
        container.addView(label);

        return container;
    }

    // Hàm thêm số lượng vòng tròn rỗng nét đứt (empty slots) vào kệ
    private void addEmptySlots(LinearLayout shelf, int count) {
        for (int i = 0; i < count; i++) {
            View slot = new View(requireContext());
            LinearLayout.LayoutParams slotParams = new LinearLayout.LayoutParams(dp(42), dp(42));
            slotParams.setMargins(dp(4), 0, dp(4), 0);
            slot.setLayoutParams(slotParams);
            slot.setBackgroundResource(R.drawable.bg_shelf_empty_slot);
            shelf.addView(slot);
        }
    }

    // Hàm kiểm tra và lấp đầy chỗ trống bằng các empty slots cho đến khi đủ số
    // lượng tối đa của kệ
    private void fillEmptySlots(LinearLayout shelf, int maxPerShelf) {
        int remaining = maxPerShelf - shelf.getChildCount();
        if (remaining > 0)
            addEmptySlots(shelf, remaining);
    }

    // Hàm thực hiện Animation làm mờ và thu nhỏ view (Fade out & Scale down)
    private void animateViewOut(View view, Runnable onEnd) {
        view.animate()
                .alpha(0f).scaleX(0.95f).scaleY(0.95f)
                .setDuration(200)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .withEndAction(onEnd).start();
    }

    // Hàm thực hiện Animation hiện rõ và phình to view (Fade in & Scale up)
    private void animateViewIn(View view) {
        view.setScaleX(0.95f);
        view.setScaleY(0.95f);
        view.animate()
                .alpha(1f).scaleX(1f).scaleY(1f)
                .setDuration(250)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();
    }

    // Hàm tải dữ liệu tổng hợp: đếm số món, số lượng hết hạn, tải danh sách và cập
    // nhật
    public void loadItems() {
        PantrySmartDatabase.databaseWriteExecutor.execute(() -> {
            int totalMain = pantryDao.countItemsByZone("MAIN");
            int totalFreezer = pantryDao.countItemsByZone("FREEZER");
            long now = System.currentTimeMillis();
            long twoDays = TimeUnit.DAYS.toMillis(2);
            List<PantryItem> expiring = pantryDao.getExpiringItems(now, twoDays);

            List<PantryItem> freezerItems = pantryDao.getItemsByZone("FREEZER");
            List<PantryItem> mainItems = pantryDao.getItemsByZone("MAIN");

            List<PantryItem> allItems = pantryDao.getAllActiveItems();
            int expiredCount = 0;
            int expiringFreezer = 0, expiringMain = 0;
            for (PantryItem item : allItems) {
                if (item.getExpiryDate() != null && item.getExpiryDate() < now) {
                    expiredCount++;
                }
            }
            for (PantryItem item : expiring) {
                if ("FREEZER".equals(item.getStorageZone()))
                    expiringFreezer++;
                else
                    expiringMain++;
            }
            final int finalExpiredCount = expiredCount;
            final int finalExpiringFreezer = expiringFreezer;
            final int finalExpiringMain = expiringMain;

            if (getActivity() == null)
                return;
            getActivity().runOnUiThread(() -> {
                int totalItems = totalMain + totalFreezer;

                // Cache for shelves
                cachedFreezerItems = freezerItems;
                cachedMainItems = mainItems;

                // Fridge Visual badges
                badgeFreezerCount.setText(totalFreezer + " món");
                badgeMainCount.setText(totalMain + " món");
                if (finalExpiringFreezer > 0) {
                    badgeFreezerWarning.setText("⚠️ " + finalExpiringFreezer);
                    badgeFreezerWarning.setVisibility(View.VISIBLE);
                } else {
                    badgeFreezerWarning.setVisibility(View.GONE);
                }
                if (finalExpiringMain > 0) {
                    badgeMainWarning.setText("⚠️ " + finalExpiringMain);
                    badgeMainWarning.setVisibility(View.VISIBLE);
                } else {
                    badgeMainWarning.setVisibility(View.GONE);
                }

                // Refresh shelves if open
                if (freezerOpen) {
                    populateShelves(cachedFreezerItems, freezerShelf1, freezerShelf2, null, tvFreezerEmpty);
                    btnViewAllFreezer.setVisibility(!cachedFreezerItems.isEmpty() ? View.VISIBLE : View.GONE);
                }
                if (mainOpen) {
                    populateShelves(cachedMainItems, mainShelf1, mainShelf2, mainShelf3, tvMainEmpty);
                    btnViewAllMain.setVisibility(!cachedMainItems.isEmpty() ? View.VISIBLE : View.GONE);
                }

                // Stats Row
                tvStatTotal.setText(String.valueOf(totalItems));
                tvStatExpiring.setText(String.valueOf(expiring.size()));
                tvStatExpired.setText(String.valueOf(finalExpiredCount));
            });
        });
    }

    // Hàm tiện ích chuyển đổi px sang dp hỗ trợ responsive
    private int dp(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density + 0.5f);
    }

    // Hàm format số lượng, bỏ phần thập phân nếu bằng 0 (ví dụ: 1.0 -> 1)
    private String formatQuantity(double qty) {
        if (qty == (long) qty)
            return String.valueOf((long) qty);
        return String.valueOf(qty);
    }

    // Hàm cắt ngắn chuỗi tên thực phẩm nếu quá dài và thêm dấu 3 chấm "…"
    private String truncate(String text, int maxLen) {
        if (text == null)
            return "";
        return text.length() > maxLen ? text.substring(0, maxLen) + "…" : text;
    }

    /**
     * Hiển thị BottomSheet dialog liệt kê toàn bộ thực phẩm trong ngăn.
     */
    private void showAllItemsDialog(List<PantryItem> items, String title, String colorHex) {

        if (getContext() == null)
            return;

        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        View content = getLayoutInflater().inflate(R.layout.dialog_item_list, null);
        dialog.setContentView(content);

        // Cấu hình window kiểu BottomSheet
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setGravity(Gravity.BOTTOM);
            dialog.getWindow().getAttributes().windowAnimations = android.R.style.Animation_InputMethod;
        }

        // Bind header views
        boolean isFreezer = colorHex.equals("#3A7AB5");
        TextView dialogTitle = content.findViewById(R.id.dialogTitle);
        TextView dialogSubtitle = content.findViewById(R.id.dialogSubtitle);
        ImageView dialogZoneIcon = content.findViewById(R.id.dialogZoneIcon);
        FrameLayout btnClose = content.findViewById(R.id.dialogBtnClose);
        RecyclerView recyclerView = content.findViewById(R.id.dialogRecyclerView);

        dialogTitle.setText(isFreezer ? "Ngăn Đông" : "Ngăn Chính");
        dialogTitle.setTextColor(Color.parseColor(colorHex));
        dialogSubtitle.setText(items.size() + " thực phẩm");
        dialogZoneIcon.setImageResource(isFreezer ? R.drawable.ic_zone_freezer : R.drawable.ic_zone_main);
        btnClose.setOnClickListener(v -> dialog.dismiss());

        // Thiết lập RecyclerView + Adapter
        PantryItemAdapter adapter = new PantryItemAdapter(requireContext(), items);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
        recyclerView.setHasFixedSize(false);

        // Xử lý nút Edit / Delete
        adapter.setOnItemActionListener(new PantryItemAdapter.OnItemActionListener() {
            @Override
            public void onEdit(PantryItem item, int position) {
                showEditItemBottomSheet(item, position, adapter, dialogSubtitle);
            }

            @Override
            public void onDelete(PantryItem item, int position) {
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Xác nhận xóa")
                        .setMessage("Bạn có chắc muốn xóa '" + item.getName() + "' khỏi tủ lạnh?")
                        .setPositiveButton("Xóa", (dialogInterface, i) -> {
                            PantrySmartDatabase.databaseWriteExecutor.execute(() -> {
                                pantryDao.delete(item);
                                if (getActivity() != null) {
                                    getActivity().runOnUiThread(() -> {
                                        adapter.removeItem(position);
                                        dialogSubtitle.setText(adapter.getItemCount() + " thực phẩm");
                                        loadItems(); // cập nhật badge + kệ
                                        Toast.makeText(requireContext(), "Đã xóa thành công!", Toast.LENGTH_SHORT)
                                                .show();
                                    });
                                }
                            });
                        })
                        .setNegativeButton("Hủy", null)
                        .show();
            }
        });

        dialog.show();
    }

    // Hiển thị BottomSheet Diaglog chỉnh sửa thông tin
    private void showEditItemBottomSheet(PantryItem item, int position,
            PantryItemAdapter adapter,
            TextView dialogSubtitle) {

        // 1. cấu hình layout bottom
        BottomSheetDialog editDialog = new BottomSheetDialog(requireContext());
        View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_edit_item, null);
        editDialog.setContentView(sheetView);

        // Header
        FrameLayout btnClose = sheetView.findViewById(R.id.btnCloseEditSheet);
        // Row 1: Emoji + Tên
        FrameLayout btnSelectEmoji = sheetView.findViewById(R.id.btnSelectEmoji);
        TextView tvSelectedEmoji = sheetView.findViewById(R.id.tvSelectedEmoji);
        EditText etItemName = sheetView.findViewById(R.id.etItemName);
        // Row 2: Số lượng + Đơn vị
        EditText etItemQuantity = sheetView.findViewById(R.id.etItemQuantity);
        EditText etItemUnit = sheetView.findViewById(R.id.etItemUnit);
        // Row 3: Hạn sử dụng
        TextView tvExpiryDate = sheetView.findViewById(R.id.tvExpiryDate);
        // Row 4: Danh mục chips
        TextView chipDairy = sheetView.findViewById(R.id.chipDairy);
        TextView chipVegetable = sheetView.findViewById(R.id.chipVegetable);
        TextView chipFruit = sheetView.findViewById(R.id.chipFruit);
        TextView chipMeat = sheetView.findViewById(R.id.chipMeat);
        TextView chipSeafood = sheetView.findViewById(R.id.chipSeafood);
        TextView chipDrink = sheetView.findViewById(R.id.chipDrink);
        TextView chipSpice = sheetView.findViewById(R.id.chipSpice);
        TextView chipOther = sheetView.findViewById(R.id.chipOther);
        // Row 5: Ngăn chứa
        TextView btnZoneMain = sheetView.findViewById(R.id.btnZoneMain);
        TextView btnZoneFreezer = sheetView.findViewById(R.id.btnZoneFreezer);
        // Nút Lưu
        TextView btnSaveEdit = sheetView.findViewById(R.id.btnSaveEdit);

        // 2. Refill dữ liệu vào form

        // Emoji
        tvSelectedEmoji.setText(FoodEmojiConfig.safeEmoji(item.getEmoji()));

        // Tên
        etItemName.setText(item.getName());

        // Số lượng — bỏ phần thập phân nếu là số nguyên
        double qty = item.getQuantity();
        etItemQuantity.setText(qty == (long) qty ? String.valueOf((long) qty) : String.valueOf(qty));

        // Đơn vị
        etItemUnit.setText(item.getUnit());

        // Hạn sử dụng
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        Calendar expiryCal = Calendar.getInstance();
        if (item.getExpiryDate() != null) {
            expiryCal.setTimeInMillis(item.getExpiryDate());
            tvExpiryDate.setText(sdf.format(expiryCal.getTime()));
        }

        // 3. Logic chọn
        // Mảng các chip view — thứ tự tương ứng với mảng categoryKeys
        TextView[] categoryChips = {
                chipDairy, chipVegetable, chipFruit, chipMeat,
                chipSeafood, chipDrink, chipSpice, chipOther
        };

        // Mảng key lưu vào DB — khớp thứ tự với categoryChips
        String[] categoryKeys = {
                "DAIRY", "VEGETABLE", "FRUIT", "MEAT",
                "SEAFOOD", "DRINK", "SPICE", "OTHER"
        };

        // Biến lưu danh mục đang được chọn
        final String[] selectedCategory = { item.getCategory() != null ? item.getCategory() : "OTHER" };

        // Pre-fill: highlight chip ban đầu dựa theo item.getCategory()
        highlightCategoryChip(categoryChips, categoryKeys, selectedCategory[0]);

        // Gán listener cho từng chip
        for (int i = 0; i < categoryChips.length; i++) {
            final int index = i;
            categoryChips[i].setOnClickListener(v -> {
                selectedCategory[0] = categoryKeys[index];
                highlightCategoryChip(categoryChips, categoryKeys, categoryKeys[index]);
            });
        }

        // Logic chọn ngăn chứa
        final String[] selectedZone = { item.getStorageZone() != null ? item.getStorageZone() : "MAIN" };

        // Hàm highlight zone button
        Runnable updateZoneUI = () -> {
            if ("FREEZER".equals(selectedZone[0])) {
                btnZoneFreezer.setBackgroundResource(R.drawable.bg_edit_zone_active);
                btnZoneFreezer.setTextColor(Color.WHITE);
                btnZoneMain.setBackgroundResource(R.drawable.bg_edit_zone_inactive);
                btnZoneMain.setTextColor(Color.parseColor("#4A5565"));
            } else {
                btnZoneMain.setBackgroundResource(R.drawable.bg_edit_zone_active);
                btnZoneMain.setTextColor(Color.WHITE);
                btnZoneFreezer.setBackgroundResource(R.drawable.bg_edit_zone_inactive);
                btnZoneFreezer.setTextColor(Color.parseColor("#4A5565"));
            }
        };
        updateZoneUI.run();
        // Pre-fill

        btnZoneMain.setOnClickListener(v -> {
            selectedZone[0] = "MAIN";
            updateZoneUI.run();
        });
        btnZoneFreezer.setOnClickListener(v -> {
            selectedZone[0] = "FREEZER";
            updateZoneUI.run();
        });

        // Date
        tvExpiryDate.setOnClickListener(v -> {
            new DatePickerDialog(requireContext(), (view, year, month, dayOfMonth) -> {
                expiryCal.set(year, month, dayOfMonth, 23, 59, 59);
                tvExpiryDate.setText(sdf.format(expiryCal.getTime()));
            },
                    expiryCal.get(Calendar.YEAR),
                    expiryCal.get(Calendar.MONTH),
                    expiryCal.get(Calendar.DAY_OF_MONTH)).show();
        });

        // Emoji — mở EmojiPickerDialog theo nhóm
        final String[] selectedEmoji = { FoodEmojiConfig.safeEmoji(item.getEmoji()) };

        btnSelectEmoji.setOnClickListener(v -> {
            EmojiPickerDialog.show(requireContext(), selectedEmoji[0], emoji -> {
                selectedEmoji[0] = emoji;
                tvSelectedEmoji.setText(emoji);
            });
        });

        // LOGIC LƯU
        btnSaveEdit.setOnClickListener(v -> {
            // === Validate ===
            String name = etItemName.getText().toString().trim();
            if (name.isEmpty()) {
                etItemName.setError("Vui lòng nhập tên");
                return;
            }

            String qtyStr = etItemQuantity.getText().toString().trim();
            double quantity;
            try {
                quantity = Double.parseDouble(qtyStr);
                if (quantity <= 0)
                    throw new NumberFormatException();
            } catch (NumberFormatException e) {
                etItemQuantity.setError("Số lượng không hợp lệ");
                return;
            }

            String unit = etItemUnit.getText().toString().trim();
            if (unit.isEmpty()) {
                etItemUnit.setError("Vui lòng nhập đơn vị");
                return;
            }

            // === Gán dữ liệu mới vào item (giữ nguyên id để Room update đúng row) ===
            item.setName(name);
            item.setEmoji(selectedEmoji[0]);
            item.setQuantity(quantity);
            item.setUnit(unit);
            item.setCategory(selectedCategory[0]);
            item.setStorageZone(selectedZone[0]);

            // Hạn sử dụng — chỉ set nếu user đã chọn ngày
            if (tvExpiryDate.getText() != null && !tvExpiryDate.getText().toString().isEmpty()
                    && !"Chọn ngày".equals(tvExpiryDate.getText().toString())) {
                item.setExpiryDate(expiryCal.getTimeInMillis());
            }

            // Disable nút tránh double-tap
            btnSaveEdit.setEnabled(false);
            btnSaveEdit.setText("Đang lưu...");

            // === Update Database trên Background Thread ===
            PantrySmartDatabase.databaseWriteExecutor.execute(() -> {
                pantryDao.update(item);

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        // Cập nhật dòng tương ứng trên RecyclerView (không cần xóa rồi thêm lại)
                        adapter.notifyItemChanged(position);

                        // Cập nhật subtitle trên dialog cha (nếu zone đổi thì số lượng có thể thay đổi)
                        // dialogSubtitle.setText(adapter.getItemCount() + " thực phẩm");

                        // Reload tủ lạnh visual + dashboard
                        loadItems();

                        Toast.makeText(requireContext(), "✅ Đã cập nhật!", Toast.LENGTH_SHORT).show();

                        editDialog.dismiss();
                    });
                }
            });
        });

        // CLOSE
        btnClose.setOnClickListener(v -> editDialog.dismiss());
        editDialog.show();

    }

    // Hàm chung để highlight chip active và reset các chip khác
    private void highlightCategoryChip(TextView[] chips, String[] keys, String activeKey) {
        for (int i = 0; i < chips.length; i++) {
            if (keys[i].equals(activeKey)) {
                chips[i].setBackgroundResource(R.drawable.bg_edit_category_chip_active);
                chips[i].setTextColor(Color.WHITE);
            } else {
                chips[i].setBackgroundResource(R.drawable.bg_edit_category_chip);
                chips[i].setTextColor(Color.parseColor("#4A5565"));
            }
        }
    }

    // FAB MENU

    private void setupFab(View root) {
        fabMain = root.findViewById(R.id.fabMain);
        fabMainIcon = root.findViewById(R.id.fabMainIcon);
        fabMenuItems = root.findViewById(R.id.fabMenuItems);
        fabOverlay = root.findViewById(R.id.fabOverlay);
        fabItemAI = root.findViewById(R.id.fabItemAI);
        fabItemScan = root.findViewById(R.id.fabItemScan);
        fabItemManual = root.findViewById(R.id.fabItemManual);

        fabMain.setOnClickListener(v -> toggleFabMenu());
        fabOverlay.setOnClickListener(v -> toggleFabMenu());

        fabItemAI.setOnClickListener(v -> {
            toggleFabMenu();
            Toast.makeText(requireContext(), "Nhận diện AI - Sắp ra mắt", Toast.LENGTH_SHORT).show();
        });

        fabItemScan.setOnClickListener(v -> {
            toggleFabMenu();
            Toast.makeText(requireContext(), "Quét hóa đơn - Sắp ra mắt", Toast.LENGTH_SHORT).show();
        });

        fabItemManual.setOnClickListener(v -> {
            toggleFabMenu();
            Toast.makeText(requireContext(), "Thêm thủ công", Toast.LENGTH_SHORT).show();
        });
    }

    private void toggleFabMenu() {
        fabMenuOpen = !fabMenuOpen;
        if (fabMenuOpen) {
            openFabMenu();
        } else {
            closeFabMenu();
        }
    }

    private void openFabMenu() {
        fabOverlay.setVisibility(View.VISIBLE);
        fabOverlay.setAlpha(0f);
        fabOverlay.animate().alpha(1f).setDuration(200).start();

        fabMainIcon.animate().rotation(45f).setDuration(250)
                .setInterpolator(new OvershootInterpolator()).start();

        fabMenuItems.setVisibility(View.VISIBLE);
        int count = fabMenuItems.getChildCount();
        for (int i = count - 1; i >= 0; i--) {
            View child = fabMenuItems.getChildAt(i);
            child.setAlpha(0f);
            child.setTranslationY(40f);
            child.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(250)
                    .setStartDelay((long) (count - 1 - i) * 50)
                    .setInterpolator(new OvershootInterpolator(1.2f))
                    .start();
        }
    }

    private void closeFabMenu() {
        fabOverlay.animate().alpha(0f).setDuration(200)
                .withEndAction(() -> fabOverlay.setVisibility(View.GONE)).start();

        fabMainIcon.animate().rotation(0f).setDuration(250)
                .setInterpolator(new AccelerateDecelerateInterpolator()).start();

        int count = fabMenuItems.getChildCount();
        for (int i = 0; i < count; i++) {
            View child = fabMenuItems.getChildAt(i);
            child.animate()
                    .alpha(0f)
                    .translationY(40f)
                    .setDuration(200)
                    .setStartDelay((long) i * 30)
                    .withEndAction(() -> {
                        if (!fabMenuOpen) {
                            fabMenuItems.setVisibility(View.GONE);
                        }
                    })
                    .start();
        }
    }

}