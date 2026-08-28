package life.hanyang.core.playlist.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import life.hanyang.core.playlist.domain.PlaylistTrack;
import org.springframework.data.domain.Page;

@Schema(description = "특정 음원 트랙의 상세 정보 및 추천글 목록 응답 DTO")
public record PlaylistTrackDetailResponse(
        @Schema(description = "Spotify 트랙 ID", example = "4cOdK2wGLETKBW3PvgPWqT")
        String trackId,

        @Schema(description = "곡 제목", example = "LOVE SONG")
        String title,

        @Schema(description = "가수명", example = "유다빈밴드")
        String artist,

        @Schema(description = "앨범 커버 이미지 URL", example = "https://i.scdn.co/image/ab67616d0000b273...")
        String albumArtUrl,

        @Schema(description = "해당 곡에 달린 총 추천글 수", example = "3")
        long totalSongsCount,

        @Schema(description = "해당 곡의 모든 추천글 좋아요 총합", example = "298")
        long totalHeartCount,

        @Schema(description = "학생들의 추천글 페이징 목록")
        Page<PlaylistSongResponse> songs
) {
    public static PlaylistTrackDetailResponse of(
            PlaylistTrack track,
            long totalSongsCount,
            long totalHeartCount,
            Page<PlaylistSongResponse> songs
    ) {
        return new PlaylistTrackDetailResponse(
                track.getTrackId(),
                track.getTitle(),
                track.getArtist(),
                track.getAlbumArtUrl(),
                totalSongsCount,
                totalHeartCount,
                songs
        );
    }
}
