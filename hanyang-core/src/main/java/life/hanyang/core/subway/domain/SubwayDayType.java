package life.hanyang.core.subway.domain;

import lombok.Getter;

@Getter
public enum SubwayDayType {
    WEEKDAY("평일"),
    //편의를 위해 주말을 Saturday로 선언함
    WEEKEND("주말"),
    HOLIDAY("공휴일");

    private final String apiValue;
    SubwayDayType(String apiValue) { this.apiValue = apiValue; }
}