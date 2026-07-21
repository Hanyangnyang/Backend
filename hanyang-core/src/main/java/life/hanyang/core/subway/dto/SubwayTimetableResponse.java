package life.hanyang.core.subway.dto;

import life.hanyang.core.shuttle.dto.ShuttleTimetableResponse;
import life.hanyang.core.subway.domain.*;

import java.time.LocalTime;

public record SubwayTimetableResponse (
        Long id,
        SubwayStation subwayStation,
        SubwayLine subwayLine,
        SubwayDirection direction,
        SubwayDayType dayType,
        LocalTime time,
        String destination,
        String trainNo
) {
    public static SubwayTimetableResponse from(SubwayTimetable entity) {
        return new SubwayTimetableResponse(
                entity.getId(),
                entity.getSubwayStation(),
                entity.getSubwayLine(),
                entity.getDirection(),
                entity.getSubwayDayType(),
                entity.getTime(),
                entity.getDestination(),
                entity.getTrainNo()
        );
    }
}
