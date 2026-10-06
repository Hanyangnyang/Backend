package life.hanyang.core.playlist.service;

import life.hanyang.core.playlist.dto.MusicSearchResponse;
import life.hanyang.core.playlist.dto.PlaylistTrackRecommendationCount;
import life.hanyang.core.playlist.dto.SpotifyTrackSearchResponse;
import life.hanyang.core.playlist.repository.PlaylistSongRepository;
import life.hanyang.core.playlist.repository.PlaylistTrackLikeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PlaylistCatalogServiceTest {

    @Mock
    private SpotifyTrackSearchService spotifyTrackSearchService;

    @Mock
    private PlaylistSongRepository playlistSongRepository;

    @Mock
    private PlaylistTrackLikeRepository playlistTrackLikeRepository;

    @InjectMocks
    private PlaylistCatalogService playlistCatalogService;

    @Test
    @DisplayName("Spotify 검색 결과의 모든 트랙 추천글 수를 한 번의 묶음 조회로 채운다")
    void searchTracks_FetchesRecommendationCountsInOneBatch() {
        List<SpotifyTrackSearchResponse> spotifyTracks = List.of(
                new SpotifyTrackSearchResponse("track-1", "Love Lee", "AKMU", null, 1),
                new SpotifyTrackSearchResponse("track-2", "후라이의 꿈", "AKMU", null, 2),
                new SpotifyTrackSearchResponse("track-1", "Love Lee", "AKMU", null, 3)
        );
        given(spotifyTrackSearchService.searchTracks("악뮤", SpotifyTrackSearchService.DEFAULT_SEARCH_LIMIT))
                .willReturn(spotifyTracks);
        given(playlistSongRepository.countRecommendationsByTrackIds(List.of("track-1", "track-2")))
                .willReturn(List.of(new PlaylistTrackRecommendationCount("track-1", 4L)));

        MusicSearchResponse result = playlistCatalogService.searchTracks("악뮤");

        assertThat(result.tracks())
                .extracting(track -> track.trackId() + ":" + track.recommendationCount())
                .containsExactly("track-1:4", "track-2:0", "track-1:4");
        verify(playlistSongRepository).countRecommendationsByTrackIds(List.of("track-1", "track-2"));
        assertThat(result.tracks()).allMatch(track -> !track.isLiked());
    }

    @Test
    void searchTracksMapsDeviceLikesIncludingUnregisteredTracks() {
        UUID deviceId = UUID.randomUUID();
        given(spotifyTrackSearchService.searchTracks("악뮤", SpotifyTrackSearchService.DEFAULT_SEARCH_LIMIT))
                .willReturn(List.of(
                        new SpotifyTrackSearchResponse("liked", "곡", "가수", null, 1),
                        new SpotifyTrackSearchResponse("unregistered", "곡2", "가수", null, 2)));
        given(playlistTrackLikeRepository.findLikedTrackIds(deviceId, List.of("liked", "unregistered")))
                .willReturn(Set.of("liked"));

        MusicSearchResponse result = playlistCatalogService.searchTracks("악뮤", deviceId);

        assertThat(result.tracks()).extracting(track -> track.isLiked()).containsExactly(true, false);
        verify(playlistTrackLikeRepository).findLikedTrackIds(deviceId, List.of("liked", "unregistered"));
    }
}
