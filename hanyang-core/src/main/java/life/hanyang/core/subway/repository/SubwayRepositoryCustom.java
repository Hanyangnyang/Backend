package life.hanyang.core.subway.repository;

import life.hanyang.core.subway.domain.*;
import java.util.List;

public interface SubwayRepositoryCustom {
    List<SubwayTimetable> findTimetableDynamic(
            SubwayStation subwayStation,
            SubwayLine subwayLine,
            SubwayDirection direction,
            SubwayDayType subwayDayType
    );
}
