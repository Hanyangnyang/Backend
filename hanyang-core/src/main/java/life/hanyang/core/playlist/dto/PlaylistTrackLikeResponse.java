package life.hanyang.core.playlist.dto;

import life.hanyang.core.playlist.domain.PlaylistTrack;

public record PlaylistTrackLikeResponse(
        String trackId,
        String title,
        String artist,
        String albumArtUrl,
        int likeCount
) {
    public static PlaylistTrackLikeResponse of(PlaylistTrack track) {
        return new PlaylistTrackLikeResponse(
                track.getTrackId(), track.getTitle(), track.getArtist(), track.getAlbumArtUrl(),
                track.getLikeCount() != null ? track.getLikeCount() : 0
        );
    }
}
