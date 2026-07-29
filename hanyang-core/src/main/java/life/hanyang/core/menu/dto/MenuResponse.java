package life.hanyang.core.menu.dto;

import life.hanyang.core.menu.entity.CafeteriaCode;
import life.hanyang.core.menu.entity.MealType;

import java.util.List;
import java.util.Map;

public record MenuResponse(
        CafeteriaCode cafeteriaCode,
        String name,
        Map<String, String> operatingHours,
        List<MenuDetailResponse> menu
) {
    public record MenuDetailResponse(
            Long id,
            MealType mealType,
            Integer displayOrder,
            Integer price,
            List<String> menuItems,
            String rawMenu
    ) {}
}

