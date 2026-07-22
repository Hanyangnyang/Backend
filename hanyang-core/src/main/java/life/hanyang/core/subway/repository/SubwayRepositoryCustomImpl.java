package life.hanyang.core.subway.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import life.hanyang.core.subway.domain.*;
import lombok.RequiredArgsConstructor;

import java.util.List;

import static life.hanyang.core.subway.domain.QSubwayTimetable.subwayTimetable;

@RequiredArgsConstructor
public class SubwayRepositoryCustomImpl implements SubwayRepositoryCustom {
    private final JPAQueryFactory queryFactory;

    @Override
    public List<SubwayTimetable> findTimetableDynamic(
            SubwayStation subwayStation,
            SubwayLine subwayLine,
            SubwayDirection direction,
            SubwayDayType subwayDayType
    ) {
        return queryFactory
                .selectFrom(subwayTimetable)
                .where(
                        eqSubwayStation(subwayStation),
                        eqSubwayLine(subwayLine),
                        eqDirection(direction),
                        eqSubwayDayType(subwayDayType)
                )
                .orderBy(subwayTimetable.time.asc())
                .fetch();
    }

    private BooleanExpression eqSubwayStation(SubwayStation subwayStation) {
        return subwayStation != null ? subwayTimetable.subwayStation.eq(subwayStation) : null;
    }

    private BooleanExpression eqSubwayLine(SubwayLine subwayLine) {
        return subwayLine != null ? subwayTimetable.subwayLine.eq(subwayLine) : null;
    }

    private BooleanExpression eqDirection(SubwayDirection direction) {
        return direction != null ? subwayTimetable.direction.eq(direction) : null;
    }

    private BooleanExpression eqSubwayDayType(SubwayDayType subwayDayType) {
        return subwayDayType != null ? subwayTimetable.subwayDayType.eq(subwayDayType) : null;
    }
}
