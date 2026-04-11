package hcmute.edu.vn.pantrysmart.data.local.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

/**
 * Chi tiết nguyên liệu đã trừ khi nấu ăn.
 * Ghi lại: lấy bao nhiêu, từ item nào trong tủ lạnh.
 */
@Entity(tableName = "cooking_log_items", foreignKeys = {
        @ForeignKey(entity = CookingLog.class, parentColumns = "id", childColumns = "cooking_log_id", onDelete = ForeignKey.CASCADE),
        @ForeignKey(entity = PantryItem.class, parentColumns = "id", childColumns = "pantry_item_id", onDelete = ForeignKey.SET_NULL)
})
public class CookingLogItem {

    @PrimaryKey(autoGenerate = true)
    private int id;

    @ColumnInfo(name = "cooking_log_id")
    private int cookingLogId;

    @ColumnInfo(name = "pantry_item_id")
    private Integer pantryItemId;

    @ColumnInfo(name = "item_name")
    private String itemName;

    @ColumnInfo(name = "quantity_used")
    private double quantityUsed;

    @ColumnInfo(name = "unit")
    private String unit;

    public CookingLogItem() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getCookingLogId() {
        return cookingLogId;
    }

    public void setCookingLogId(int cookingLogId) {
        this.cookingLogId = cookingLogId;
    }

    public Integer getPantryItemId() {
        return pantryItemId;
    }

    public void setPantryItemId(Integer pantryItemId) {
        this.pantryItemId = pantryItemId;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public double getQuantityUsed() {
        return quantityUsed;
    }

    public void setQuantityUsed(double quantityUsed) {
        this.quantityUsed = quantityUsed;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }
}
