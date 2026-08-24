package life.hanyang.core.menu.dto;

import java.time.LocalDate;
import java.util.List;

public record DailyMenuResponse(
        LocalDate date,
        List<MenuResponse> cafeterias
) {}
