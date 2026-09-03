package life.hanyang.user.playlist.controller;

import life.hanyang.core.global.exception.ErrorCode;
import life.hanyang.core.global.response.ApiResponse;
import life.hanyang.core.playlist.dto.MusicSearchResponse;
import life.hanyang.core.playlist.dto.MusicSearchTrackResponse;
import life.hanyang.core.playlist.exception.SpotifyRateLimitException;
import life.hanyang.core.playlist.service.PlaylistCatalogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class PlaylistCatalogControllerTest {

    @Mock
    private PlaylistCatalogService playlistCatalogService;

    private PlaylistCatalogController playlistCatalogController;

    @BeforeEach
    void setUp() {
        playlistCatalogController = new PlaylistCatalogController(playlistCatalogService);
    }

    @Test
    @DisplayName("음원 카탈로그 검색은 공통 응답 규격으로 최대 8개를 반환한다")
    void searchTracks_Success() {
        MusicSearchResponse catalogResponse = new MusicSearchResponse(List.of(
                new MusicSearchTrackResponse(
                        "track-1", "Love Lee", "AKMU", "https://i.scdn.co/image/cover", 3L
                )
        ));
        given(playlistCatalogService.searchTracks("악뮤")).willReturn(catalogResponse);

        ResponseEntity<?> response = playlistCatalogController.searchTracks("악뮤");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isInstanceOf(ApiResponse.class);
        assertThat(((ApiResponse<?>) response.getBody()).getData())
                .isEqualTo(catalogResponse);
    }

    @Test
    @DisplayName("2자 미만 검색어는 Spotify를 호출하지 않고 공통 400 오류를 반환한다")
    void searchTracks_ReturnsBadRequest_WhenKeywordTooShort() {
        ResponseEntity<?> response = playlistCatalogController.searchTracks("악");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(((ApiResponse<?>) response.getBody()).getError().getCode())
                .isEqualTo(ErrorCode.INVALID_INPUT_VALUE.getCode());
    }

    @Test
    @DisplayName("Spotify 요청 제한은 Retry-After 헤더와 공통 429 오류를 반환한다")
    void searchTracks_ReturnsRetryAfter_WhenRateLimited() {
        given(playlistCatalogService.searchTracks("악뮤"))
                .willThrow(new SpotifyRateLimitException(17, new RuntimeException()));

        ResponseEntity<?> response = playlistCatalogController.searchTracks("악뮤");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(response.getHeaders().getFirst(HttpHeaders.RETRY_AFTER)).isEqualTo("17");
        assertThat(((ApiResponse<?>) response.getBody()).getError().getCode())
                .isEqualTo(ErrorCode.SPOTIFY_RATE_LIMITED.getCode());
    }
}
