package hcmute.edu.vn.pantrysmart.data.local.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Thực phẩm trong tủ lạnh.
 * Mỗi item đại diện cho một loại thực phẩm với số lượng, hạn sử dụng và vị trí
 * lưu trữ.
 */
@Entity(tableName = "pantry_items")
public class PantryItem {

    @PrimaryKey(autoGenerate = true)
    private int id;

    @ColumnInfo(name = "name")
    private String name;

    @ColumnInfo(name = "emoji")
    private String emoji;

    @ColumnInfo(name = "image_path")
    private String imagePath;

    @ColumnInfo(name = "quantity", defaultValue = "1")
    private double quantity;

    // đơn vị (kg/ quả/ chai/ gói)
    @ColumnInfo(name = "unit")
    private String unit;

    @ColumnInfo(name = "expiry_date")
    private Long expiryDate;

    @ColumnInfo(name = "added_date")
    private long addedDate;

    // khu vực lưu trữ (tủ lạnh: tủ khô/ ngăn đá)
    @ColumnInfo(name = "storage_zone", defaultValue = "MAIN")
    private String storageZone;

    @ColumnInfo(name = "category")
    private String category;

    @ColumnInfo(name = "is_active", defaultValue = "1")
    private boolean isActive;

    public PantryItem() {
        this.storageZone = "MAIN";
        this.isActive = true;
        this.quantity = 1;
        this.addedDate = System.currentTimeMillis();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmoji() {
        return emoji;
    }

    public void setEmoji(String emoji) {
        this.emoji = emoji;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public double getQuantity() {
        return quantity;
    }

    public void setQuantity(double quantity) {
        this.quantity = quantity;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public Long getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(Long expiryDate) {
        this.expiryDate = expiryDate;
    }

    public long getAddedDate() {
        return addedDate;
    }

    public void setAddedDate(long addedDate) {
        this.addedDate = addedDate;
    }

    public String getStorageZone() {
        return storageZone;
    }

    public void setStorageZone(String storageZone) {
        this.storageZone = storageZone;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    @Override
    public String toString() {
        return name + " \u2014 " + quantity + " " + unit
                + " (" + storageZone + ")";
    }
}
