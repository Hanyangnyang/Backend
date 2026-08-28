package life.hanyang.core.academic.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import life.hanyang.core.academic.domain.AcademicPeriod;
import life.hanyang.core.academic.domain.AcademicPeriodType;
import life.hanyang.core.academic.domain.AcademicSemester;

import java.time.Instant;
import java.time.LocalDate;

@Schema(description = "학사 일정 상세 응답 DTO")
public record AcademicPeriodResponse(
        @Schema(description = "학사 일정 ID", example = "1")
        Long id,

        @Schema(description = "연도", example = "2026")
        Integer year,

        @Schema(description = "학기 구분 (FIRST: 1학기, SECOND: 2학기)", example = "FIRST")
        AcademicSemester semester,

        @Schema(description = "기간 유형 (SEMESTER: 학기중, SEASONAL: 계절학기, VACATION: 방학중)", example = "SEMESTER")
        AcademicPeriodType periodType,

        @Schema(description = "일정 명칭", example = "2026학년도 1학기")
        String name,

        @Schema(description = "시작일", example = "2026-03-03")
        LocalDate startDate,

        @Schema(description = "종료일", example = "2026-06-23")
        LocalDate endDate,

        @Schema(description = "등록 일시", example = "2026-08-28T10:00:00Z")
        Instant createdAt,

        @Schema(description = "수정 일시", example = "2026-08-28T10:00:00Z")
        Instant updatedAt
) {
    public static AcademicPeriodResponse from(AcademicPeriod period) {
        return new AcademicPeriodResponse(
                period.getId(),
                period.getYear(),
                period.getSemester(),
                period.getPeriodType(),
                period.getName(),
                period.getStartDate(),
                period.getEndDate(),
                period.getCreatedAt(),
                period.getUpdatedAt()
        );
    }
}
