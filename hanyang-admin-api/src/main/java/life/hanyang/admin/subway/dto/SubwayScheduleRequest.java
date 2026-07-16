package life.hanyang.admin.subway.dto;

import jakarta.validation.constraints.NotNull;
import life.hanyang.core.subway.domain.SubwayStation;

public record SubwayScheduleRequest(
    @NotNull(message = "동기화할 지하철역 정보는 필수입니다.")
    SubwayStation station
) {}
