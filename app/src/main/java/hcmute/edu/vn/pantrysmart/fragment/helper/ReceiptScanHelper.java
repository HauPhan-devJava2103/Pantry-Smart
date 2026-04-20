package hcmute.edu.vn.pantrysmart.fragment.helper;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.NumberFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import hcmute.edu.vn.pantrysmart.R;
import hcmute.edu.vn.pantrysmart.adapter.EmojiPickerDialog;
import hcmute.edu.vn.pantrysmart.adapter.ScannedReviewAdapter;
import hcmute.edu.vn.pantrysmart.config.FoodIconConfig;
import hcmute.edu.vn.pantrysmart.config.GeminiReceiptParser;
import hcmute.edu.vn.pantrysmart.config.ReceiptOcrProcessor;
import hcmute.edu.vn.pantrysmart.data.local.PantrySmartDatabase;
import hcmute.edu.vn.pantrysmart.data.local.dao.ExpenseDao;
import hcmute.edu.vn.pantrysmart.data.local.dao.PantryItemDao;
import hcmute.edu.vn.pantrysmart.data.local.entity.Expense;
import hcmute.edu.vn.pantrysmart.data.local.entity.PantryItem;
import hcmute.edu.vn.pantrysmart.model.ScannedItem;

import android.app.DatePickerDialog;
import android.graphics.Color;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import java.text.SimpleDateFormat;

/**
 * Helper quản lý toàn bộ luồng quét hóa đơn:
 * 1. Mở Camera hoặc Gallery
 * 2. Nhận ảnh → hiển thị loading dialog
 * 3. Gọi ML Kit OCR → parse mặt hàng
 * 4. Hiển thị dialog review để người dùng xem xét / xóa item
 * 5. Lưu tất cả vào PantryItem + Expense khi xác nhận
 */
public class ReceiptScanHelper {

    // Nâng lên 3000px để giữ lại độ nét tối đa (Full HD) cho ML Kit đọc chính xác
    // dấu Tiếng Việt
    private static final int MAX_IMAGE_SIZE = 3000; // px
    private static final String AUTHORITY = "hcmute.edu.vn.pantrysmart.fileprovider";

    private final Fragment fragment;
    private final Runnable onSavedCallback;

    private Uri cameraImageUri;
    private AlertDialog loadingDialog;

    // ActivityResult launchers — đăng ký trong fragment
    private final ActivityResultLauncher<Uri> cameraLauncher;
    private final ActivityResultLauncher<String> galleryLauncher;
    private final ActivityResultLauncher<String> cameraPermissionLauncher;

    public ReceiptScanHelper(Fragment fragment, Runnable onSavedCallback) {
        this.fragment = fragment;
        this.onSavedCallback = onSavedCallback;

        // 1. Camera launcher
        cameraLauncher = fragment.registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                success -> {
                    if (Boolean.TRUE.equals(success) && cameraImageUri != null) {
                        processUri(cameraImageUri);
                    }
                });

