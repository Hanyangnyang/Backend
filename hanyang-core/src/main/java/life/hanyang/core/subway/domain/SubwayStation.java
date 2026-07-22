package life.hanyang.core.subway.domain;

import lombok.Getter;

@Getter
public enum SubwayStation {
    HANDAEAP("한대앞");

    private final String apiValue;
    SubwayStation(String apiValue) { this.apiValue = apiValue; }
}
