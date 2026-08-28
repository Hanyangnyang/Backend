package life.hanyang.core.playlist.domain;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "인기 차트 유형 (RISING: 실시간 급상승, WEEKLY: 주간 차트, MONTHLY: 월간 차트)")
public enum ChartType {
    RISING,
    WEEKLY,
    MONTHLY
}
