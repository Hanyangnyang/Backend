package life.hanyang.core.shuttle.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import life.hanyang.core.shuttle.domain.ShuttleDayType;
import life.hanyang.core.shuttle.domain.ShuttlePeriod;
import life.hanyang.core.shuttle.domain.ShuttleRoute;

import java.time.LocalTime;

public record ShuttleTimetableRequest(
        @NotNull(message = "노선(route)은 필수 입력값입니다.")
        ShuttleRoute route,

        @NotNull(message = "운행 학기 구분(period)은 필수 입력값입니다.")
        ShuttlePeriod period,

        @NotNull(message = "운행 요일 구분(dayType)은 필수 입력값입니다.")
        ShuttleDayType dayType,

        @NotNull(message = "출발 시간(dep)은 필수 입력값입니다.")
        @JsonFormat(pattern = "HH:mm:ss")
        LocalTime dep
) {
}
