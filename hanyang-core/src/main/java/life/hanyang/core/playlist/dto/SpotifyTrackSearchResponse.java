package life.hanyang.core.playlist.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;

@Schema(description = "외부 음원 카탈로그 검색 결과")
public record SpotifyTrackSearchResponse(
        @Schema(description = "Spotify 트랙 ID", example = "4cOdK2wGLETKBW3PvgPWqT")
        String trackId,

        @Schema(description = "곡 제목", example = "Love Lee")
        String title,

        @Schema(description = "가수명", example = "AKMU")
        String artist,

        @Schema(description = "앨범 커버 이미지 URL")
        String albumArtUrl,

        @Schema(description = "Spotify 검색 순위", example = "1")
        int rank
) implements Serializable {
}
