package life.hanyang.api.config.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import life.hanyang.admin.config.security.AdminAuthProperties;
import life.hanyang.admin.config.security.CustomAccessDeniedHandler;
import life.hanyang.admin.config.security.CustomAuthenticationEntryPoint;
import life.hanyang.admin.config.security.JwtAuthenticationFilter;
import life.hanyang.admin.config.security.SecurityConfig;
import life.hanyang.admin.playlist.controller.PlaylistAdminController;
import life.hanyang.core.auth.JwtProvider;
import life.hanyang.core.playlist.dto.PlaylistArtistBackfillRequest;
import life.hanyang.core.playlist.dto.PlaylistArtistBackfillResponse;
import life.hanyang.core.playlist.service.PlaylistAdminService;
import life.hanyang.core.playlist.service.PlaylistArtistSyncService;
import life.hanyang.core.playlist.service.PlaylistService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = PlaylistAdminController.class)
@Import({SecurityConfig.class, PlaylistArtistBackfillApiTest.TestSecurityConfiguration.class})
@ContextConfiguration(classes = {PlaylistAdminController.class, SecurityConfig.class,
        PlaylistArtistBackfillApiTest.TestSecurityConfiguration.class})
class PlaylistArtistBackfillApiTest {
    @Autowired MockMvc mvc;
    @Autowired JwtProvider jwtProvider;
    @Autowired UserDetailsService users;
    @MockitoBean PlaylistAdminService adminService;
    @MockitoBean PlaylistService playlistService;
    @MockitoBean PlaylistArtistSyncService syncService;

    @BeforeEach
    void authenticateTokens() {
        lenient().when(jwtProvider.validateToken(anyString(), anyString())).thenReturn(true);
        lenient().when(jwtProvider.getSubject(anyString(), anyString())).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(users.loadUserByUsername("admin")).thenReturn(User.withUsername("admin").password("unused").roles("ADMIN").build());
        lenient().when(users.loadUserByUsername("user")).thenReturn(User.withUsername("user").password("unused").roles("USER").build());
    }

    @Test
    void backfillRequiresAdminRole() throws Exception {
        mvc.perform(post("/api/v1/admin/playlist/artists/backfill")
                .contentType(MediaType.APPLICATION_JSON).content("{\"limit\":1}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/admin/playlist/artists/backfill")
                .header("Authorization", "Bearer user")
                .contentType(MediaType.APPLICATION_JSON).content("{\"limit\":1}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(syncService);
    }

    @Test
    void invalidBatchSizeNeverInvokesBackfill() throws Exception {
        mvc.perform(post("/api/v1/admin/playlist/artists/backfill")
                .header("Authorization", "Bearer admin")
                .contentType(MediaType.APPLICATION_JSON).content("{\"limit\":21}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/admin/playlist/artists/backfill")
                .header("Authorization", "Bearer admin")
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(syncService);
    }

    @Test
    void adminReceivesCursorFailuresAndRetryAfter() throws Exception {
        when(syncService.backfill(new PlaylistArtistBackfillRequest("cursor", 2)))
                .thenReturn(new PlaylistArtistBackfillResponse(1, 0, 0, List.of("failed"), "failed", false, 17L));
        mvc.perform(post("/api/v1/admin/playlist/artists/backfill")
                .header("Authorization", "Bearer admin")
                .contentType(MediaType.APPLICATION_JSON).content("{\"afterTrackId\":\"cursor\",\"limit\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.failedTrackIds[0]").value("failed"))
                .andExpect(jsonPath("$.data.nextAfterTrackId").value("failed"))
                .andExpect(jsonPath("$.data.retryAfterSeconds").value(17));
    }

    @TestConfiguration
    static class TestSecurityConfiguration {
        @Bean AdminAuthProperties adminAuthProperties() {
            AdminAuthProperties.Jwt jwt = new AdminAuthProperties.Jwt();
            jwt.setSecret("test-secret-test-secret-test-secret");
            AdminAuthProperties properties = new AdminAuthProperties();
            properties.setJwt(jwt);
            return properties;
        }
        @Bean JwtProvider jwtProvider() { return mock(JwtProvider.class); }
        @Bean UserDetailsService userDetailsService() { return mock(UserDetailsService.class); }
        @Bean JwtAuthenticationFilter jwtAuthenticationFilter(JwtProvider jwtProvider, UserDetailsService users,
                                                             AdminAuthProperties properties) {
            return new JwtAuthenticationFilter(jwtProvider, users, properties);
        }
        @Bean CustomAuthenticationEntryPoint authenticationEntryPoint(ObjectMapper mapper) {
            return new CustomAuthenticationEntryPoint(mapper);
        }
        @Bean CustomAccessDeniedHandler accessDeniedHandler(ObjectMapper mapper) {
            return new CustomAccessDeniedHandler(mapper);
        }
    }
}
