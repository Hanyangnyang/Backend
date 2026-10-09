package life.hanyang.core.playlist.dto;

import java.util.List;
import java.util.Map;

public record MusicSearchTrackResponse(
        String trackId,
        String title,
        String artist,
        String albumArtUrl,
        long recommendationCount,
        boolean isLiked,
        List<PlaylistArtistResponse> artists
) {
    public static MusicSearchTrackResponse from(SpotifyTrackSearchResponse track) {
        return from(track, 0L);
    }

    public static MusicSearchTrackResponse from(SpotifyTrackSearchResponse track, long recommendationCount) {
        return from(track, recommendationCount, false);
    }

    public static MusicSearchTrackResponse from(SpotifyTrackSearchResponse track, long recommendationCount, boolean isLiked) {
        return from(track, recommendationCount, isLiked, Map.of());
    }

    public static MusicSearchTrackResponse from(SpotifyTrackSearchResponse track, long recommendationCount, boolean isLiked,
                                                Map<String, PlaylistArtistResponse> storedArtists) {
        List<PlaylistArtistResponse> artists = track.artists().stream().map(artist -> {
            PlaylistArtistResponse stored = artist.spotifyArtistId() == null ? null : storedArtists.get(artist.spotifyArtistId());
            // Keep the live localized search name; use DB only to enrich identity and photo.
            return stored == null ? artist : new PlaylistArtistResponse(stored.id(), artist.spotifyArtistId(),
                    artist.name(), stored.imageUrl());
        }).toList();
        return new MusicSearchTrackResponse(
                track.trackId(),
                track.title(),
                track.artist(),
                track.albumArtUrl(),
                recommendationCount,
                isLiked,
                artists
        );
    }
}
