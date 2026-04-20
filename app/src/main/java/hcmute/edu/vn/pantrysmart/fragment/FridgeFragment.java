package hcmute.edu.vn.pantrysmart.fragment;


import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import hcmute.edu.vn.pantrysmart.R;
import hcmute.edu.vn.pantrysmart.adapter.PantryItemAdapter;
import hcmute.edu.vn.pantrysmart.config.FoodIconConfig;
import hcmute.edu.vn.pantrysmart.config.GeminiFoodRecognitionService;
import hcmute.edu.vn.pantrysmart.data.local.PantrySmartDatabase;
import hcmute.edu.vn.pantrysmart.data.local.dao.PantryItemDao;
import hcmute.edu.vn.pantrysmart.data.local.entity.PantryItem;
import hcmute.edu.vn.pantrysmart.fragment.helper.FridgeAnimationHelper;
import hcmute.edu.vn.pantrysmart.fragment.helper.FridgeDialogHelper;
import hcmute.edu.vn.pantrysmart.fragment.helper.FridgeFabHelper;
import hcmute.edu.vn.pantrysmart.fragment.helper.ReceiptScanHelper;

/**
 * FridgeFragment — Tab "Tủ lạnh"
 * Fridge Visual 3D (Ngăn đông + Ngăn chính + mở cửa xem kệ thực phẩm)
 * Stats Row (Tổng / Sắp hết hạn / Đã hết hạn)
 *
 * Logic được tách nhỏ vào các helper:
 * - FridgeAnimationHelper: animation cửa tủ
 * - FridgeDialogHelper:    dialog xem / chỉnh sửa thực phẩm
 * - FridgeFabHelper:       FAB menu
 * - ReceiptScanHelper:     quét hóa đơn OCR (ML Kit)
 * - FridgeDialogHelper: dialog xem / chỉnh sửa thực phẩm
 * - FridgeFabHelper: FAB menu
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

    // Cached items for shelf population
    private List<PantryItem> cachedFreezerItems = new ArrayList<>();
    private List<PantryItem> cachedMainItems = new ArrayList<>();

    // Search views
    private EditText etSearch;
    private View layoutSearchResults;
    private RecyclerView rvSearchResults;

    // Helpers
    private FridgeDialogHelper dialogHelper;
    private FridgeFabHelper fabHelper;
    private ReceiptScanHelper receiptScanHelper;

    // Bottom sheet quét hóa đơn
    private BottomSheetDialog scanBottomSheet;

    // Permission launcher — xin quyền Camera runtime
    private final ActivityResultLauncher<String> cameraPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            granted -> {
                if (granted) {
                    openCamera();
                } else {
                    Toast.makeText(requireContext(),
                            "Cần cấp quyền Camera để nhận diện thực phẩm",
                            Toast.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == android.app.Activity.RESULT_OK && result.getData() != null) {
                    // Lấy ảnh từ Camera trả về
                    Bundle extras = result.getData().getExtras();
                    if (extras != null) {
                        android.graphics.Bitmap photo = (android.graphics.Bitmap) extras.get("data");
                        if (photo != null) {
                            String base64Image = encodeImage(photo);

                            // Tạo custom AI scanning dialog với Lottie animation
                            Dialog aiDialog = new Dialog(requireContext());
                            aiDialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
                            aiDialog.setContentView(R.layout.dialog_ai_scanning);
                            aiDialog.setCancelable(false);
                            if (aiDialog.getWindow() != null) {
                                aiDialog.getWindow().setBackgroundDrawable(
                                        new ColorDrawable(android.graphics.Color.TRANSPARENT));
                                aiDialog.getWindow().setLayout(
                                        ViewGroup.LayoutParams.WRAP_CONTENT,
                                        ViewGroup.LayoutParams.WRAP_CONTENT);
                            }
                            aiDialog.show();

                            // 2. Gọi Service AI để nhận diện
                            GeminiFoodRecognitionService.recognizeFood(
                                    base64Image,
                                    new GeminiFoodRecognitionService.RecognitionCallback() {
                                        @Override
                                        public void onSuccess(PantryItem item) {
                                            if (getActivity() == null)
                                                return;
                                            getActivity().runOnUiThread(() -> {
                                                if (aiDialog.isShowing())
                                                    aiDialog.dismiss();
                                                dialogHelper.showAIAddItemBottomSheet(item);
                                            });
                                        }

                                        @Override
                                        public void onError(String errorMessage) {
                                            if (getActivity() == null)
                                                return;
                                            getActivity().runOnUiThread(() -> {
                                                if (aiDialog.isShowing())
                                                    aiDialog.dismiss();
                                                Toast.makeText(requireContext(), errorMessage,
                                                        Toast.LENGTH_SHORT).show();
                                            });
                                        }
                                    });
                        }
                    }
                }
            });

    // Mở Camera sau khi đã có quyền
    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(requireContext().getPackageManager()) != null) {
            cameraLauncher.launch(intent);
        } else {
            Toast.makeText(requireContext(),
                    "Không tìm thấy ứng dụng Camera", Toast.LENGTH_SHORT).show();
        }
    }

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

        // Khởi tạo helpers
        dialogHelper = new FridgeDialogHelper(this, pantryDao, this::loadItems);

        // ReceiptScanHelper phải khởi tạo trước fabHelper (dùng registerForActivityResult)
        receiptScanHelper = new ReceiptScanHelper(this, this::loadItems);

        fabHelper = new FridgeFabHelper(this);
        fabHelper.setOnScanReceiptListener(this::openScanBottomSheet);

        bindViews(view);
        setupListeners();
        fabHelper.setupFab(view, dialogHelper);
        fabHelper.setOnFabActionListener(() -> {
            // Kiểm tra quyền Camera trước khi mở
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    android.Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA);
            }
        });
        loadItems();
        setupSearch(view);
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(),
                new androidx.activity.OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        if (layoutSearchResults.getVisibility() == View.VISIBLE) {
                            etSearch.setText(""); // Đóng tìm kiếm
                        } else {
                            setEnabled(false);
                            requireActivity().onBackPressed();
                        }
                    }
                });
    }

    @Override
    public void onResume() {
        super.onResume();
        loadItems();
    }

    // ========================= SCAN BOTTOM SHEET =========================

    /**
     * Mở bottom sheet "Quét hóa đơn" với 2 nút: Camera & Gallery.
     */
    private void openScanBottomSheet() {
        if (getContext() == null) return;

        View sheetView = LayoutInflater.from(requireContext())
                .inflate(R.layout.bottom_sheet_scan_receipt, null);

        scanBottomSheet = new BottomSheetDialog(requireContext());
        scanBottomSheet.setContentView(sheetView);

        // Đóng bottom sheet
        sheetView.findViewById(R.id.btnCloseScanSheet).setOnClickListener(v ->
                scanBottomSheet.dismiss());

        // Chụp ảnh bằng Camera
        sheetView.findViewById(R.id.btnScanCamera).setOnClickListener(v -> {
            scanBottomSheet.dismiss();
            receiptScanHelper.openCamera();
        });

        // Chọn ảnh từ Gallery
        sheetView.findViewById(R.id.btnScanGallery).setOnClickListener(v -> {
            scanBottomSheet.dismiss();
            receiptScanHelper.openGallery();
        });

        scanBottomSheet.show();
    }

    // ========================= BIND VIEWS =========================

    private void bindViews(View view) {
        tvStatTotal = view.findViewById(R.id.tvStatTotal);
        tvStatExpiring = view.findViewById(R.id.tvStatExpiring);
        tvStatExpired = view.findViewById(R.id.tvStatExpired);

        badgeFreezerCount = view.findViewById(R.id.badgeFreezerCount);
        badgeFreezerWarning = view.findViewById(R.id.badgeFreezerWarning);
        badgeMainCount = view.findViewById(R.id.badgeMainCount);
        badgeMainWarning = view.findViewById(R.id.badgeMainWarning);

        doorFreezer = view.findViewById(R.id.doorFreezer);
        doorMain = view.findViewById(R.id.doorMain);

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
        btnViewAllFreezer = view.findViewById(R.id.btnViewAllFreezer);
        btnViewAllMain = view.findViewById(R.id.btnViewAllMain);
        if (getActivity() != null) {
            etSearch = getActivity().findViewById(R.id.etSearch);
        }
        layoutSearchResults = view.findViewById(R.id.layoutSearchResults);
        rvSearchResults = view.findViewById(R.id.rvSearchResults);
        View btnClearSearch = getActivity() != null ? getActivity().findViewById(R.id.btnClearSearch) : null;
        if (btnClearSearch != null) {
            btnClearSearch.setOnClickListener(v -> {
                if (etSearch != null) {
                    etSearch.setText(""); // Xóa chữ
                    etSearch.clearFocus();
                    // Ẩn bàn phím
                    android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) requireContext()
                            .getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(etSearch.getWindowToken(), 0);
                }
            });
        }
    }

    private void setupSearch(View view) {
        if (etSearch == null)
            return;

        etSearch.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim().toLowerCase();

                // Tìm nút xóa để ẩn/hiện
                View btnClear = getActivity() != null ? getActivity().findViewById(R.id.btnClearSearch) : null;
                if (btnClear != null) {
                    btnClear.setVisibility(query.isEmpty() ? View.GONE : View.VISIBLE);
                }

                if (query.isEmpty()) {
                    layoutSearchResults.setVisibility(View.GONE);
                } else {
                    layoutSearchResults.setVisibility(View.VISIBLE);
                    performSearch(query);
                }
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
            }
        });

        // 1. Khi nhấn vào vùng chứa (searchBar), tập trung vào ô nhập liệu
        View searchBar = null;
        if (getActivity() != null) {
            searchBar = getActivity().findViewById(R.id.searchBar);
        }

        if (searchBar != null) {
            searchBar.setOnClickListener(v -> {
                etSearch.requestFocus();
                android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) requireContext()
                        .getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(etSearch, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
            });
        }

        // 2. Xử lý nút "Tìm" trên bàn phím
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                // Thu nhỏ bàn phím khi nhấn nút Tìm
                android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) requireContext()
                        .getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(etSearch.getWindowToken(), 0);
                return true;
            }
            return false;
        });
    }

    private void performSearch(String query) {
        List<PantryItem> searchResults = new ArrayList<>();

        // 1. Dò trong Ngăn Đông
        for (PantryItem item : cachedFreezerItems) {
            if (item.getName().toLowerCase().contains(query)) {
                searchResults.add(item);
            }
        }

        // 2. Dò trong Ngăn Chính
        for (PantryItem item : cachedMainItems) {
            if (item.getName().toLowerCase().contains(query)) {
                searchResults.add(item);
            }
        }

        // 3. Cập nhật số lượng kết quả lên giao diện
        TextView tvSearchCount = layoutSearchResults.findViewById(R.id.tvSearchCount);
        if (tvSearchCount != null) {
            tvSearchCount.setText("Tìm thấy " + searchResults.size() + " thực phẩm");
        }

        // 4. Hiển thị danh sách (Chúng ta sẽ thiết lập Adapter ở bước sau)
        displaySearchResults(searchResults);
    }

    private void displaySearchResults(List<PantryItem> results) {
        // 1. Thiết lập LayoutManager nếu chưa có
        if (rvSearchResults.getLayoutManager() == null) {
            rvSearchResults.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(requireContext()));
        }

        // 2. Khởi tạo hoặc cập nhật Adapter
        PantryItemAdapter adapter = new PantryItemAdapter(requireContext(), results);
        rvSearchResults.setAdapter(adapter);

        // 3. Xử lý khi nhấn vào một món đồ trong kết quả tìm kiếm
        adapter.setOnItemActionListener(new PantryItemAdapter.OnItemActionListener() {
            @Override
            public void onEdit(PantryItem item, int position) {
                // Tái sử dụng Dialog chỉnh sửa mà bạn đã làm ở nhánh trước
                dialogHelper.showEditItemBottomSheet(item, position, adapter, null);
            }

            @Override
            public void onDelete(PantryItem item, int position) {
                // Hiện thông báo xác nhận trước khi xóa
                new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Xác nhận xóa")
                        .setMessage("Bạn có chắc muốn xóa '" + item.getName() + "' khỏi tủ lạnh?")
                        .setPositiveButton("Xóa", (dialog, which) -> {
                            // Thực hiện xóa trong Database ở luồng phụ
                            PantrySmartDatabase.databaseWriteExecutor.execute(() -> {
                                pantryDao.delete(item);

                                if (getActivity() != null) {
                                    getActivity().runOnUiThread(() -> {
                                        // 1. Xóa món đồ khỏi danh sách tìm kiếm hiện tại
                                        adapter.removeItem(position);

                                        // 2. Cập nhật lại số lượng kết quả trên header
                                        TextView tvCount = layoutSearchResults.findViewById(R.id.tvSearchCount);
                                        if (tvCount != null) {
                                            tvCount.setText("Tìm thấy " + adapter.getItemCount() + " thực phẩm");
                                        }

                                        // 3. Quan trọng: Load lại dữ liệu tủ lạnh (Stats + Shelves) ở phía dưới
                                        loadItems();

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
    }

    private void setupListeners() {
        doorFreezer.setOnClickListener(v -> toggleFreezerDoor());
        doorMain.setOnClickListener(v -> toggleMainDoor());
        btnCloseFreezer.setOnClickListener(v -> toggleFreezerDoor());
        btnCloseMain.setOnClickListener(v -> toggleMainDoor());
        btnViewAllFreezer
                .setOnClickListener(v -> dialogHelper.showAllItemsDialog(cachedFreezerItems, "Ngăn Đông", "#3A7AB5"));
        btnViewAllMain
                .setOnClickListener(v -> dialogHelper.showAllItemsDialog(cachedMainItems, "Ngăn Chính", "#A06828"));
    }

    // ========================= DOOR TOGGLE =========================

    // Hàm xử lý khi nhấn vào cửa Ngăn Đông
    private void toggleFreezerDoor() {
        freezerOpen = !freezerOpen;
        if (freezerOpen) {
            FridgeAnimationHelper.animateViewOut(doorFreezer, () -> {
                doorFreezer.setVisibility(View.GONE);
                populateShelves(cachedFreezerItems, freezerShelf1, freezerShelf2,
                        null, tvFreezerEmpty);
                btnViewAllFreezer.setVisibility(
                        !cachedFreezerItems.isEmpty() ? View.VISIBLE : View.GONE);
                interiorFreezer.setAlpha(0f);
                interiorFreezer.setVisibility(View.VISIBLE);
                FridgeAnimationHelper.animateViewIn(interiorFreezer);
            });
        } else {
            FridgeAnimationHelper.animateViewOut(interiorFreezer, () -> {
                interiorFreezer.setVisibility(View.GONE);
                btnViewAllFreezer.setVisibility(View.GONE);
                doorFreezer.setAlpha(0f);
                doorFreezer.setVisibility(View.VISIBLE);
                FridgeAnimationHelper.animateViewIn(doorFreezer);
            });
        }
    }

    // Hàm xử lý khi nhấn vào cửa Ngăn Chính
    private void toggleMainDoor() {
        mainOpen = !mainOpen;
        if (mainOpen) {
            FridgeAnimationHelper.animateViewOut(doorMain, () -> {
                doorMain.setVisibility(View.GONE);
                populateShelves(cachedMainItems, mainShelf1, mainShelf2,
                        mainShelf3, tvMainEmpty);
                btnViewAllMain.setVisibility(
                        !cachedMainItems.isEmpty() ? View.VISIBLE : View.GONE);
                interiorMain.setAlpha(0f);
                interiorMain.setVisibility(View.VISIBLE);
                FridgeAnimationHelper.animateViewIn(interiorMain);
            });
        } else {
            FridgeAnimationHelper.animateViewOut(interiorMain, () -> {
                interiorMain.setVisibility(View.GONE);
                btnViewAllMain.setVisibility(View.GONE);
                doorMain.setAlpha(0f);
                doorMain.setVisibility(View.VISIBLE);
                FridgeAnimationHelper.animateViewIn(doorMain);
            });
        }
    }

    // ========================= SHELF =========================

    // Hàm phân bổ danh sách thực phẩm vào các kệ của tủ lạnh
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
                targetShelf = (i < maxPerShelf) ? shelf1 : shelf2;
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

    // Hàm tạo view hiển thị thực phẩm (vòng tròn chứa icon)
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

        // Icon circle
        FrameLayout iconFrame = new FrameLayout(requireContext());
        iconFrame.setLayoutParams(new LinearLayout.LayoutParams(dp(42), dp(42)));
        iconFrame.setElevation(dp(2));
        iconFrame.setBackgroundResource(
                isExpiring ? R.drawable.bg_food_on_shelf_warning : R.drawable.bg_food_on_shelf);

        ImageView iconView = new ImageView(requireContext());
        FrameLayout.LayoutParams iconParams = new FrameLayout.LayoutParams(dp(24), dp(24));
        iconParams.gravity = Gravity.CENTER;
        iconView.setLayoutParams(iconParams);
        iconView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        iconView.setImageResource(FoodIconConfig.safeIcon(item.getEmoji()));
        iconFrame.addView(iconView);

        iconFrame.setOnClickListener(v -> Toast.makeText(requireContext(),
                item.getName() + " — " + formatQuantity(item.getQuantity())
                        + " " + item.getUnit(),
                Toast.LENGTH_SHORT).show());
        container.addView(iconFrame);

        // Name label
        TextView label = new TextView(requireContext());
        label.setText(truncate(item.getName(), 6));
        label.setTextSize(8);
        label.setTextColor(isExpiring
                ? Color.parseColor("#EA580C")
                : Color.parseColor("#6B7280"));
        label.setGravity(Gravity.CENTER);
        label.setPadding(0, dp(2), 0, 0);
        container.addView(label);

        return container;
    }

    // Hàm thêm các empty slots vào kệ
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

    // Hàm lấp đầy chỗ trống bằng empty slots
    private void fillEmptySlots(LinearLayout shelf, int maxPerShelf) {
        int remaining = maxPerShelf - shelf.getChildCount();
        if (remaining > 0)
            addEmptySlots(shelf, remaining);
    }

    // ========================= DATA =========================

    // Hàm tải dữ liệu tổng hợp: đếm số món, sắp hết hạn, đã hết hạn
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

                cachedFreezerItems = freezerItems;
                cachedMainItems = mainItems;

                // Fridge Visual badges
                badgeFreezerCount.setText(totalFreezer + " món");
                badgeMainCount.setText(totalMain + " món");
                if (finalExpiringFreezer > 0) {
                    badgeFreezerWarning.setText(String.valueOf(finalExpiringFreezer));
                    badgeFreezerWarning.setCompoundDrawablesWithIntrinsicBounds(
                            R.drawable.ic_warning, 0, 0, 0);
                    badgeFreezerWarning.setCompoundDrawablePadding(dp(3));
                    badgeFreezerWarning.setVisibility(View.VISIBLE);
                } else {
                    badgeFreezerWarning.setVisibility(View.GONE);
                }
                if (finalExpiringMain > 0) {
                    badgeMainWarning.setText(String.valueOf(finalExpiringMain));
                    badgeMainWarning.setCompoundDrawablesWithIntrinsicBounds(
                            R.drawable.ic_warning, 0, 0, 0);
                    badgeMainWarning.setCompoundDrawablePadding(dp(3));
                    badgeMainWarning.setVisibility(View.VISIBLE);
                } else {
                    badgeMainWarning.setVisibility(View.GONE);
                }

                // Refresh shelves if open
                if (freezerOpen) {
                    populateShelves(cachedFreezerItems,
                            freezerShelf1, freezerShelf2, null, tvFreezerEmpty);
                    btnViewAllFreezer.setVisibility(
                            !cachedFreezerItems.isEmpty() ? View.VISIBLE : View.GONE);
                }
                if (mainOpen) {
                    populateShelves(cachedMainItems,
                            mainShelf1, mainShelf2, mainShelf3, tvMainEmpty);
                    btnViewAllMain.setVisibility(
                            !cachedMainItems.isEmpty() ? View.VISIBLE : View.GONE);
                }

                // Stats Row
                tvStatTotal.setText(String.valueOf(totalItems));
                tvStatExpiring.setText(String.valueOf(expiring.size()));
                tvStatExpired.setText(String.valueOf(finalExpiredCount));
            });
        });
    }

    // ========================= UTILS =========================

    private int dp(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density + 0.5f);
    }

    private String formatQuantity(double qty) {
        if (qty == (long) qty)
            return String.valueOf((long) qty);
        return String.valueOf(qty);
    }

    private String truncate(String text, int maxLen) {
        if (text == null)
            return "";
        return text.length() > maxLen ? text.substring(0, maxLen) + "…" : text;
    }

    private String encodeImage(Bitmap bm) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.JPEG, 60, baos);
        byte[] b = baos.toByteArray();
        return Base64.encodeToString(b, Base64.NO_WRAP);
    }
}