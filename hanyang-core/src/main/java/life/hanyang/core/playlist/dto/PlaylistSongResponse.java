package life.hanyang.core.playlist.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import life.hanyang.core.playlist.domain.Genre;
import life.hanyang.core.playlist.domain.PlaylistSong;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Schema(description = "곡 추천 상세 응답 DTO")
public record PlaylistSongResponse(
        @Schema(description = "추천글 고유 ID", example = "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d")
        UUID id,

        @Schema(description = "Spotify 음원 트랙 ID", example = "4cOdK2wGLETKBW3PvgPWqT")
        String trackId,

        @Schema(description = "곡 제목", example = "LOVE SONG")
        String title,

        @Schema(description = "가수명", example = "유다빈밴드")
        String artist,

        @Schema(description = "앨범 커버 이미지 URL", example = "https://i.scdn.co/image/...")
        String albumArtUrl,

        @Schema(description = "추천 코멘트", example = "과제할 때 들으면 집중 진짜 잘 됩니다!")
        String comment,

        @Schema(description = "작성 기기 식별자 ID", example = "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d")
        UUID deviceId,

        @Schema(description = "선택된 장르 목록 (1~3개)", example = "[\"KPOP\", \"BAND\"]")
        Set<Genre> genres,

        @Schema(description = "해당 음원의 전체 누적 재생수", example = "128")
        Integer totalPlayCount,

        @Schema(description = "10대 이모지 리액션 목록")
        List<PlaylistReactionItemResponse> reactions,

        @Schema(description = "작성 일시 (KST/UTC)", example = "2026-08-27T10:15:30Z")
        Instant createdAt,

        @Schema(description = "수정 일시 (KST/UTC)", example = "2026-08-27T10:15:30Z")
        Instant updatedAt
) {
    public static PlaylistSongResponse of(PlaylistSong song) {
        return of(song, Collections.emptyList());
    }

    public static PlaylistSongResponse of(PlaylistSong song, List<PlaylistReactionItemResponse> reactions) {
        return new PlaylistSongResponse(
                song.getId(),
                song.getTrackId(),
                song.getTitle(),
                song.getArtist(),
                song.getAlbumArtUrl(),
                song.getComment(),
                song.getDeviceId(),
                song.getGenres(),
                song.getTotalPlayCount() != null ? song.getTotalPlayCount() : 0,
                (reactions != null) ? reactions : Collections.emptyList(),
                song.getCreatedAt(),
                song.getUpdatedAt()
        );
    }
}
