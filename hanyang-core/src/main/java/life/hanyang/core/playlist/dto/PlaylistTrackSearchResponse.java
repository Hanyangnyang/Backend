package life.hanyang.core.playlist.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "음원 트랙 검색 결과 응답 DTO")
public record PlaylistTrackSearchResponse(
        @Schema(description = "Spotify 트랙 ID", example = "4cOdK2wGLETKBW3PvgPWqT")
        String trackId,

        @Schema(description = "곡 제목", example = "LOVE SONG")
        String title,

        @Schema(description = "가수명", example = "유다빈밴드")
        String artist,

        @Schema(description = "앨범 커버 이미지 URL", example = "https://i.scdn.co/image/ab67616d0000b273...")
        String albumArtUrl,

        @Schema(description = "해당 음원에 달린 총 추천글 수", example = "3")
        long totalSongsCount,

        @Schema(description = "해당 음원의 모든 추천글 좋아요 총합", example = "298")
        long totalHeartCount,

        @Schema(description = "참여 순서대로 정렬된 아티스트. 미연결 곡은 빈 배열")
        List<PlaylistArtistResponse> artists
) {}
