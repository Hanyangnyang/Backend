package life.hanyang.core.menu.entity;

import lombok.Getter;

@Getter
public enum MealType {
    BREAKFAST("조식"),
    LUNCH("중식"),
    DINNER("석식");

    private final String apiValue;
    MealType(String apiValue) { this.apiValue = apiValue; }

    public static MealType fromTitle(String title) {
        if (title == null) return LUNCH;
        if (title.contains("조식")) return BREAKFAST;
        if (title.contains("석식")) return DINNER;
        return LUNCH;
    }
}
