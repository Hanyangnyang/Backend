package life.hanyang.core.academic.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import life.hanyang.core.academic.domain.AcademicPeriodType;
import life.hanyang.core.academic.domain.AcademicSemester;

import java.time.LocalDate;

@Schema(description = "학사 일정 수정 요청 DTO")
public record AcademicPeriodUpdateRequest(
        @NotNull(message = "연도는 필수입니다.")
        @Schema(description = "연도", example = "2026")
        Integer year,

        @NotNull(message = "학기는 필수입니다.")
        @Schema(description = "학기 구분 (FIRST: 1학기, SECOND: 2학기)", example = "FIRST")
        AcademicSemester semester,

        @NotNull(message = "기간 유형은 필수입니다.")
        @Schema(description = "기간 유형 (SEMESTER: 학기중, SEASONAL: 계절학기, VACATION: 방학중)", example = "SEMESTER")
        AcademicPeriodType periodType,

        @NotBlank(message = "일정 명칭은 필수입니다.")
        @Schema(description = "일정 명칭", example = "2026학년도 1학기")
        String name,

        @NotNull(message = "시작일은 필수입니다.")
        @Schema(description = "시작일", example = "2026-03-03")
        LocalDate startDate,

        @NotNull(message = "종료일은 필수입니다.")
        @Schema(description = "종료일", example = "2026-06-23")
        LocalDate endDate
) {
}
