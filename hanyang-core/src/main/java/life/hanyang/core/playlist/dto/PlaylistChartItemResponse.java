package life.hanyang.core.playlist.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import life.hanyang.core.playlist.domain.PlaylistTrack;
import java.util.List;

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
        String albumArtUrl,

        @Schema(description = "현재 기기가 이 곡을 좋아요했는지 여부", example = "true")
        boolean isLiked,

        @Schema(description = "참여 순서대로 정렬된 아티스트. 미연결 곡은 빈 배열")
        List<PlaylistArtistResponse> artists
) {
    public PlaylistChartItemResponse(int rank, String trackId, String title, String artist, String albumArtUrl) {
        this(rank, trackId, title, artist, albumArtUrl, false, List.of());
    }

    public static PlaylistChartItemResponse from(int rank, PlaylistTrack track) {
        return new PlaylistChartItemResponse(rank, track.getTrackId(), track.getTitle(), track.getArtist(),
                track.getAlbumArtUrl(), false, PlaylistArtistResponse.fromTrack(track));
    }
}
