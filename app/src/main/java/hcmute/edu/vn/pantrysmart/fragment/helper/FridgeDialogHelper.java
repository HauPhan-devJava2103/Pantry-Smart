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
     * @param fragment      Fragment cha (dùng lấy Context, LayoutInflater, Activity)
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
        if (fragment.getContext() == null) return;

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
            dialog.getWindow().getAttributes().windowAnimations =
                    android.R.style.Animation_InputMethod;
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
        if (fragment.getContext() == null) return;

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
        if (tvSheetTitle != null) tvSheetTitle.setText("Thêm thực phẩm");

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
        String[] categoryKeys = {"DAIRY", "VEGETABLE", "FRUIT", "MEAT", "SEAFOOD", "DRINK", "SPICE", "OTHER"};

        // Ngăn chứa
        TextView btnZoneMain = sheetView.findViewById(R.id.btnZoneMain);
        TextView btnZoneFreezer = sheetView.findViewById(R.id.btnZoneFreezer);

        // 3. Setup UI ban đầu
        imgSelectedIcon.setImageResource(FoodIconConfig.safeIcon(newItem.getEmoji()));
        etItemQuantity.setText("1");
        etItemUnit.setText("kg");

        final String[] selectedCategory = {newItem.getCategory()};
        highlightCategoryChip(categoryChips, categoryKeys, selectedCategory[0]);

        final String[] selectedZone = {newItem.getStorageZone()};
        Runnable updateZoneUI = () -> {
            boolean isFreezer = "FREEZER".equals(selectedZone[0]);
            btnZoneFreezer.setBackgroundResource(isFreezer ? R.drawable.bg_edit_zone_active : R.drawable.bg_edit_zone_inactive);
            btnZoneFreezer.setTextColor(isFreezer ? Color.WHITE : Color.parseColor("#4A5565"));
            btnZoneMain.setBackgroundResource(!isFreezer ? R.drawable.bg_edit_zone_active : R.drawable.bg_edit_zone_inactive);
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

        btnZoneMain.setOnClickListener(v -> { selectedZone[0] = "MAIN"; updateZoneUI.run(); });
        btnZoneFreezer.setOnClickListener(v -> { selectedZone[0] = "FREEZER"; updateZoneUI.run(); });

        Calendar expiryCal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        tvExpiryDate.setOnClickListener(v -> {
            new DatePickerDialog(fragment.requireContext(), (view, year, month, dayOfMonth) -> {
                expiryCal.set(year, month, dayOfMonth, 23, 59, 59);
                tvExpiryDate.setText(sdf.format(expiryCal.getTime()));
            }, expiryCal.get(Calendar.YEAR), expiryCal.get(Calendar.MONTH), expiryCal.get(Calendar.DAY_OF_MONTH)).show();
        });

        final String[] selectedEmoji = {newItem.getEmoji()};
        btnSelectEmoji.setOnClickListener(v -> {
            EmojiPickerDialog.show(fragment.requireContext(), selectedEmoji[0], emoji -> {
                selectedEmoji[0] = emoji;
                imgSelectedIcon.setImageResource(FoodIconConfig.safeIcon(emoji));
            });
        });

        // 5. Logic LƯU (INSERT)
        btnSave.setOnClickListener(v -> {
            String name = etItemName.getText().toString().trim();
            if (name.isEmpty()) { etItemName.setError("Nhập tên thực phẩm"); return; }

            double quantity;
            try {
                quantity = Double.parseDouble(etItemQuantity.getText().toString().trim());
            } catch (Exception e) { etItemQuantity.setError("Số lượng lỗi"); return; }

            String unit = etItemUnit.getText().toString().trim();
            if (unit.isEmpty()) { etItemUnit.setError("Nhập đơn vị"); return; }

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
                if (quantity <= 0) throw new NumberFormatException();
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
}
