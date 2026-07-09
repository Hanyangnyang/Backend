package life.hanyang.core.gym.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import life.hanyang.core.gym.domain.GymPeriod;
import life.hanyang.core.gym.domain.GymPeriodType;
import life.hanyang.core.gym.domain.GymSemesterNo;
import java.time.LocalDate;
import java.time.LocalTime;

public record GymPeriodRequest (
        @NotNull(message = "연도는 필수 입력값입니다.")
        Integer year,

        @NotBlank(message = "학기는 필수 입력값입니다. (FIRST, SECOND 등)")
        String semester,

        @NotBlank(message = "학기 유형은 필수 입력값입니다. (SEMESTER, SEASONAL, VACATION)")
        String periodType,

        @NotBlank(message = "제목(이름)은 필수 입력값입니다.")
        String title,

        @NotNull(message = "시작 날짜는 필수 입력값입니다.")
        LocalDate startDate,

        @NotNull(message = "종료 날짜는 필수 입력값입니다.")
        LocalDate endDate,

        @NotNull(message = "시작 시간은 필수 입력값입니다.")
        LocalTime startTime,

        @NotNull(message = "종료 시간은 필수 입력값입니다.")
        LocalTime endTime,

        Boolean activeWeekend
) {
    public GymPeriod toEntity() {
        return GymPeriod.builder()
                .year(this.year)
                .semester(GymSemesterNo.valueOf(this.semester.toUpperCase()))
                .periodType(GymPeriodType.valueOf(this.periodType.toUpperCase()))
                .title(this.title)
                .startDate(this.startDate)
                .endDate(this.endDate)
                .startTime(this.startTime)
                .endTime(this.endTime)
                .activeWeekend(this.activeWeekend != null && this.activeWeekend)
                .build();
    }
}