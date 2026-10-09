package life.hanyang.user.playlist.controller;

import life.hanyang.core.global.exception.GlobalExceptionHandler;
import life.hanyang.core.playlist.dto.PlaylistRecommendationResponse;
import life.hanyang.core.playlist.service.PlaylistRecommendationService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PlaylistRecommendationControllerTest {
    @Test
    void requiresUuidAndReturnsEmptyItemsForNewDevice() throws Exception {
        var service = mock(PlaylistRecommendationService.class);
        var device = UUID.randomUUID();
        when(service.getRecommendations(device)).thenReturn(new PlaylistRecommendationResponse(List.of()));
        var mvc = MockMvcBuilders.standaloneSetup(new PlaylistRecommendationController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        mvc.perform(get("/api/v1/playlist/recommendations")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/playlist/recommendations").param("deviceId", "invalid"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
        mvc.perform(get("/api/v1/playlist/recommendations").param("deviceId", device.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.items").isEmpty());
        verify(service).getRecommendations(device);
    }
}
