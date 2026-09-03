package life.hanyang.core.playlist.dto;

import java.util.LinkedHashSet;
import java.util.List;

public record SpotifySearchExpansion(
        List<String> trackIds,
        List<String> titles,
        List<String> artists
) {
    private static final SpotifySearchExpansion EMPTY = new SpotifySearchExpansion(List.of(), List.of(), List.of());

    public SpotifySearchExpansion {
        trackIds = List.copyOf(trackIds);
        titles = List.copyOf(titles);
        artists = List.copyOf(artists);
    }

    public static SpotifySearchExpansion empty() {
        return EMPTY;
    }

    public static SpotifySearchExpansion from(List<SpotifyTrackSearchResponse> tracks) {
        if (tracks == null || tracks.isEmpty()) {
            return empty();
        }

        LinkedHashSet<String> trackIds = new LinkedHashSet<>();
        LinkedHashSet<String> titles = new LinkedHashSet<>();
        LinkedHashSet<String> artists = new LinkedHashSet<>();
        tracks.stream()
                .sorted(java.util.Comparator.comparingInt(SpotifyTrackSearchResponse::rank))
                .forEach(track -> {
                    addIfPresent(trackIds, track.trackId());
                    addIfPresent(titles, track.title());
                    addIfPresent(artists, track.artist());
                });

        return new SpotifySearchExpansion(
                trackIds.stream().toList(),
                titles.stream().toList(),
                artists.stream().toList()
        );
    }

    private static void addIfPresent(LinkedHashSet<String> values, String value) {
        if (value != null && !value.isBlank()) {
            values.add(value);
        }
    }
}
