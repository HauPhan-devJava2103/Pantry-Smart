package hcmute.edu.vn.pantrysmart.fragment.helper;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import hcmute.edu.vn.pantrysmart.R;
import hcmute.edu.vn.pantrysmart.adapter.EmojiPickerDialog;
import hcmute.edu.vn.pantrysmart.adapter.PantryItemAdapter;
import hcmute.edu.vn.pantrysmart.config.FoodIconConfig;
import hcmute.edu.vn.pantrysmart.data.local.PantrySmartDatabase;
import hcmute.edu.vn.pantrysmart.data.local.dao.PantryItemDao;
import hcmute.edu.vn.pantrysmart.data.local.entity.PantryItem;

/**
 * Helper quản lý các Dialog của FridgeFragment:
 * - Dialog xem toàn bộ thực phẩm (showAllItemsDialog)
 * - BottomSheet chỉnh sửa thông tin (showEditItemBottomSheet)
 */
public class FridgeDialogHelper {

    private final Fragment fragment;
    private final PantryItemDao pantryDao;
    private final Runnable onDataChanged;

    /**
     * @param fragment      Fragment cha (dùng lấy Context, LayoutInflater,
     *                      Activity)
     * @param pantryDao     DAO thao tác với bảng pantry_items
     * @param onDataChanged Callback khi dữ liệu thay đổi (VD: loadItems())
     */
    public FridgeDialogHelper(Fragment fragment, PantryItemDao pantryDao, Runnable onDataChanged) {
        this.fragment = fragment;
        this.pantryDao = pantryDao;
        this.onDataChanged = onDataChanged;
    }

