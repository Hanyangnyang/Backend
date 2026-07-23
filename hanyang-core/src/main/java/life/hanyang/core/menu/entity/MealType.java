package life.hanyang.core.menu.entity;

public enum MealType {
    BREAKFAST("조식"),
    LUNCH("중식"),
    DINNER("석식");

    private final String apiValue;
    MealType(String apiValue) { this.apiValue = apiValue; }
}
