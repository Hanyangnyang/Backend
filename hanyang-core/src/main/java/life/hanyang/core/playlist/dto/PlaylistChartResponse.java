package life.hanyang.core.playlist.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import life.hanyang.core.playlist.domain.ChartType;
import life.hanyang.core.playlist.domain.Genre;

import java.time.Instant;
import java.util.List;

@Schema(description = "인기 차트 조회 전체 응답 DTO")
public record PlaylistChartResponse(
        @Schema(description = "차트 유형 (RISING, WEEKLY, MONTHLY)", example = "RISING")
        ChartType chartType,

        @Schema(description = "장르별 차트 필터 (null이면 전체 차트)", example = "KPOP", nullable = true)
        Genre genre,

        @Schema(description = "차트 스냅샷 생성/기준 시각 (KST)", example = "2026-08-27T19:00:00Z")
        Instant snapshotTime,

        @Schema(description = "집계 대상 시작 일시 (KST)", example = "2026-08-26T19:00:00Z")
        Instant startPeriod,

        @Schema(description = "집계 대상 종료 일시 (KST)", example = "2026-08-27T19:00:00Z")
        Instant endPeriod,

        @Schema(description = "화면 표시용 차트 타이틀", example = "08.27 19:00 기준 실시간 급상승")
        String displayTitle,

        @Schema(description = "순위별 음원 트랙 목록 (1위부터 정렬)")
        List<PlaylistChartItemResponse> tracks
) {
    public static PlaylistChartResponse of(
            ChartType chartType,
            Genre genre,
            Instant snapshotTime,
            Instant startPeriod,
            Instant endPeriod,
            String displayTitle,
            List<PlaylistChartItemResponse> tracks
    ) {
        return new PlaylistChartResponse(chartType, genre, snapshotTime, startPeriod, endPeriod, displayTitle, tracks);
    }
}
