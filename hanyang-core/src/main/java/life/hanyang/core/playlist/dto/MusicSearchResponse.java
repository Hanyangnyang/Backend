package life.hanyang.core.playlist.dto;

import java.util.List;
import java.util.Map;

public record MusicSearchResponse(List<MusicSearchTrackResponse> tracks) {

    public MusicSearchResponse {
        tracks = List.copyOf(tracks);
    }

    public static MusicSearchResponse from(List<SpotifyTrackSearchResponse> tracks) {
        return from(tracks, Map.of());
    }

    public static MusicSearchResponse from(
            List<SpotifyTrackSearchResponse> tracks,
            Map<String, Long> recommendationCounts
    ) {
        return new MusicSearchResponse(tracks.stream()
                .map(track -> MusicSearchTrackResponse.from(
                        track,
                        recommendationCounts.getOrDefault(track.trackId(), 0L)
                ))
                .toList());
    }
}