    /**
     * Hiển thị Dialog liệt kê toàn bộ thực phẩm trong ngăn.
     */
    public void showAllItemsDialog(List<PantryItem> items,
            String title, String colorHex) {
        if (fragment.getContext() == null)
            return;

        Dialog dialog = new Dialog(fragment.requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        View content = fragment.getLayoutInflater().inflate(R.layout.dialog_item_list, null);
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
        dialogZoneIcon.setImageResource(
                isFreezer ? R.drawable.ic_zone_freezer : R.drawable.ic_zone_main);
        btnClose.setOnClickListener(v -> dialog.dismiss());

        // Thiết lập RecyclerView + Adapter
        PantryItemAdapter adapter = new PantryItemAdapter(fragment.requireContext(), items);
        recyclerView.setLayoutManager(new LinearLayoutManager(fragment.requireContext()));
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
                new MaterialAlertDialogBuilder(fragment.requireContext())
                        .setTitle("Xác nhận xóa")
                        .setMessage("Bạn có chắc muốn xóa '"
                                + item.getName() + "' khỏi tủ lạnh?")
                        .setPositiveButton("Xóa", (dialogInterface, i) -> {
                            PantrySmartDatabase.databaseWriteExecutor.execute(() -> {
                                pantryDao.delete(item);
                                if (fragment.getActivity() != null) {
                                    fragment.getActivity().runOnUiThread(() -> {
                                        adapter.removeItem(position);
                                        dialogSubtitle.setText(
                                                adapter.getItemCount() + " thực phẩm");
                                        onDataChanged.run();
                                        Toast.makeText(fragment.requireContext(),
                                                "Đã xóa thành công!",
                                                Toast.LENGTH_SHORT).show();
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

    /**
     * Hiển thị BottomSheet để thêm mới thực phẩm thủ công.
     */
    public void showAddItemBottomSheet() {
        if (fragment.getContext() == null)
            return;

        // 1. Khởi tạo đối tượng mới với các giá trị mặc định
        PantryItem newItem = new PantryItem();
        newItem.setEmoji("ic_food_package"); // Icon mặc định
        newItem.setQuantity(1.0);
        newItem.setUnit("kg");
        newItem.setCategory("OTHER");
        newItem.setStorageZone("MAIN");

        BottomSheetDialog addDialog = new BottomSheetDialog(fragment.requireContext());
        View sheetView = fragment.getLayoutInflater().inflate(R.layout.bottom_sheet_edit_item, null);
        addDialog.setContentView(sheetView);

        // 2. Bind views (Dùng chung layout với Edit)
        TextView tvSheetTitle = sheetView.findViewById(R.id.tvEditSheetTitle);
        if (tvSheetTitle != null)
            tvSheetTitle.setText("Thêm thực phẩm");

        FrameLayout btnClose = sheetView.findViewById(R.id.btnCloseEditSheet);
        FrameLayout btnSelectEmoji = sheetView.findViewById(R.id.btnSelectEmoji);
        ImageView imgSelectedIcon = sheetView.findViewById(R.id.imgSelectedIcon);
        EditText etItemName = sheetView.findViewById(R.id.etItemName);
        EditText etItemQuantity = sheetView.findViewById(R.id.etItemQuantity);
        EditText etItemUnit = sheetView.findViewById(R.id.etItemUnit);
        TextView tvExpiryDate = sheetView.findViewById(R.id.tvExpiryDate);
        TextView btnSave = sheetView.findViewById(R.id.btnSaveEdit);
        btnSave.setText("Thêm vào tủ");

        // Chips Danh mục
        TextView[] categoryChips = {
                sheetView.findViewById(R.id.chipDairy), sheetView.findViewById(R.id.chipVegetable),
                sheetView.findViewById(R.id.chipFruit), sheetView.findViewById(R.id.chipMeat),
                sheetView.findViewById(R.id.chipSeafood), sheetView.findViewById(R.id.chipDrink),
                sheetView.findViewById(R.id.chipSpice), sheetView.findViewById(R.id.chipOther)
        };
        String[] categoryKeys = { "DAIRY", "VEGETABLE", "FRUIT", "MEAT", "SEAFOOD", "DRINK", "SPICE", "OTHER" };

        // Ngăn chứa
        TextView btnZoneMain = sheetView.findViewById(R.id.btnZoneMain);
        TextView btnZoneFreezer = sheetView.findViewById(R.id.btnZoneFreezer);

        // 3. Setup UI ban đầu
        imgSelectedIcon.setImageResource(FoodIconConfig.safeIcon(newItem.getEmoji()));
        etItemQuantity.setText("1");
        etItemUnit.setText("kg");

        final String[] selectedCategory = { newItem.getCategory() };
        highlightCategoryChip(categoryChips, categoryKeys, selectedCategory[0]);

        final String[] selectedZone = { newItem.getStorageZone() };
        Runnable updateZoneUI = () -> {
            boolean isFreezer = "FREEZER".equals(selectedZone[0]);
            btnZoneFreezer.setBackgroundResource(
                    isFreezer ? R.drawable.bg_edit_zone_active : R.drawable.bg_edit_zone_inactive);
            btnZoneFreezer.setTextColor(isFreezer ? Color.WHITE : Color.parseColor("#4A5565"));
            btnZoneMain.setBackgroundResource(
                    !isFreezer ? R.drawable.bg_edit_zone_active : R.drawable.bg_edit_zone_inactive);
            btnZoneMain.setTextColor(!isFreezer ? Color.WHITE : Color.parseColor("#4A5565"));
        };
        updateZoneUI.run();

        // 4. Xử lý sự kiện tương tác
        for (int i = 0; i < categoryChips.length; i++) {
            final int index = i;
            categoryChips[i].setOnClickListener(v -> {
                selectedCategory[0] = categoryKeys[index];
                highlightCategoryChip(categoryChips, categoryKeys, categoryKeys[index]);
            });
        }

        btnZoneMain.setOnClickListener(v -> {
            selectedZone[0] = "MAIN";
            updateZoneUI.run();
        });
        btnZoneFreezer.setOnClickListener(v -> {
            selectedZone[0] = "FREEZER";
            updateZoneUI.run();
        });

        Calendar expiryCal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        tvExpiryDate.setOnClickListener(v -> {
            new DatePickerDialog(fragment.requireContext(), (view, year, month, dayOfMonth) -> {
                expiryCal.set(year, month, dayOfMonth, 23, 59, 59);
                tvExpiryDate.setText(sdf.format(expiryCal.getTime()));
            }, expiryCal.get(Calendar.YEAR), expiryCal.get(Calendar.MONTH), expiryCal.get(Calendar.DAY_OF_MONTH))
                    .show();
        });

        final String[] selectedEmoji = { newItem.getEmoji() };
        btnSelectEmoji.setOnClickListener(v -> {
            EmojiPickerDialog.show(fragment.requireContext(), selectedEmoji[0], emoji -> {
                selectedEmoji[0] = emoji;
                imgSelectedIcon.setImageResource(FoodIconConfig.safeIcon(emoji));
            });
        });

        // 5. Logic LƯU (INSERT)
        btnSave.setOnClickListener(v -> {
            String name = etItemName.getText().toString().trim();
            if (name.isEmpty()) {
                etItemName.setError("Nhập tên thực phẩm");
                return;
            }

            double quantity;
            try {
                quantity = Double.parseDouble(etItemQuantity.getText().toString().trim());
            } catch (Exception e) {
                etItemQuantity.setError("Số lượng lỗi");
                return;
            }

            String unit = etItemUnit.getText().toString().trim();
            if (unit.isEmpty()) {
                etItemUnit.setError("Nhập đơn vị");
                return;
            }

            newItem.setName(name);
            newItem.setEmoji(selectedEmoji[0]);
            newItem.setQuantity(quantity);
            newItem.setUnit(unit);
            newItem.setCategory(selectedCategory[0]);
            newItem.setStorageZone(selectedZone[0]);
            if (!tvExpiryDate.getText().toString().equals("Chọn ngày")) {
                newItem.setExpiryDate(expiryCal.getTimeInMillis());
            }

            btnSave.setEnabled(false);
            PantrySmartDatabase.databaseWriteExecutor.execute(() -> {
                pantryDao.insert(newItem); // Thực hiện INSERT
                if (fragment.getActivity() != null) {
                    fragment.getActivity().runOnUiThread(() -> {
                        onDataChanged.run(); // Load lại danh sách ở màn hình chính
                        Toast.makeText(fragment.requireContext(), "Đã thêm " + name, Toast.LENGTH_SHORT).show();
                        addDialog.dismiss();
                    });
                }
            });
        });

        btnClose.setOnClickListener(v -> addDialog.dismiss());
        addDialog.show();
    }

    /**
     * Hiển thị BottomSheet chỉnh sửa thông tin thực phẩm.
     */
    public void showEditItemBottomSheet(PantryItem item, int position,
            PantryItemAdapter adapter,
            TextView dialogSubtitle) {

        BottomSheetDialog editDialog = new BottomSheetDialog(fragment.requireContext());
        View sheetView = fragment.getLayoutInflater()
                .inflate(R.layout.bottom_sheet_edit_item, null);
        editDialog.setContentView(sheetView);

        // Header
        FrameLayout btnClose = sheetView.findViewById(R.id.btnCloseEditSheet);
        // Row 1: Icon + Tên
        FrameLayout btnSelectEmoji = sheetView.findViewById(R.id.btnSelectEmoji);
        ImageView imgSelectedIcon = sheetView.findViewById(R.id.imgSelectedIcon);
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

        // ---- Refill dữ liệu vào form ----

        imgSelectedIcon.setImageResource(FoodIconConfig.safeIcon(item.getEmoji()));
        etItemName.setText(item.getName());

        double qty = item.getQuantity();
        etItemQuantity.setText(
                qty == (long) qty ? String.valueOf((long) qty) : String.valueOf(qty));
        etItemUnit.setText(item.getUnit());

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        Calendar expiryCal = Calendar.getInstance();
        if (item.getExpiryDate() != null) {
            expiryCal.setTimeInMillis(item.getExpiryDate());
            tvExpiryDate.setText(sdf.format(expiryCal.getTime()));
        }

        // ---- Logic chọn danh mục ----

        TextView[] categoryChips = {
                chipDairy, chipVegetable, chipFruit, chipMeat,
                chipSeafood, chipDrink, chipSpice, chipOther
        };
        String[] categoryKeys = {
                "DAIRY", "VEGETABLE", "FRUIT", "MEAT",
                "SEAFOOD", "DRINK", "SPICE", "OTHER"
        };
        final String[] selectedCategory = {
                item.getCategory() != null ? item.getCategory() : "OTHER"
        };
        highlightCategoryChip(categoryChips, categoryKeys, selectedCategory[0]);

        for (int i = 0; i < categoryChips.length; i++) {
            final int index = i;
            categoryChips[i].setOnClickListener(v -> {
                selectedCategory[0] = categoryKeys[index];
                highlightCategoryChip(categoryChips, categoryKeys, categoryKeys[index]);
            });
        }

        // ---- Logic chọn ngăn chứa ----

        final String[] selectedZone = {
                item.getStorageZone() != null ? item.getStorageZone() : "MAIN"
        };

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

        btnZoneMain.setOnClickListener(v -> {
            selectedZone[0] = "MAIN";
            updateZoneUI.run();
        });
        btnZoneFreezer.setOnClickListener(v -> {
            selectedZone[0] = "FREEZER";
            updateZoneUI.run();
        });

        // ---- Date Picker ----

        tvExpiryDate.setOnClickListener(v -> {
            new DatePickerDialog(fragment.requireContext(), (view, year, month, dayOfMonth) -> {
                expiryCal.set(year, month, dayOfMonth, 23, 59, 59);
                tvExpiryDate.setText(sdf.format(expiryCal.getTime()));
            },
                    expiryCal.get(Calendar.YEAR),
                    expiryCal.get(Calendar.MONTH),
                    expiryCal.get(Calendar.DAY_OF_MONTH)).show();
        });

        // ---- Icon Picker ----

        final String[] selectedEmoji = { item.getEmoji() };
        btnSelectEmoji.setOnClickListener(v -> {
            EmojiPickerDialog.show(fragment.requireContext(), selectedEmoji[0], emoji -> {
                selectedEmoji[0] = emoji;
                imgSelectedIcon.setImageResource(FoodIconConfig.safeIcon(emoji));
            });
        });

        // ---- LOGIC LƯU ----

        btnSaveEdit.setOnClickListener(v -> {
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

            item.setName(name);
            item.setEmoji(selectedEmoji[0]);
            item.setQuantity(quantity);
            item.setUnit(unit);
            item.setCategory(selectedCategory[0]);
            item.setStorageZone(selectedZone[0]);

            if (tvExpiryDate.getText() != null
                    && !tvExpiryDate.getText().toString().isEmpty()
                    && !"Chọn ngày".equals(tvExpiryDate.getText().toString())) {
                item.setExpiryDate(expiryCal.getTimeInMillis());
            }

            btnSaveEdit.setEnabled(false);
            btnSaveEdit.setText("Đang lưu...");

            PantrySmartDatabase.databaseWriteExecutor.execute(() -> {
                pantryDao.update(item);
                if (fragment.getActivity() != null) {
                    fragment.getActivity().runOnUiThread(() -> {
                        adapter.notifyItemChanged(position);
                        onDataChanged.run();
                        Toast.makeText(fragment.requireContext(),
                                "Đã cập nhật!", Toast.LENGTH_SHORT).show();
                        editDialog.dismiss();
                    });
                }
            });
        });

        btnClose.setOnClickListener(v -> editDialog.dismiss());
        editDialog.show();
    }

    /** Highlight chip active và reset các chip khác. */
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

    /**
     * Hiển thị Dialog xem lại danh sách nhiều thực phẩm AI vừa nhận diện được.
     */
    public void showAIRecognitionReviewDialog(java.util.List<hcmute.edu.vn.pantrysmart.data.local.entity.PantryItem> items) {
        if (fragment.getActivity() == null || items == null || items.isEmpty()) return;

        // 1. Inflate layout review
        android.view.View dialogView = android.view.LayoutInflater.from(fragment.requireContext())
                .inflate(R.layout.dialog_review_scanned_items, null);

        // 2. Thiết lập tiêu đề và mô tả
        android.widget.TextView tvSubtitle = dialogView.findViewById(R.id.tvReviewSubtitle);
        if (tvSubtitle != null) {
            tvSubtitle.setText("Gemini đã tìm thấy " + items.size() + " thực phẩm.");
        }

        // 3. Chuyển đổi sang ScannedItem
        java.util.List<hcmute.edu.vn.pantrysmart.model.ScannedItem> scannedItems = new java.util.ArrayList<>();
        for (hcmute.edu.vn.pantrysmart.data.local.entity.PantryItem p : items) {
            hcmute.edu.vn.pantrysmart.model.ScannedItem s = new hcmute.edu.vn.pantrysmart.model.ScannedItem();
            s.setName(p.getName());
            s.setQuantity(p.getQuantity());
            s.setUnit(p.getUnit());
            s.setCategory(p.getCategory());
            s.setEmoji(p.getEmoji());
            s.setStorageZone("MAIN");
            scannedItems.add(s);
        }

        // 4. Thiết lập RecyclerView
        androidx.recyclerview.widget.RecyclerView rv = dialogView.findViewById(R.id.rvReviewScannedItems);

        final hcmute.edu.vn.pantrysmart.adapter.ScannedReviewAdapter[] adapterArr = {null};

        adapterArr[0] = new hcmute.edu.vn.pantrysmart.adapter.ScannedReviewAdapter(scannedItems, (item, position) -> {
            showEditScannedItemBottomSheet(item, position, adapterArr[0]);
        });

        hcmute.edu.vn.pantrysmart.adapter.ScannedReviewAdapter adapter = adapterArr[0];
        rv.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(fragment.requireContext()));
        rv.setAdapter(adapter);

        // 5. Khởi tạo Dialog
        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(fragment.requireContext())
                .setView(dialogView)
                .setCancelable(true)
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        // 6. Nút Thêm tất cả
        android.widget.TextView btnSaveAll = dialogView.findViewById(R.id.btnSaveAllScanned);
        if (btnSaveAll != null) {
            btnSaveAll.setText("Nạp " + scannedItems.size() + " món vào tủ lạnh");
            btnSaveAll.setOnClickListener(v -> {
                saveAIItemsToDatabase(adapter.getItems());
                dialog.dismiss();
            });
        }

        // Nút Hủy
        android.view.View btnCancel = dialogView.findViewById(R.id.btnCancelReview);
        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dialog.dismiss());
        }

        dialog.show();

        // Giới hạn chiều cao dialog
        android.util.DisplayMetrics dm = fragment.getResources().getDisplayMetrics();
        int maxHeight = (int) (dm.heightPixels * 0.85);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(android.view.WindowManager.LayoutParams.MATCH_PARENT, maxHeight);
        }
    }

    /**
     * Lưu toàn bộ danh sách thực phẩm AI đã xác nhận vào Database.
     */
    private void saveAIItemsToDatabase(java.util.List<hcmute.edu.vn.pantrysmart.model.ScannedItem> items) {
        PantrySmartDatabase.databaseWriteExecutor.execute(() -> {
            long now = System.currentTimeMillis();
            for (hcmute.edu.vn.pantrysmart.model.ScannedItem scanned : items) {
                PantryItem pantryItem = new PantryItem();
                pantryItem.setName(scanned.getName());
                pantryItem.setEmoji(scanned.getEmoji());
                pantryItem.setQuantity(scanned.getQuantity());
                pantryItem.setUnit(scanned.getUnit());
                pantryItem.setStorageZone(scanned.getStorageZone() != null ? scanned.getStorageZone() : "MAIN");
                pantryItem.setCategory(scanned.getCategory());
                pantryItem.setAddedDate(now);

                // Tính toán ngày hết hạn (Mặc định 7 ngày nếu không có dữ liệu)
                java.util.Calendar cal = java.util.Calendar.getInstance();
                int days = scanned.getExpiryDays() > 0 ? scanned.getExpiryDays() : 7;
                cal.add(java.util.Calendar.DAY_OF_YEAR, days);
                pantryItem.setExpiryDate(cal.getTimeInMillis());

                pantryItem.setActive(true);
                pantryDao.insert(pantryItem);
            }

            if (fragment.getActivity() != null) {
                fragment.getActivity().runOnUiThread(() -> {
                    onDataChanged.run(); // Load lại giao diện tủ lạnh
                    android.widget.Toast.makeText(fragment.requireContext(),
                            "✅ Đã thêm " + items.size() + " món vào tủ lạnh", android.widget.Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    /**
     * Dùng BottomSheet để chỉnh sửa TOÀN BỘ thông tin món đồ AI vừa quét (cho phép sửa ngày, danh mục, ngăn chứa)
     */
    private void showEditScannedItemBottomSheet(hcmute.edu.vn.pantrysmart.model.ScannedItem item, int position, hcmute.edu.vn.pantrysmart.adapter.ScannedReviewAdapter adapter) {
        com.google.android.material.bottomsheet.BottomSheetDialog editDialog = new com.google.android.material.bottomsheet.BottomSheetDialog(fragment.requireContext());
        android.view.View sheetView = fragment.getLayoutInflater().inflate(R.layout.bottom_sheet_edit_item, null);
        editDialog.setContentView(sheetView);

        // 1. Bind Views
        android.widget.EditText etName = sheetView.findViewById(R.id.etItemName);
        android.widget.EditText etQty = sheetView.findViewById(R.id.etItemQuantity);
        android.widget.EditText etUnit = sheetView.findViewById(R.id.etItemUnit);
        android.widget.TextView tvExpiryDate = sheetView.findViewById(R.id.tvExpiryDate);
        android.widget.ImageView imgIcon = sheetView.findViewById(R.id.imgSelectedIcon);
        android.widget.FrameLayout btnSelectEmoji = sheetView.findViewById(R.id.btnSelectEmoji);
        android.widget.TextView btnSave = sheetView.findViewById(R.id.btnSaveEdit);
        android.widget.TextView btnZoneMain = sheetView.findViewById(R.id.btnZoneMain);
        android.widget.TextView btnZoneFreezer = sheetView.findViewById(R.id.btnZoneFreezer);

        // 2. Load dữ liệu ban đầu
        etName.setText(item.getName());
        double qty = item.getQuantity();
        etQty.setText(qty == (long) qty ? String.valueOf((long) qty) : String.valueOf(qty));
        etUnit.setText(item.getUnit());
        imgIcon.setImageResource(hcmute.edu.vn.pantrysmart.config.FoodIconConfig.safeIcon(item.getEmoji()));

        // Hạn sử dụng (Date Picker)
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault());
        java.util.Calendar expiryCal = java.util.Calendar.getInstance();
        if (item.getExpiryDate() != null) {
            expiryCal.setTimeInMillis(item.getExpiryDate());
            tvExpiryDate.setText(sdf.format(expiryCal.getTime()));
        } else {
            tvExpiryDate.setText("Chọn ngày");
        }

        tvExpiryDate.setOnClickListener(v -> {
            new android.app.DatePickerDialog(fragment.requireContext(), (view, year, month, dayOfMonth) -> {
                expiryCal.set(year, month, dayOfMonth, 23, 59, 59);
                tvExpiryDate.setText(sdf.format(expiryCal.getTime()));
                item.setExpiryDate(expiryCal.getTimeInMillis());

                // Tính lại expiryDays để hàm lưu chung (saveAIItemsToDatabase) hoạt động đúng
                long diff = expiryCal.getTimeInMillis() - System.currentTimeMillis();
                item.setExpiryDays(Math.max(1, (int) (diff / (1000 * 60 * 60 * 24))));
            }, expiryCal.get(java.util.Calendar.YEAR), expiryCal.get(java.util.Calendar.MONTH), expiryCal.get(java.util.Calendar.DAY_OF_MONTH)).show();
        });

        // Danh mục (Chips)
        android.widget.TextView[] categoryChips = {
                sheetView.findViewById(R.id.chipDairy), sheetView.findViewById(R.id.chipVegetable),
                sheetView.findViewById(R.id.chipFruit), sheetView.findViewById(R.id.chipMeat),
                sheetView.findViewById(R.id.chipSeafood), sheetView.findViewById(R.id.chipDrink),
                sheetView.findViewById(R.id.chipSpice), sheetView.findViewById(R.id.chipOther)
        };
        String[] categoryKeys = { "DAIRY", "VEGETABLE", "FRUIT", "MEAT", "SEAFOOD", "DRINK", "SPICE", "OTHER" };
        final String[] selectedCategory = { item.getCategory() != null ? item.getCategory().toUpperCase() : "OTHER" };
        highlightCategoryChip(categoryChips, categoryKeys, selectedCategory[0]);

        for (int i = 0; i < categoryChips.length; i++) {
            final int index = i;
            categoryChips[i].setOnClickListener(v -> {
                selectedCategory[0] = categoryKeys[index];
                highlightCategoryChip(categoryChips, categoryKeys, selectedCategory[0]);
            });
        }

        // Ngăn chứa (Zone)
        final String[] selectedZone = { item.getStorageZone() != null ? item.getStorageZone() : "MAIN" };
        java.lang.Runnable updateZoneUI = () -> {
            boolean isFreezer = "FREEZER".equals(selectedZone[0]);
            btnZoneFreezer.setBackgroundResource(isFreezer ? R.drawable.bg_edit_zone_active : R.drawable.bg_edit_zone_inactive);
            btnZoneFreezer.setTextColor(isFreezer ? android.graphics.Color.WHITE : android.graphics.Color.parseColor("#4A5565"));
            btnZoneMain.setBackgroundResource(!isFreezer ? R.drawable.bg_edit_zone_active : R.drawable.bg_edit_zone_inactive);
            btnZoneMain.setTextColor(!isFreezer ? android.graphics.Color.WHITE : android.graphics.Color.parseColor("#4A5565"));
        };
        updateZoneUI.run();
        btnZoneMain.setOnClickListener(v -> { selectedZone[0] = "MAIN"; updateZoneUI.run(); });
        btnZoneFreezer.setOnClickListener(v -> { selectedZone[0] = "FREEZER"; updateZoneUI.run(); });

        // Icon (Emoji Picker)
        final String[] selectedEmoji = { item.getEmoji() };
        btnSelectEmoji.setOnClickListener(v -> {
            hcmute.edu.vn.pantrysmart.adapter.EmojiPickerDialog.show(fragment.requireContext(), selectedEmoji[0], emoji -> {
                selectedEmoji[0] = emoji;
                imgIcon.setImageResource(hcmute.edu.vn.pantrysmart.config.FoodIconConfig.safeIcon(emoji));
            });
        });

        // 3. Logic Lưu thay đổi vào Object (Chưa lưu DB)
        btnSave.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            if (name.isEmpty()) { etName.setError("Nhập tên"); return; }

            item.setName(name);
            try {
                item.setQuantity(Double.parseDouble(etQty.getText().toString().trim()));
            } catch (Exception ignored) {}
            item.setUnit(etUnit.getText().toString().trim());
            item.setEmoji(selectedEmoji[0]);
            item.setCategory(selectedCategory[0]);
            item.setStorageZone(selectedZone[0]);

            adapter.notifyItemChanged(position);
            editDialog.dismiss();
        });

        sheetView.findViewById(R.id.btnCloseEditSheet).setOnClickListener(v -> editDialog.dismiss());
        editDialog.show();
    }
}
