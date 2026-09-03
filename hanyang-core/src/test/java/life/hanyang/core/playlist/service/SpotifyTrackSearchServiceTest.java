package life.hanyang.core.playlist.service;

import life.hanyang.core.playlist.client.SpotifyApiClient;
import life.hanyang.core.playlist.dto.SpotifyTrackSearchResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SpotifyTrackSearchServiceTest {

    @Mock
    private SpotifyApiClient spotifyApiClient;

    private SpotifyTrackSearchService spotifyTrackSearchService;

    @BeforeEach
    void setUp() {
        spotifyTrackSearchService = new SpotifyTrackSearchService(spotifyApiClient);
        ReflectionTestUtils.setField(spotifyTrackSearchService, "market", "KR");
    }

    @Test
    @DisplayName("Spotify 검색 시 API에는 앞뒤 공백을 제거한 원문을 전달한다")
    void searchTracks_StripsKeyword() {
        SpotifyTrackSearchResponse track = new SpotifyTrackSearchResponse(
                "track-1", "Love Lee", "AKMU", "https://i.scdn.co/image/test", 1
        );
        given(spotifyApiClient.searchTracks("악뮤", 10)).willReturn(List.of(track));

        List<SpotifyTrackSearchResponse> result = spotifyTrackSearchService.searchTracks("  악뮤  ", 10);

        assertThat(result).containsExactly(track);
        verify(spotifyApiClient).searchTracks("악뮤", 10);
    }

    @Test
    @DisplayName("공백과 영문 대소문자가 다른 검색어는 같은 캐시 키를 사용한다")
    void cacheKey_NormalizesKeyword() {
        String first = spotifyTrackSearchService.cacheKey("  AKMU   노래 ", 10);
        String second = spotifyTrackSearchService.cacheKey("akmu 노래", 10);

        assertThat(first).isEqualTo(second);
        assertThat(first).startsWith("KR:10:");
    }

    @Test
    @DisplayName("검색 결과 개수가 다르면 서로 다른 캐시 키를 사용한다")
    void cacheKey_ContainsLimit() {
        String tenTracks = spotifyTrackSearchService.cacheKey("악뮤", 10);
        String fiveTracks = spotifyTrackSearchService.cacheKey("악뮤", 5);

        assertThat(tenTracks).isNotEqualTo(fiveTracks);
    }
}
