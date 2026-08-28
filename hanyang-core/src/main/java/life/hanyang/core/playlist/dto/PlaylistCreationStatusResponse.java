package life.hanyang.core.playlist.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Set;

@Schema(description = "곡 작성 전 사용자 기기 상태 조회 응답 DTO")
public record PlaylistCreationStatusResponse(
        @Schema(description = "오늘 추천글 작성 가능 여부 (오늘 등록 수 < 3)", example = "true")
        boolean canCreate,

        @Schema(description = "오늘 이미 등록한 곡 수", example = "1")
        long dailyCount,

        @Schema(description = "하루 최대 등록 가능 제한 수", example = "3")
        int dailyMaxLimit,

        @Schema(description = "오늘 남은 등록 가능 횟수", example = "2")
        long remainingCount,

        @Schema(description = "최근 7일 이내에 이미 추천한 Spotify 트랙 ID 목록 (중복 추천 방지용)", example = "[\"4cOdK2wGLETKBW3PvgPWqT\"]")
        Set<String> recentTrackIdsIn7Days
) {
    public static PlaylistCreationStatusResponse of(long dailyCount, int dailyMaxLimit, Set<String> recentTrackIdsIn7Days) {
        long remaining = Math.max(0, dailyMaxLimit - dailyCount);
        boolean canCreate = dailyCount < dailyMaxLimit;
        return new PlaylistCreationStatusResponse(canCreate, dailyCount, dailyMaxLimit, remaining, recentTrackIdsIn7Days);
    }
}
