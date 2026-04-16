package hcmute.edu.vn.pantrysmart.adapter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import hcmute.edu.vn.pantrysmart.R;
import hcmute.edu.vn.pantrysmart.config.FoodIconConfig;

/**
 * BottomSheet dialog cho phep chon icon thuc pham theo nhom.
 * Tra ve drawable name (VD: "ic_food_steak") de luu vao DB.
 */
public class EmojiPickerDialog {

    public interface OnIconPickedListener {
        void onPicked(String iconName);
    }

    public static void show(Context context, String currentIconName, OnIconPickedListener listener) {
        BottomSheetDialog dialog = new BottomSheetDialog(context);
        View content = View.inflate(context, R.layout.dialog_emoji_picker, null);
        dialog.setContentView(content);

        Map<String, int[]> groupedIcons = FoodIconConfig.getGroupedIcons();
        List<String> groupNames = new ArrayList<>(groupedIcons.keySet());

        RecyclerView rvGrid = content.findViewById(R.id.rvEmojiGrid);
        rvGrid.setLayoutManager(new GridLayoutManager(context, 5));

        int[] firstGroupIcons = groupedIcons.get(groupNames.get(0));
        int currentIconRes = FoodIconConfig.safeIcon(currentIconName);
        IconGridAdapter adapter = new IconGridAdapter(context, firstGroupIcons, currentIconRes);
        rvGrid.setAdapter(adapter);

        adapter.setOnIconSelectedListener(iconRes -> {
            rvGrid.postDelayed(() -> {
                if (listener != null) {
                    // Reverse lookup: drawable res -> drawable name (de luu DB)
                    String iconName = FoodIconConfig.getIconName(iconRes);
                    listener.onPicked(iconName);
                }
                dialog.dismiss();
            }, 300);
        });

        // Tab nhom
        LinearLayout layoutTabs = content.findViewById(R.id.layoutEmojiGroupTabs);
        List<TextView> tabViews = new ArrayList<>();

        for (int i = 0; i < groupNames.size(); i++) {
            String groupName = groupNames.get(i);
            TextView tab = createTabView(context, groupName, i == 0);
            tabViews.add(tab);

            final int index = i;
            tab.setOnClickListener(v -> {
                for (int j = 0; j < tabViews.size(); j++) {
                    setTabActive(tabViews.get(j), j == index);
                }
                int[] icons = groupedIcons.get(groupNames.get(index));
                adapter.updateIcons(icons);
                rvGrid.scrollToPosition(0);
            });

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(dp(context, 4), 0, dp(context, 4), 0);
            layoutTabs.addView(tab, params);
        }

        FrameLayout btnClose = content.findViewById(R.id.btnCloseEmojiPicker);
        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    // === Grid Adapter ===

    private static class IconGridAdapter extends RecyclerView.Adapter<IconGridAdapter.VH> {

        interface OnIconSelectedListener {
            void onIconSelected(int iconRes);
        }

        private final Context context;
        private int[] icons;
        private int selectedIconRes;
        private OnIconSelectedListener listener;

        IconGridAdapter(Context context, int[] icons, int selectedIconRes) {
            this.context = context;
            this.icons = icons;
            this.selectedIconRes = selectedIconRes;
        }

        void setOnIconSelectedListener(OnIconSelectedListener listener) {
            this.listener = listener;
        }

        void updateIcons(int[] newIcons) {
            this.icons = newIcons;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            FrameLayout frame = new FrameLayout(context);
            int size = dp(context, 52);
            RecyclerView.LayoutParams params = new RecyclerView.LayoutParams(size, size);
            int margin = dp(context, 6);
            params.setMargins(margin, margin, margin, margin);
            frame.setLayoutParams(params);

            ImageView iv = new ImageView(context);
            int iconSize = dp(context, 28);
            FrameLayout.LayoutParams ivParams = new FrameLayout.LayoutParams(iconSize, iconSize);
            ivParams.gravity = Gravity.CENTER;
            iv.setLayoutParams(ivParams);
            iv.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            iv.setTag("icon");
            frame.addView(iv);

            return new VH(frame);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            int iconRes = icons[position];
            holder.iv.setImageResource(iconRes);

            boolean isSelected = iconRes == selectedIconRes;
            holder.frame.setBackgroundResource(
                    isSelected ? R.drawable.bg_emoji_cell_selected : R.drawable.bg_emoji_cell);
            holder.frame.setScaleX(isSelected ? 1.12f : 1.0f);
            holder.frame.setScaleY(isSelected ? 1.12f : 1.0f);

            holder.frame.setOnClickListener(v -> {
                selectedIconRes = iconRes;
                notifyDataSetChanged();
                v.animate().scaleX(1.3f).scaleY(1.3f).setDuration(100)
                        .withEndAction(() -> v.animate()
                                .scaleX(1.12f).scaleY(1.12f)
                                .setDuration(100).start())
                        .start();
                if (listener != null)
                    listener.onIconSelected(iconRes);
            });
        }

        @Override
        public int getItemCount() {
            return icons != null ? icons.length : 0;
        }

        static class VH extends RecyclerView.ViewHolder {
            final FrameLayout frame;
            final ImageView iv;

            VH(@NonNull View itemView) {
                super(itemView);
                frame = (FrameLayout) itemView;
                iv = itemView.findViewWithTag("icon");
            }
        }
    }

    // === Helpers ===

    private static TextView createTabView(Context context, String text, boolean active) {
        TextView tv = new TextView(context);
        tv.setText(text);
        tv.setTextSize(13);
        tv.setTypeface(null, Typeface.BOLD);
        tv.setPadding(dp(context, 14), dp(context, 8), dp(context, 14), dp(context, 8));
        setTabActive(tv, active);
        return tv;
    }

    private static void setTabActive(TextView tv, boolean active) {
        if (active) {
            tv.setBackgroundResource(R.drawable.bg_emoji_tab_active);
            tv.setTextColor(Color.parseColor("#059669"));
        } else {
            tv.setBackgroundResource(R.drawable.bg_emoji_tab_inactive);
            tv.setTextColor(Color.parseColor("#6B7280"));
        }
    }

    private static int dp(Context context, int dp) {
        return (int) (dp * context.getResources().getDisplayMetrics().density + 0.5f);
    }
}
