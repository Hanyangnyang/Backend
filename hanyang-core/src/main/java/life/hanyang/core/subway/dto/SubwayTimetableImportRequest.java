package life.hanyang.core.subway.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import life.hanyang.core.subway.domain.SubwayDayType;
import life.hanyang.core.subway.domain.SubwayDirection;
import life.hanyang.core.subway.domain.SubwayLine;
import life.hanyang.core.subway.domain.SubwayStation;

import java.time.LocalTime;
import java.util.List;

public record SubwayTimetableImportRequest(
        @NotNull SubwayStation station,
        @NotNull SubwayLine line,
        @NotEmpty @Valid List<SubwayTimetableItem> timetables
) {
    public record SubwayTimetableItem(
            @NotNull SubwayDirection direction,
            @NotNull SubwayDayType dayType,
            @NotNull LocalTime time,
            @NotNull String destination,
            @NotNull String trainNo
    ) {}
}
