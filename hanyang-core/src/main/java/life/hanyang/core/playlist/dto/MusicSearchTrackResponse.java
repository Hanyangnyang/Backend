package life.hanyang.core.playlist.dto;

public record MusicSearchTrackResponse(
        String trackId,
        String title,
        String artist,
        String albumArtUrl,
        long recommendationCount
) {
    public static MusicSearchTrackResponse from(SpotifyTrackSearchResponse track) {
        return from(track, 0L);
    }

    public static MusicSearchTrackResponse from(SpotifyTrackSearchResponse track, long recommendationCount) {
        return new MusicSearchTrackResponse(
                track.trackId(),
                track.title(),
                track.artist(),
                track.albumArtUrl(),
                recommendationCount
        );
    }
}
