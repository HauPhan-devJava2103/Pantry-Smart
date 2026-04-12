package hcmute.edu.vn.pantrysmart.adapter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
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
import hcmute.edu.vn.pantrysmart.config.FoodEmojiConfig;

/**
 * Dialog tái sử dụng: hiển thị BottomSheet chọn emoji theo nhóm.
 */
public class EmojiPickerDialog {

    public interface OnEmojiPickedListener {
        void onPicked(String emoji);
    }

    public static void show(Context context, String currentEmoji, OnEmojiPickedListener listener) {
        BottomSheetDialog dialog = new BottomSheetDialog(context);
        View content = View.inflate(context, R.layout.dialog_emoji_picker, null);
        dialog.setContentView(content);

        // Lấy dữ liệu nhóm emoji từ FoodEmojiConfig
        Map<String, String[]> groupedEmojis = FoodEmojiConfig.getGroupedEmojis();
        List<String> groupNames = new ArrayList<>(groupedEmojis.keySet());

        // Setup RecyclerView grid 5 cột
        RecyclerView rvGrid = content.findViewById(R.id.rvEmojiGrid);
        rvGrid.setLayoutManager(new GridLayoutManager(context, 5));

        // Mặc định hiển thị nhóm đầu tiên
        String[] firstGroupEmojis = groupedEmojis.get(groupNames.get(0));
        EmojiGridAdapter adapter = new EmojiGridAdapter(context, firstGroupEmojis, currentEmoji);
        rvGrid.setAdapter(adapter);

        // Khi user chọn emoji → callback + đóng dialog
        adapter.setOnEmojiSelectedListener(emoji -> {
            rvGrid.postDelayed(() -> {
                if (listener != null) {
                    listener.onPicked(emoji);
                }
                dialog.dismiss();
            }, 300);
        });

        // Setup tab nhóm (chips ngang)
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
                String[] emojis = groupedEmojis.get(groupNames.get(index));
                adapter.updateEmojis(emojis);
                rvGrid.scrollToPosition(0);
            });

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(dp(context, 4), 0, dp(context, 4), 0);
            layoutTabs.addView(tab, params);
        }

        // Nút close
        FrameLayout btnClose = content.findViewById(R.id.btnCloseEmojiPicker);
        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private static class EmojiGridAdapter extends RecyclerView.Adapter<EmojiGridAdapter.VH> {

        interface OnEmojiSelectedListener {
            void onEmojiSelected(String emoji);
        }

        private final Context context;
        private String[] emojis;
        private String selectedEmoji;
        private OnEmojiSelectedListener listener;

        EmojiGridAdapter(Context context, String[] emojis, String selectedEmoji) {
            this.context = context;
            this.emojis = emojis;
            this.selectedEmoji = selectedEmoji;
        }

        void setOnEmojiSelectedListener(OnEmojiSelectedListener listener) {
            this.listener = listener;
        }

        void updateEmojis(String[] newEmojis) {
            this.emojis = newEmojis;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            TextView tv = new TextView(context);
            int size = dp(context, 52);
            RecyclerView.LayoutParams params = new RecyclerView.LayoutParams(size, size);
            int margin = dp(context, 6);
            params.setMargins(margin, margin, margin, margin);
            tv.setLayoutParams(params);
            tv.setTextSize(26);
            tv.setGravity(Gravity.CENTER);
            return new VH(tv);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            String emoji = emojis[position];
            holder.tv.setText(emoji);

            boolean isSelected = emoji.equals(selectedEmoji);
            holder.tv.setBackgroundResource(
                    isSelected ? R.drawable.bg_emoji_cell_selected : R.drawable.bg_emoji_cell);
            holder.tv.setScaleX(isSelected ? 1.12f : 1.0f);
            holder.tv.setScaleY(isSelected ? 1.12f : 1.0f);

            holder.tv.setOnClickListener(v -> {
                selectedEmoji = emoji;
                notifyDataSetChanged();
                v.animate().scaleX(1.3f).scaleY(1.3f).setDuration(100)
                        .withEndAction(() -> v.animate()
                                .scaleX(1.12f).scaleY(1.12f)
                                .setDuration(100).start())
                        .start();
                if (listener != null)
                    listener.onEmojiSelected(emoji);
            });
        }

        @Override
        public int getItemCount() {
            return emojis != null ? emojis.length : 0;
        }

        static class VH extends RecyclerView.ViewHolder {
            final TextView tv;

            VH(@NonNull View itemView) {
                super(itemView);
                tv = (TextView) itemView;
            }
        }
    }

    // Helpers

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
