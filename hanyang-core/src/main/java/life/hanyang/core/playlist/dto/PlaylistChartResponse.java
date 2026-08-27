package life.hanyang.core.playlist.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import life.hanyang.core.playlist.domain.ChartType;

import java.time.Instant;
import java.util.List;

@Schema(description = "인기 차트 조회 전체 응답 DTO")
public record PlaylistChartResponse(
        @Schema(description = "차트 유형 (RISING, WEEKLY, MONTHLY)", example = "RISING")
        ChartType chartType,

        @Schema(description = "차트 집계 기준 시각 (KST)", example = "2026-08-27T16:00:00Z")
        Instant updatedAt,

        @Schema(description = "순위별 음원 트랙 목록 (1위부터 정렬)")
        List<PlaylistChartItemResponse> tracks
) {
    public static PlaylistChartResponse of(ChartType chartType, List<PlaylistChartItemResponse> tracks) {
        return new PlaylistChartResponse(chartType, Instant.now(), tracks);
    }
}
