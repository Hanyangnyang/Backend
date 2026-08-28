package life.hanyang.core.academic.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AcademicPeriodType {
    SEMESTER("학기중"),
    SEASONAL("계절학기"),
    VACATION("방학중");

    private final String description;
}
