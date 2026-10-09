package life.hanyang.user.playlist.controller;

import life.hanyang.core.global.exception.ErrorCode;
import life.hanyang.core.playlist.exception.SpotifyRateLimitException;
import life.hanyang.core.playlist.service.PlaylistService;
import life.hanyang.core.playlist.service.PlaylistChartQueryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class PlaylistControllerTest {
    @Mock private PlaylistService playlistService;
    @Mock private PlaylistChartQueryService playlistChartQueryService;

    @Test
    void like_Returns429WithRetryAfter() throws Exception {
        UUID deviceId = UUID.randomUUID();
        given(playlistService.toggleTrackLike("track-1", deviceId))
                .willThrow(new SpotifyRateLimitException(17, new RuntimeException()));
        var mvc = MockMvcBuilders.standaloneSetup(
                new PlaylistController(playlistService, playlistChartQueryService)).build();

        mvc.perform(post("/api/v1/playlist/songs/tracks/track-1/like")
                        .contentType("application/json")
                        .content("{\"deviceId\":\"" + deviceId + "\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "17"))
                .andExpect(jsonPath("$.error.code").value(ErrorCode.SPOTIFY_RATE_LIMITED.getCode()));
    }
    @Test
    void mySongsRejectsInvalidPageSizesBeforeQuerying() throws Exception {
        var mvc = MockMvcBuilders.standaloneSetup(
                new PlaylistController(playlistService, playlistChartQueryService))
                .setControllerAdvice(new life.hanyang.core.global.exception.GlobalExceptionHandler()).build();
        for (int size : new int[]{0, -1, 101, Integer.MAX_VALUE}) {
            mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/playlist/songs/my-songs")
                            .param("deviceId", UUID.randomUUID().toString()).param("size", String.valueOf(size)))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/playlist/songs/my-songs")
                        .param("deviceId", UUID.randomUUID().toString()).param("page", "-1"))
                .andExpect(status().isBadRequest());
        org.mockito.Mockito.verifyNoInteractions(playlistService);
    }
}
