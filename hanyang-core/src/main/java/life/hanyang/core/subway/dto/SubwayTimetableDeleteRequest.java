package life.hanyang.core.subway.dto;

import life.hanyang.core.subway.domain.SubwayDayType;
import life.hanyang.core.subway.domain.SubwayDirection;
import life.hanyang.core.subway.domain.SubwayLine;
import life.hanyang.core.subway.domain.SubwayStation;

public record SubwayTimetableDeleteRequest(
        SubwayStation station,
        SubwayLine line,
        SubwayDirection direction,
        SubwayDayType dayType
) {
    public boolean hasCondition() {
        return station != null || line != null || direction != null || dayType != null;
    }
}
