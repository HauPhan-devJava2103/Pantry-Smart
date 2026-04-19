package hcmute.edu.vn.pantrysmart.fragment.helper;

import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import hcmute.edu.vn.pantrysmart.R;

/**
 * Helper quản lý FAB menu (Floating Action Button) của FridgeFragment.
 * Bao gồm: setup, toggle, animation mở/đóng.
 */
public class FridgeFabHelper {

    /** Callback khi người dùng nhấn "Quét hóa đơn" */
    public interface OnScanReceiptListener {
        void onScanReceiptClicked();
    }

    /** Callback khi người dùng nhấn "Thêm thủ công" */
    public interface OnManualAddListener {
        void onManualAddClicked();
    }

    private final Fragment fragment;
    private OnScanReceiptListener scanListener;
    private OnManualAddListener manualAddListener;

    private FrameLayout fabMain;
    private TextView fabMainIcon;
    private LinearLayout fabMenuItems;
    private View fabOverlay;
    private LinearLayout fabItemAI, fabItemScan, fabItemManual;

    private boolean fabMenuOpen = false;

    public FridgeFabHelper(Fragment fragment) {
        this.fragment = fragment;
    }

    public void setOnScanReceiptListener(OnScanReceiptListener listener) {
        this.scanListener = listener;
    }

    public void setOnManualAddListener(OnManualAddListener listener) {
        this.manualAddListener = listener;
    }

    /** Bind views và gán click listener cho FAB menu. */
    public void setupFab(View root) {
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
            Toast.makeText(fragment.requireContext(),
                    "Nhận diện AI - Sắp ra mắt", Toast.LENGTH_SHORT).show();
        });

        // Quét hóa đơn → mở bottom sheet thực sự
        fabItemScan.setOnClickListener(v -> {
            toggleFabMenu();
            if (scanListener != null) {
                scanListener.onScanReceiptClicked();
            }
        });

        fabItemManual.setOnClickListener(v -> {
            toggleFabMenu();
            if (manualAddListener != null) {
                manualAddListener.onManualAddClicked();
            } else {
                Toast.makeText(fragment.requireContext(),
                        "Thêm thủ công", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void closeFabMenuIfOpen() {
        if (fabMenuOpen) toggleFabMenu();
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
