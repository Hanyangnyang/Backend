package life.hanyang.core.menu.dto;

import life.hanyang.core.menu.entity.CafeteriaCode;
import life.hanyang.core.menu.entity.MealType;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record MenuCrawlResultDto(
        CafeteriaCode code,
        LocalDate date,
        Map<String, String> operatingHours,
        List<MenuDetailDto> menus
) {
    public record MenuDetailDto(
            MealType mealType,
            String rawMenu,
            String displayMenu,
            Integer price
    ) {}
}
