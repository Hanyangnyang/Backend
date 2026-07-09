package life.hanyang.core.shuttle.dto;

import life.hanyang.core.shuttle.domain.ShuttleDayType;
import life.hanyang.core.shuttle.domain.ShuttlePeriod;
import life.hanyang.core.shuttle.domain.ShuttleRoute;
import life.hanyang.core.shuttle.domain.ShuttleTimetable;

import java.time.LocalTime;

public record ShuttleTimetableResponse(
        Long id,
        ShuttleRoute route,
        ShuttlePeriod period,
        ShuttleDayType dayType,
        LocalTime departureTime
) {
    public static ShuttleTimetableResponse from(ShuttleTimetable entity) {
        return new ShuttleTimetableResponse(
                entity.getId(),
                entity.getShuttleRoute(),
                entity.getShuttlePeriod(),
                entity.getShuttleDayType(),
                entity.getDepartureTime()
        );
    }
}
