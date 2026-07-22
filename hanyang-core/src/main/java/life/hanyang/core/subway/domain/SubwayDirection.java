package life.hanyang.core.subway.domain;

import lombok.Getter;

@Getter
public enum SubwayDirection {
    UPWARD("상행"),
    DOWNWARD("하행");

    private final String apiValue;
    SubwayDirection(String apiValue) { this.apiValue = apiValue; }
}

