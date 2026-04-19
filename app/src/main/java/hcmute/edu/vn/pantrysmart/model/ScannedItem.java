package hcmute.edu.vn.pantrysmart.model;

/**
 * Model trung gian chứa thông tin một mặt hàng
 * được trích xuất từ hóa đơn siêu thị qua OCR.
 * Người dùng xem lại danh sách này trước khi lưu vào tủ lạnh.
 */
public class ScannedItem {

    private String name;
    private double quantity;
    private String unit;
    private long price;
    private String category;
    private String emoji;
    private int expiryDays;
    private String storageZone; // "FREEZER" or "MAIN"
    private Long expiryDate; // Absolute expiry date in timestamp (added for UI edit sync)

    public ScannedItem() {
        this.quantity = 1;
        this.unit = "gói";
        this.price = 0;
        this.category = "khác";
        this.emoji = "ic_food_package";
        this.expiryDays = 7;
        this.storageZone = "MAIN";
    }

    public ScannedItem(String name, double quantity, String unit, long price, String category, String emoji,
            int expiryDays) {
        this.name = name;
        this.quantity = quantity;
        this.unit = unit;
        this.price = price;
        this.category = category;
        this.emoji = emoji;
        this.expiryDays = expiryDays;
        this.storageZone = "thịt".equalsIgnoreCase(category) ? "FREEZER" : "MAIN";
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public long getPrice() {
        return price;
    }

    public void setPrice(long price) {
        this.price = price;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
        this.storageZone = "thịt".equalsIgnoreCase(category) ? "FREEZER" : "MAIN";
    }

    public String getEmoji() {
        return emoji;
    }

    public void setEmoji(String emoji) {
        this.emoji = emoji;
    }

    public int getExpiryDays() {
        return expiryDays;
    }

    public void setExpiryDays(int expiryDays) {
        this.expiryDays = expiryDays;
    }

    public String getStorageZone() {
        return storageZone;
    }

    public void setStorageZone(String storageZone) {
        this.storageZone = storageZone;
    }

    public Long getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(Long expiryDate) {
        this.expiryDate = expiryDate;
    }

    @Override
    public String toString() {
        return name + " — " + quantity + " " + unit + " (" + price + "đ)";
    }
}