        // 2. Gallery launcher
        galleryLauncher = fragment.registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        processUri(uri);
                    }
                });

        // 3. Camera permission launcher
        cameraPermissionLauncher = fragment.registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (granted) {
                        launchCamera();
                    } else {
                        Toast.makeText(fragment.requireContext(),
                                "Cần cấp quyền Camera để chụp hóa đơn", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // ========================= PUBLIC API =========================

    /** Mở Camera để chụp ảnh hóa đơn */
    public void openCamera() {
        if (ContextCompat.checkSelfPermission(fragment.requireContext(),
                Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            launchCamera();
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    /** Mở Gallery để chọn ảnh hóa đơn */
    public void openGallery() {
        galleryLauncher.launch("image/*");
    }

    // ========================= CAMERA =========================

    private void launchCamera() {
        try {
            File imageFile = createTempImageFile();
            cameraImageUri = FileProvider.getUriForFile(
                    fragment.requireContext(), AUTHORITY, imageFile);
            cameraLauncher.launch(cameraImageUri);
        } catch (IOException e) {
            Toast.makeText(fragment.requireContext(),
                    "Không thể tạo file ảnh tạm", Toast.LENGTH_SHORT).show();
        }
    }

    private File createTempImageFile() throws IOException {
        File cacheDir = new File(fragment.requireContext().getCacheDir(), "receipt_images");
        if (!cacheDir.exists())
            cacheDir.mkdirs();
        // Xóa ảnh cũ để tiết kiệm bộ nhớ
        File[] oldFiles = cacheDir.listFiles();
        if (oldFiles != null) {
            for (File f : oldFiles)
                f.delete();
        }
        return new File(cacheDir, "receipt_" + System.currentTimeMillis() + ".jpg");
    }

    // ========================= PROCESS IMAGE =========================

    private void processUri(Uri uri) {
        showLoadingDialog();
        PantrySmartDatabase.databaseWriteExecutor.execute(() -> {
            try {
                Bitmap bitmap = loadAndCompressBitmap(uri);
                if (bitmap == null) {
                    dismissLoadingDialog();
                    showToast("Không thể đọc ảnh. Hãy thử lại.");
                    return;
                }
                // BƯỚC 1: Đọc ảnh chưa nén bằng ML Kit (Tốc độ vài mini giây, không tốn mạng)
                fragment.requireActivity().runOnUiThread(
                        () -> ReceiptOcrProcessor.extractRawText(bitmap, new ReceiptOcrProcessor.OcrRawCallback() {
                            @Override
                            public void onSuccess(String rawText) {
                                // BƯỚC 2: Chặn 429 bằng cách chỉ gửi Text thô lên Gemini
                                GeminiReceiptParser.parseReceiptText(rawText, new GeminiReceiptParser.ParseCallback() {
                                    @Override
                                    public void onSuccess(List<ScannedItem> items) {
                                        if (fragment.getActivity() != null) {
                                            fragment.requireActivity().runOnUiThread(() -> {
                                                dismissLoadingDialog();
                                                showReviewDialog(items);
                                            });
                                        }
                                    }

                                    @Override
                                    public void onError(String message) {
                                        if (fragment.getActivity() != null) {
                                            fragment.requireActivity().runOnUiThread(() -> {
                                                dismissLoadingDialog();
                                                showToastOnMain(message);
                                            });
                                        }
                                    }
                                });
                            }

                            @Override
                            public void onError(String message) {
                                dismissLoadingDialog();
                                showToastOnMain(message);
                            }
                        }));
            } catch (Exception e) {
                dismissLoadingDialog();
                showToast("Lỗi xử lý HD an toàn: " + e.getMessage());
            }
        });
    }

    /**
     * Tải và compress ảnh xuống MAX_IMAGE_SIZE × MAX_IMAGE_SIZE để tối ưu tốc độ
     * OCR
     */
    private Bitmap loadAndCompressBitmap(Uri uri) {
        try {
            // Lấy kích thước gốc
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            InputStream stream = fragment.requireContext()
                    .getContentResolver().openInputStream(uri);
            if (stream == null)
                return null;
            BitmapFactory.decodeStream(stream, null, opts);
            stream.close();

            // Tính inSampleSize
            int sampleSize = 1;
            int w = opts.outWidth, h = opts.outHeight;
            while (w / sampleSize > MAX_IMAGE_SIZE || h / sampleSize > MAX_IMAGE_SIZE) {
                sampleSize *= 2;
            }

            // Decode với sample size
            opts.inJustDecodeBounds = false;
            opts.inSampleSize = sampleSize;
            InputStream stream2 = fragment.requireContext()
                    .getContentResolver().openInputStream(uri);
            if (stream2 == null)
                return null;
            Bitmap bitmap = BitmapFactory.decodeStream(stream2, null, opts);
            stream2.close();
            return bitmap;
        } catch (Exception e) {
            return null;
        }
    }

    // ========================= LOADING DIALOG =========================

    private void showLoadingDialog() {
        if (fragment.getActivity() == null)
            return;
        fragment.requireActivity().runOnUiThread(() -> {
            View loadingView = LayoutInflater.from(fragment.requireContext())
                    .inflate(R.layout.dialog_loading_scan, null);
            loadingDialog = new AlertDialog.Builder(fragment.requireContext())
                    .setView(loadingView)
                    .setCancelable(false)
                    .create();
            if (loadingDialog.getWindow() != null) {
                loadingDialog.getWindow().setBackgroundDrawableResource(
                        R.drawable.bg_dialog_main);
            }
            loadingDialog.show();
        });
    }

    private void dismissLoadingDialog() {
        if (fragment.getActivity() == null)
            return;
        fragment.requireActivity().runOnUiThread(() -> {
            if (loadingDialog != null && loadingDialog.isShowing()) {
                loadingDialog.dismiss();
            }
        });
    }

    // ========================= REVIEW DIALOG =========================

    private void showReviewDialog(List<ScannedItem> items) {
        if (fragment.getActivity() == null)
            return;

        View dialogView = LayoutInflater.from(fragment.requireContext())
                .inflate(R.layout.dialog_review_scanned_items, null);

        // Subtitle
        TextView tvSubtitle = dialogView.findViewById(R.id.tvReviewSubtitle);
        tvSubtitle.setText("Tìm thấy " + items.size() + " mặt hàng. Kiểm tra trước khi lưu.");

        // RecyclerView
        RecyclerView rv = dialogView.findViewById(R.id.rvReviewScannedItems);
        ScannedReviewAdapter adapter = new ScannedReviewAdapter(items, (item, position) -> {
            showEditItemBottomSheet(item, position, (ScannedReviewAdapter) rv.getAdapter());
        });
        rv.setLayoutManager(new LinearLayoutManager(fragment.requireContext()));
        rv.setAdapter(adapter);

        // Nút lưu tất cả
        TextView btnSave = dialogView.findViewById(R.id.btnSaveAllScanned);
        updateSaveButtonText(btnSave, items.size());

        // Cập nhật số lượng khi adapter thay đổi
        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeRemoved(int positionStart, int itemCount) {
                updateSaveButtonText(btnSave, adapter.getItemCount());
            }
        });

        AlertDialog dialog = new AlertDialog.Builder(fragment.requireContext())
                .setView(dialogView)
                .setCancelable(true)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        // Nút Hủy
        TextView btnCancel = dialogView.findViewById(R.id.btnCancelReview);
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        // Nút Lưu
        btnSave.setOnClickListener(v -> {
            List<ScannedItem> finalItems = adapter.getItems();
            if (finalItems.isEmpty()) {
                Toast.makeText(fragment.requireContext(),
                        "Danh sách trống, không có gì để lưu.", Toast.LENGTH_SHORT).show();
                return;
            }
            dialog.dismiss();
            saveItemsToDatabase(finalItems);
        });

        dialog.show();

        // Giới hạn chiều cao dialog tối đa 80% màn hình
        if (dialog.getWindow() != null) {
            DisplayMetrics dm = fragment.getResources().getDisplayMetrics();
            int maxHeight = (int) (dm.heightPixels * 0.80);
            dialog.getWindow().setLayout(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    maxHeight);
        }
    }

    private void showEditItemBottomSheet(ScannedItem item, int position, ScannedReviewAdapter adapter) {
        BottomSheetDialog editDialog = new BottomSheetDialog(fragment.requireContext());
        View sheetView = LayoutInflater.from(fragment.requireContext())
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
        TextView tvExpiryDateView = sheetView.findViewById(R.id.tvExpiryDate);
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

        double qtyValue = item.getQuantity();
        etItemQuantity.setText(qtyValue == (long) qtyValue ? String.valueOf((long) qtyValue) : String.valueOf(qtyValue));
        etItemUnit.setText(item.getUnit());

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        Calendar expiryCal = Calendar.getInstance();
        
        // Nếu đã có ngày tuyệt đối thì dùng, không thì tính từ expiryDays
        if (item.getExpiryDate() != null) {
            expiryCal.setTimeInMillis(item.getExpiryDate());
        } else {
            expiryCal.add(Calendar.DAY_OF_YEAR, item.getExpiryDays());
            item.setExpiryDate(expiryCal.getTimeInMillis());
        }
        tvExpiryDateView.setText(sdf.format(expiryCal.getTime()));

        // ---- Logic chọn danh mục ----
        TextView[] categoryChips = {
                chipDairy, chipVegetable, chipFruit, chipMeat,
                chipSeafood, chipDrink, chipSpice, chipOther
        };
        String[] categoryKeys = {
                "sữa", "rau", "trái cây", "thịt",
                "hải sản", "đồ uống", "gia vị", "khác"
        };
        final String[] selectedCategory = { item.getCategory() };
        highlightCategoryChip(categoryChips, categoryKeys, selectedCategory[0]);

        for (int i = 0; i < categoryChips.length; i++) {
            final int index = i;
            categoryChips[i].setOnClickListener(v -> {
                selectedCategory[0] = categoryKeys[index];
                highlightCategoryChip(categoryChips, categoryKeys, categoryKeys[index]);
            });
        }

        // ---- Logic chọn ngăn chứa ----
        final String[] selectedZone = { item.getStorageZone() };
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

        btnZoneMain.setOnClickListener(v -> { selectedZone[0] = "MAIN"; updateZoneUI.run(); });
        btnZoneFreezer.setOnClickListener(v -> { selectedZone[0] = "FREEZER"; updateZoneUI.run(); });

        // ---- Date Picker ----
        tvExpiryDateView.setOnClickListener(v -> {
            new DatePickerDialog(fragment.requireContext(), (view, year, month, dayOfMonth) -> {
                expiryCal.set(year, month, dayOfMonth, 23, 59, 59);
                tvExpiryDateView.setText(sdf.format(expiryCal.getTime()));
                item.setExpiryDate(expiryCal.getTimeInMillis());
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
            item.setName(etItemName.getText().toString());
            item.setEmoji(selectedEmoji[0]);
            try {
                item.setQuantity(Double.parseDouble(etItemQuantity.getText().toString()));
            } catch (Exception ignored) {}
            item.setUnit(etItemUnit.getText().toString());
            item.setCategory(selectedCategory[0]);
            item.setStorageZone(selectedZone[0]);

            adapter.notifyItemChanged(position);
            editDialog.dismiss();
        });

        btnClose.setOnClickListener(v -> editDialog.dismiss());
        editDialog.show();
    }

    private void highlightCategoryChip(TextView[] chips, String[] keys, String activeKey) {
        for (int i = 0; i < chips.length; i++) {
            if (keys[i].equalsIgnoreCase(activeKey)) {
                chips[i].setBackgroundResource(R.drawable.bg_edit_category_chip_active);
                chips[i].setTextColor(Color.WHITE);
            } else {
                chips[i].setBackgroundResource(R.drawable.bg_edit_category_chip);
                chips[i].setTextColor(Color.parseColor("#4A5565"));
            }
        }
    }

    private void updateSaveButtonText(TextView btn, int count) {
        btn.setText("Thêm tất cả (" + count + ")");
    }

    // ========================= SAVE TO DATABASE =========================

    /**
     * Lưu toàn bộ danh sách vào:
     * - PantryItem (mỗi mặt hàng → 1 row trong tủ lạnh)
     * - Expense (tổng hóa đơn → 1 giao dịch chi tiêu nguồn SCAN)
     */
    private void saveItemsToDatabase(List<ScannedItem> items) {
        PantrySmartDatabase.databaseWriteExecutor.execute(() -> {
            PantrySmartDatabase db = PantrySmartDatabase.getInstance(
                    fragment.requireContext());
            PantryItemDao pantryDao = db.pantryItemDao();
            ExpenseDao expenseDao = db.expenseDao();

            long now = System.currentTimeMillis();
            double totalPrice = 0;
            int savedCount = 0;

            for (ScannedItem scanned : items) {
                // Tạo PantryItem
                PantryItem pantryItem = new PantryItem();
                pantryItem.setName(scanned.getName());
                pantryItem.setEmoji(scanned.getEmoji());
                pantryItem.setQuantity(scanned.getQuantity());
                pantryItem.setUnit(scanned.getUnit());
                pantryItem.setStorageZone(scanned.getStorageZone());
                pantryItem.setCategory(scanned.getCategory());
                pantryItem.setAddedDate(now);

                // Tính toán ngày hết hạn
                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.DAY_OF_YEAR, scanned.getExpiryDays());
                pantryItem.setExpiryDate(cal.getTimeInMillis());

                pantryItem.setActive(true);
                pantryDao.insert(pantryItem);

                totalPrice += scanned.getPrice();
                savedCount++;
            }

            // Ghi một khoản chi tiêu tổng hợp (nếu có giá)
            if (totalPrice > 0) {
                Expense expense = new Expense();
                expense.setName("Mua sắm (quét hóa đơn) — " + savedCount + " mặt hàng");
                expense.setAmount(totalPrice);
                expense.setCategoryKey("SHOPPING");
                expense.setSource("SCAN");
                expense.setExpenseDate(now);
                expenseDao.insertExpense(expense);
            }

            // Callback trên UI thread
            final int finalCount = savedCount;
            final double finalTotal = totalPrice;
            if (fragment.getActivity() != null) {
                fragment.requireActivity().runOnUiThread(() -> {
                    String msg = "✅ Đã lưu " + finalCount + " mặt hàng vào tủ lạnh";
                    if (finalTotal > 0) {
                        NumberFormat fmt = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
                        msg += "\n💰 Chi tiêu: " + fmt.format((long) finalTotal) + "đ";
                    }
                    Toast.makeText(fragment.requireContext(), msg, Toast.LENGTH_LONG).show();
                    if (onSavedCallback != null)
                        onSavedCallback.run();
                });
            }
        });
    }

    // ========================= UTILS =========================

    private void showToast(String message) {
        if (fragment.getActivity() != null) {
            fragment.requireActivity()
                    .runOnUiThread(() -> Toast.makeText(fragment.requireContext(), message, Toast.LENGTH_LONG).show());
        }
    }

    private void showToastOnMain(String message) {
        Toast.makeText(fragment.requireContext(), message, Toast.LENGTH_LONG).show();
    }
}
