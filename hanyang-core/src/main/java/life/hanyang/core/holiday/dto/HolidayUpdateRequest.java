package life.hanyang.core.holiday.dto;

import life.hanyang.core.holiday.domain.DayType;

public record HolidayUpdateRequest(
        String name,
        DayType dayType
) {}
