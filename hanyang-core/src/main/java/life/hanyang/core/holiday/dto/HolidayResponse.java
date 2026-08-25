package life.hanyang.core.holiday.dto;

import life.hanyang.core.holiday.domain.DayType;
import life.hanyang.core.holiday.domain.Holiday;

import java.time.LocalDate;

public record HolidayResponse(
        Long id,
        LocalDate date,
        String name,
        DayType dayType
) {
    public static HolidayResponse from(Holiday holiday) {
        return new HolidayResponse(
                holiday.getId(),
                holiday.getDate(),
                holiday.getName(),
                holiday.getDayType()
        );
    }
}
