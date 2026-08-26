package life.hanyang.core.playlist.dto;

import life.hanyang.core.playlist.domain.Genre;
import life.hanyang.core.playlist.domain.PlaylistSong;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record PlaylistSongResponse(
        UUID id,
        String trackId,
        String title,
        String artist,
        String albumArtUrl,
        String comment,
        UUID userId,
        Set<Genre> genres,
        Integer heartCount,
        boolean isLiked,
        Instant createdAt,
        Instant updatedAt
) {
    public static PlaylistSongResponse of(PlaylistSong song, boolean isLiked) {
        return new PlaylistSongResponse(
                song.getId(),
                song.getTrackId(),
                song.getTitle(),
                song.getArtist(),
                song.getAlbumArtUrl(),
                song.getComment(),
                song.getUserId(),
                song.getGenres(),
                song.getHeartCount(),
                isLiked,
                song.getCreatedAt(),
                song.getUpdatedAt()
        );
    }
}
