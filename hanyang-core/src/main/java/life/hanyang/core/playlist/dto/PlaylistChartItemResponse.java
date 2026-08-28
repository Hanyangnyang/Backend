package life.hanyang.core.playlist.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "차트 개별 순위 항목 응답 DTO")
public record PlaylistChartItemResponse(
        @Schema(description = "순위 (1부터 시작)", example = "1")
        int rank,

        @Schema(description = "Spotify 트랙 ID", example = "4cOdK2wGLETKBW3PvgPWqT")
        String trackId,

        @Schema(description = "곡 제목", example = "LOVE SONG")
        String title,

        @Schema(description = "가수명", example = "유다빈밴드")
        String artist,

        @Schema(description = "앨범 커버 이미지 URL", example = "https://i.scdn.co/image/ab67616d0000b273...")
        String albumArtUrl
) {}
