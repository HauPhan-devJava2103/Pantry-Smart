package hcmute.edu.vn.pantrysmart.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

import hcmute.edu.vn.pantrysmart.R;
import hcmute.edu.vn.pantrysmart.config.FoodIconConfig;
import hcmute.edu.vn.pantrysmart.model.ScannedItem;

/**
 * RecyclerView adapter hiển thị danh sách mặt hàng nhận diện được từ hóa đơn.
 * Hỗ trợ xóa từng item trước khi người dùng xác nhận lưu vào tủ lạnh.
 */
public class ScannedReviewAdapter extends RecyclerView.Adapter<ScannedReviewAdapter.ViewHolder> {

    public interface OnEditClickListener {
        void onEdit(ScannedItem item, int position);
    }

    private final List<ScannedItem> items;
    private final NumberFormat currencyFormat;
    private final OnEditClickListener editListener;

    public ScannedReviewAdapter(List<ScannedItem> items, OnEditClickListener editListener) {
        this.items = items;
        this.editListener = editListener;
        this.currencyFormat = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_scanned_review, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ScannedItem item = items.get(position);

        // Icon thực phẩm
        holder.ivIcon.setImageResource(FoodIconConfig.safeIcon(item.getEmoji()));

        // Tên mặt hàng
        holder.tvName.setText(item.getName());

        // Subtext: số lượng • giá tiền • ngăn • HSD
        String qty = formatQty(item.getQuantity()) + " " + item.getUnit();
        String price = item.getPrice() > 0
                ? currencyFormat.format(item.getPrice()) + " đ"
                : "—";
        String zone = "FREEZER".equals(item.getStorageZone()) ? "Ngăn đông" : "Ngăn mát";
        String hsd;
        if (item.getExpiryDate() != null) {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            hsd = "HSD: " + sdf.format(new java.util.Date(item.getExpiryDate()));
        } else {
            hsd = "HSD: " + item.getExpiryDays() + " ngày";
        }

        holder.tvSubtext.setText(qty + " • " + price + "\n" + zone + " • " + hsd);

        // Nút sửa
        holder.btnEdit.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION && editListener != null) {
                editListener.onEdit(item, pos);
            }
        });

        // Nút xóa
        holder.btnDelete.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) {
                items.remove(pos);
                notifyItemRemoved(pos);
                notifyItemRangeChanged(pos, items.size());
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public List<ScannedItem> getItems() {
        return items;
    }

    // Định dạng số lượng: 1.0 → "1", 0.5 → "0.5"
    private String formatQty(double qty) {
        if (qty == (long) qty)
            return String.valueOf((long) qty);
        return String.valueOf(qty);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivIcon;
        TextView tvName;
        TextView tvSubtext;
        ImageView btnEdit;
        ImageView btnDelete;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.ivScannedItemIcon);
            tvName = itemView.findViewById(R.id.tvScannedItemName);
            tvSubtext = itemView.findViewById(R.id.tvScannedItemSubtext);
            btnEdit = itemView.findViewById(R.id.btnEditScannedItem);
            btnDelete = itemView.findViewById(R.id.btnDeleteScannedItem);
        }
    }
}
