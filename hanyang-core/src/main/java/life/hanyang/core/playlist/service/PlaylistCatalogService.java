package life.hanyang.core.playlist.service;

import life.hanyang.core.playlist.dto.MusicSearchResponse;
import life.hanyang.core.playlist.dto.PlaylistTrackRecommendationCount;
import life.hanyang.core.playlist.dto.SpotifyTrackSearchResponse;
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
        return MusicSearchResponse.from(tracks, recommendationCounts, likedTrackIds);
    }
}
