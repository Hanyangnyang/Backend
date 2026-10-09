package life.hanyang.core.playlist.service;

import life.hanyang.core.playlist.dto.MusicSearchResponse;
import life.hanyang.core.playlist.dto.PlaylistTrackRecommendationCount;
import life.hanyang.core.playlist.dto.SpotifyTrackSearchResponse;
import life.hanyang.core.playlist.dto.PlaylistArtistResponse;
import life.hanyang.core.playlist.repository.PlaylistArtistRepository;
import life.hanyang.core.playlist.repository.PlaylistSongRepository;
import life.hanyang.core.playlist.repository.PlaylistTrackLikeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlaylistCatalogService {

    private final SpotifyTrackSearchService spotifyTrackSearchService;
    private final PlaylistSongRepository playlistSongRepository;
    private final PlaylistTrackLikeRepository playlistTrackLikeRepository;
    private final PlaylistArtistRepository playlistArtistRepository;

    public MusicSearchResponse searchTracks(String keyword) {
        return searchTracks(keyword, null);
    }

    public MusicSearchResponse searchTracks(String keyword, UUID deviceId) {
        List<SpotifyTrackSearchResponse> tracks = spotifyTrackSearchService.searchTracks(
                keyword,
                SpotifyTrackSearchService.DEFAULT_SEARCH_LIMIT
        );
        if (tracks.isEmpty()) {
            return MusicSearchResponse.from(tracks);
        }

        List<String> trackIds = tracks.stream()
                .map(SpotifyTrackSearchResponse::trackId)
                .distinct()
                .toList();
        Map<String, Long> recommendationCounts = playlistSongRepository.countRecommendationsByTrackIds(trackIds).stream()
                .collect(Collectors.toMap(
                        PlaylistTrackRecommendationCount::trackId,
                        PlaylistTrackRecommendationCount::recommendationCount
                ));

        Set<String> likedTrackIds = deviceId == null ? Set.of()
                : playlistTrackLikeRepository.findLikedTrackIds(deviceId, trackIds);
        List<String> artistIds = tracks.stream().flatMap(track -> track.artists().stream())
                .map(PlaylistArtistResponse::spotifyArtistId).filter(java.util.Objects::nonNull).distinct().toList();
        Map<String, PlaylistArtistResponse> storedArtists = artistIds.isEmpty() ? Map.of()
                : playlistArtistRepository.findBySpotifyArtistIdIn(artistIds).stream()
                .map(PlaylistArtistResponse::from)
                .collect(Collectors.toMap(PlaylistArtistResponse::spotifyArtistId, Function.identity()));
        return MusicSearchResponse.from(tracks, recommendationCounts, likedTrackIds, storedArtists);
    }
}
