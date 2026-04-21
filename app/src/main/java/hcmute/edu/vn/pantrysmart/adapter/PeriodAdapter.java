package hcmute.edu.vn.pantrysmart.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import hcmute.edu.vn.pantrysmart.R;

public class PeriodAdapter extends RecyclerView.Adapter<PeriodAdapter.PeriodViewHolder> {

    public static class PeriodItem {
        public String label;
        public long startTime;
        public long endTime;

        public PeriodItem(String label, long startTime, long endTime) {
            this.label = label;
            this.startTime = startTime;
            this.endTime = endTime;
        }
    }

    public interface OnPeriodSelectedListener {
        void onPeriodSelected(PeriodItem periodItem);
    }

    private List<PeriodItem> periodItems;
    private int selectedPosition = 0;
    private OnPeriodSelectedListener listener;

    public PeriodAdapter(List<PeriodItem> periodItems, int initialSelectedPos, OnPeriodSelectedListener listener) {
        this.periodItems = periodItems;
        this.selectedPosition = initialSelectedPos;
        this.listener = listener;
    }

    public void updateData(List<PeriodItem> newItems, int newSelectedPos) {
        this.periodItems = newItems;
        this.selectedPosition = newSelectedPos;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PeriodViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_period_selector, parent, false);
        return new PeriodViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PeriodViewHolder holder, int position) {
        PeriodItem item = periodItems.get(position);
        holder.tvMonthName.setText(item.label);

        if (position == selectedPosition) {
            holder.tvMonthName.setBackgroundResource(R.drawable.bg_tab_switcher_selected);
            holder.tvMonthName.setTextColor(Color.parseColor("#1E2939"));
            holder.tvMonthName.setElevation(4f);
        } else {
            holder.tvMonthName.setBackgroundResource(R.drawable.bg_tab_switcher_unselected);
            holder.tvMonthName.setTextColor(Color.parseColor("#99A1AF"));
            holder.tvMonthName.setElevation(0f);
        }

        holder.itemView.setOnClickListener(v -> {
            int currentPos = holder.getAdapterPosition();
            if (currentPos != RecyclerView.NO_POSITION && currentPos != selectedPosition) {
                int oldPos = selectedPosition;
                selectedPosition = currentPos;
                notifyItemChanged(oldPos);
                notifyItemChanged(selectedPosition);
                listener.onPeriodSelected(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return periodItems == null ? 0 : periodItems.size();
    }

    static class PeriodViewHolder extends RecyclerView.ViewHolder {
        TextView tvMonthName;

        public PeriodViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMonthName = itemView.findViewById(R.id.tvMonthName);
        }
    }
}
