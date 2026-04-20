package hcmute.edu.vn.pantrysmart.data.local.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

// Lịch sử nấu ăn.
// Ghi lại mỗi lần user nấu một món (AI gợi ý hoặc tự nấu).
@Entity(tableName = "cooking_logs")
public class CookingLog {

    @PrimaryKey(autoGenerate = true)
    private int id;

    // Tên món đã nấu (do AI gợi ý hoặc user tự nhập)
    @ColumnInfo(name = "dish_name")
    private String dishName;

    @ColumnInfo(name = "cooked_at")
    private long cookedAt;

    // Số nguyên liệu đã trừ
    @ColumnInfo(name = "ingredients_deducted")
    private int ingredientsDeducted;

    // URL ảnh món ăn (từ Pexels)
    @ColumnInfo(name = "image_url")
    private String imageUrl;

    // Dữ liệu recipe dạng JSON để xem lại chi tiết
    @ColumnInfo(name = "recipe_json")
    private String recipeJson;

    public CookingLog() {
        this.cookedAt = System.currentTimeMillis();
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getDishName() { return dishName; }
    public void setDishName(String dishName) { this.dishName = dishName; }

    public long getCookedAt() { return cookedAt; }
    public void setCookedAt(long cookedAt) { this.cookedAt = cookedAt; }

    public int getIngredientsDeducted() { return ingredientsDeducted; }
    public void setIngredientsDeducted(int ingredientsDeducted) { this.ingredientsDeducted = ingredientsDeducted; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getRecipeJson() { return recipeJson; }
    public void setRecipeJson(String recipeJson) { this.recipeJson = recipeJson; }
}
