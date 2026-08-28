package life.hanyang.core.academic.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@Schema(description = "학기 기간 유형 (SEMESTER: 학기중, SEASONAL: 계절학기, VACATION: 방학중)", example = "SEMESTER")
public enum AcademicPeriodType {
    SEMESTER("학기중"),
    SEASONAL("계절학기"),
    VACATION("방학중");

    private final String description;
}
