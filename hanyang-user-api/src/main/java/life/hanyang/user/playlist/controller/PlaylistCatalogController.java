package life.hanyang.user.playlist.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import life.hanyang.core.global.exception.ErrorCode;
import life.hanyang.core.global.response.ApiResponse;
import life.hanyang.core.playlist.dto.MusicSearchResponse;
import life.hanyang.core.playlist.exception.SpotifyRateLimitException;
import life.hanyang.core.playlist.exception.SpotifyServiceUnavailableException;
import life.hanyang.core.playlist.service.PlaylistCatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/playlist/catalog/tracks")
@RequiredArgsConstructor
@Tag(name = "플레이리스트 곡 카탈로그 API", description = "플레이리스트 추천에 사용할 Spotify 기반 외부 곡 카탈로그를 검색합니다.")
public class PlaylistCatalogController {

    private final PlaylistCatalogService playlistCatalogService;

    @Operation(summary = "플레이리스트 후보 곡 검색", description = "검색어로 한국 Spotify 트랙을 최대 8개 검색합니다.")
    @GetMapping("/search")
    public ResponseEntity<?> searchTracks(
            @Parameter(description = "2자 이상의 검색어", required = true, example = "악뮤")
            @RequestParam(required = false) String keyword
    ) {
        if (keyword == null || keyword.strip().length() < 2) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.fail(ErrorCode.INVALID_INPUT_VALUE.getCode(), "검색어는 최소 2자 이상 입력해주세요.")
            );
        }

        try {
            MusicSearchResponse response = playlistCatalogService.searchTracks(keyword);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (SpotifyRateLimitException exception) {
            return ResponseEntity.status(ErrorCode.SPOTIFY_RATE_LIMITED.getStatus())
                    .header(HttpHeaders.RETRY_AFTER, Long.toString(exception.getRetryAfterSeconds()))
                    .body(ApiResponse.fail(
                            ErrorCode.SPOTIFY_RATE_LIMITED.getCode(),
                            ErrorCode.SPOTIFY_RATE_LIMITED.getMessage()
                    ));
        } catch (SpotifyServiceUnavailableException exception) {
            return ResponseEntity.status(ErrorCode.SPOTIFY_SERVICE_UNAVAILABLE.getStatus())
                    .body(ApiResponse.fail(
                            ErrorCode.SPOTIFY_SERVICE_UNAVAILABLE.getCode(),
                            ErrorCode.SPOTIFY_SERVICE_UNAVAILABLE.getMessage()
                    ));
        }
    }
}
