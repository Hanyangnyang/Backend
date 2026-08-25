package life.hanyang.core.holiday.dto;

import life.hanyang.core.holiday.domain.DayType;

import java.time.DayOfWeek;
import java.time.LocalDate;

public record DateInfoResponse(
        LocalDate date,
        DayOfWeek dayOfWeek,
        DayType dayType,
        String name
) {
    public static DateInfoResponse of(LocalDate date, DayType dayType, String name) {
        return new DateInfoResponse(date, date.getDayOfWeek(), dayType, name);
    }
}
