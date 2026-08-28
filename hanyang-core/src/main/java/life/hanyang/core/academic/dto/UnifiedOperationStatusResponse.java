package life.hanyang.core.academic.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import life.hanyang.core.academic.domain.AcademicPeriodType;
import life.hanyang.core.academic.domain.AcademicSemester;
import life.hanyang.core.holiday.domain.DayType;

import java.time.LocalDate;

@Schema(description = "통합 운영 상태 응답 DTO")
public record UnifiedOperationStatusResponse(
        @Schema(description = "조회 날짜", example = "2026-08-28")
        LocalDate date,

        @Schema(description = "달력 및 공휴일 정보")
        CalendarStatus calendar,

        @Schema(description = "학사 일정 정보")
        AcademicStatus academic,

        @Schema(description = "셔틀 운행 기준 정보")
        ShuttleStatus shuttle
) {
    @Schema(description = "달력 및 공휴일 상태")
    public record CalendarStatus(
            @Schema(description = "일자 구분 (WEEKDAY, WEEKEND, HOLIDAY, NO_OPERATION)", example = "WEEKDAY")
            DayType dayType,

            @Schema(description = "공휴일 여부", example = "false")
            boolean isHoliday,

            @Schema(description = "공휴일 또는 휴무 명칭", example = "개교기념일")
            String holidayName
    ) {}

    @Schema(description = "학사 일정 상태")
    public record AcademicStatus(
            @Schema(description = "학사 연도", example = "2026")
            Integer year,

            @Schema(description = "학기 (FIRST: 1학기, SECOND: 2학기)", example = "FIRST")
            AcademicSemester semester,

            @Schema(description = "학기 구분 (SEMESTER: 학기중, SEASONAL: 계절학기, VACATION: 방학중)", example = "VACATION")
            AcademicPeriodType periodType,

            @Schema(description = "학사 일정 명칭", example = "26년 여름방학")
            String title
    ) {}

    @Schema(description = "셔틀 운행 기준 상태")
    public record ShuttleStatus(
            @Schema(description = "셔틀 정상 운행 여부 (미운행 시 false)", example = "true")
            boolean isOperating,

            @Schema(description = "셔틀 적용 학기 구분 (SEMESTER: 학기중, SEASONAL: 계절학기, VACATION: 방학중)", example = "VACATION")
            AcademicPeriodType periodType,

            @Schema(description = "셔틀 적용 요일 구분 (WEEKDAY: 평일시간표, WEEKEND: 주말시간표)", example = "WEEKDAY")
            DayType dayType,

            @Schema(description = "미운행 사유", example = "신정 셔틀 미운행")
            String noOperationReason
    ) {}
}
