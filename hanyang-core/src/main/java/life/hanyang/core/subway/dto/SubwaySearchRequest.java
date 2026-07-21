package life.hanyang.core.subway.dto;

import life.hanyang.core.subway.domain.SubwayDayType;
import life.hanyang.core.subway.domain.SubwayDirection;
import life.hanyang.core.subway.domain.SubwayLine;
import life.hanyang.core.subway.domain.SubwayStation;

public record SubwaySearchRequest (
        SubwayStation subwayStation,
        SubwayLine subwayLine,
        SubwayDirection direction,
        SubwayDayType dayType
){}