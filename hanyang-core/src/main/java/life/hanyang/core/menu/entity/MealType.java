package life.hanyang.core.menu.entity;

import lombok.Getter;
import java.util.Optional;

@Getter
public enum MealType {
    BREAKFAST("조식"),
    LUNCH("중식"),
    DINNER("석식");

    private final String apiValue;
    MealType(String apiValue) { this.apiValue = apiValue; }

    public static MealType fromTitle(String title) {
        return findFromTitle(title).orElse(LUNCH);
    }

    public static Optional<MealType> findFromTitle(String title) {
        if (title == null) return Optional.empty();
        if (title.contains("조식")) return Optional.of(BREAKFAST);
        if (title.contains("석식")) return Optional.of(DINNER);
        if (title.contains("중식")) return Optional.of(LUNCH);
        return Optional.empty();
    }
}
