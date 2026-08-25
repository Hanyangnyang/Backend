package life.hanyang.core.holiday.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import life.hanyang.core.holiday.domain.DayType;

import java.time.LocalDate;

public record HolidayCreateRequest(
        @NotNull(message = "날짜는 필수 입력 항목입니다.")
        LocalDate date,

        @NotBlank(message = "공휴일/일정 이름은 필수 입력 항목입니다.")
        String name,

        DayType dayType
) {}
