package life.hanyang.core.academic.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AcademicSemester {
    FIRST("1학기"),
    SECOND("2학기");

    private final String description;
}
