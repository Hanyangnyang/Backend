package life.hanyang.core.subway.domain;

import lombok.Getter;

@Getter
public enum SubwayLine {
    LINE4("4호선"),
    SUIN("수인분당선");

    private final String apiValue;
    SubwayLine(String apiValue) { this.apiValue = apiValue; }
}