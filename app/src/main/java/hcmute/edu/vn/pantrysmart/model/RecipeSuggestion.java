package hcmute.edu.vn.pantrysmart.model;

import java.util.ArrayList;
import java.util.List;

/**
 * POJO chứa thông tin một gợi ý món ăn từ Gemini AI.
 * Không lưu DB — chỉ dùng trong memory để hiển thị UI.
 */
public class RecipeSuggestion {

    private String dishName;
    private String description;
    private String cookingTime;
    private String difficulty;
    private List<String> matchedIngredients;
    private List<String> steps;
    private String imageSearch;

    public RecipeSuggestion() {
        this.matchedIngredients = new ArrayList<>();
        this.steps = new ArrayList<>();
    }

    public String getDishName() { return dishName; }
    public void setDishName(String dishName) { this.dishName = dishName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCookingTime() { return cookingTime; }
    public void setCookingTime(String cookingTime) { this.cookingTime = cookingTime; }

    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }

    public List<String> getMatchedIngredients() { return matchedIngredients; }
    public void setMatchedIngredients(List<String> matchedIngredients) { this.matchedIngredients = matchedIngredients; }

    public List<String> getSteps() { return steps; }
    public void setSteps(List<String> steps) { this.steps = steps; }

    public String getImageSearch() { return imageSearch; }
    public void setImageSearch(String imageSearch) { this.imageSearch = imageSearch; }
}
