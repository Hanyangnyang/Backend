package life.hanyang.core.academic.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@Schema(description = "학기 구분 (FIRST: 1학기, SECOND: 2학기)", example = "FIRST")
public enum AcademicSemester {
    FIRST("1학기"),
    SECOND("2학기");

    private final String description;
}
