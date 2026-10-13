package life.hanyang.api.config.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import life.hanyang.admin.config.security.*;
import life.hanyang.admin.playlist.controller.PlaylistCommentAdminController;
import life.hanyang.core.auth.JwtProvider;
import life.hanyang.core.playlist.service.PlaylistCommentReportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.security.core.userdetails.*;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = PlaylistCommentAdminController.class)
@Import({SecurityConfig.class, PlaylistCommentReportApiTest.TestSecurityConfiguration.class,
        life.hanyang.core.global.exception.GlobalExceptionHandler.class})
@ContextConfiguration(classes = {PlaylistCommentAdminController.class, SecurityConfig.class,
        PlaylistCommentReportApiTest.TestSecurityConfiguration.class})
class PlaylistCommentReportApiTest {
    @Autowired MockMvc mvc;
    @Autowired JwtProvider jwt;
    @Autowired UserDetailsService users;
    @MockitoBean PlaylistCommentReportService reports;
    final String root = "/api/v1/admin/playlist";

    @BeforeEach void setup() {
        lenient().when(jwt.validateToken(anyString(), anyString())).thenReturn(true);
        lenient().when(jwt.getSubject(anyString(), anyString())).thenAnswer(call -> call.getArgument(0));
        lenient().when(users.loadUserByUsername("admin")).thenReturn(User.withUsername("admin").password("unused").roles("ADMIN").build());
        lenient().when(users.loadUserByUsername("user")).thenReturn(User.withUsername("user").password("unused").roles("USER").build());
    }

    @Test void allCommentModerationEndpointsRequireAdmin() throws Exception {
        for (String token : new String[]{"", "Bearer user"}) {
            var requests = List.of(get(root + "/comment-reports"),
                    patch(root + "/comment-reports/" + UUID.randomUUID()).contentType("application/json").content("{\"status\":\"REVIEWED\"}"),
                    delete(root + "/songs/" + UUID.randomUUID() + "/comments/" + UUID.randomUUID()));
            for (var request : requests) {
                if (!token.isEmpty()) request.header("Authorization", token);
                mvc.perform(request).andExpect(status().is(token.isEmpty() ? 401 : 403));
            }
        }
        verifyNoInteractions(reports);
    }

    @Test void adminCanListAndDeleteAndInvalidPageIsRejected() throws Exception {
        when(reports.list(isNull(), any())).thenReturn(Page.empty());
        mvc.perform(get(root + "/comment-reports").header("Authorization", "Bearer admin"))
                .andExpect(status().isOk());
        mvc.perform(get(root + "/comment-reports").header("Authorization", "Bearer admin").param("size", "101"))
                .andExpect(status().isBadRequest());
        UUID song = UUID.randomUUID(), comment = UUID.randomUUID();
        mvc.perform(delete(root + "/songs/" + song + "/comments/" + comment).header("Authorization", "Bearer admin"))
                .andExpect(status().isOk());
        verify(reports).deleteByAdmin(song, comment, null);
    }

    @TestConfiguration
    static class TestSecurityConfiguration {
        @Bean AdminAuthProperties adminAuthProperties() {
            var jwt = new AdminAuthProperties.Jwt();
            jwt.setSecret("test-secret-test-secret-test-secret");
            var properties = new AdminAuthProperties();
            properties.setJwt(jwt);
            return properties;
        }
        @Bean JwtProvider jwtProvider() { return mock(JwtProvider.class); }
        @Bean UserDetailsService userDetailsService() { return mock(UserDetailsService.class); }
        @Bean JwtAuthenticationFilter jwtAuthenticationFilter(JwtProvider jwt, UserDetailsService users, AdminAuthProperties properties) {
            return new JwtAuthenticationFilter(jwt, users, properties);
        }
        @Bean CustomAuthenticationEntryPoint authenticationEntryPoint(ObjectMapper mapper) { return new CustomAuthenticationEntryPoint(mapper); }
        @Bean CustomAccessDeniedHandler accessDeniedHandler(ObjectMapper mapper) { return new CustomAccessDeniedHandler(mapper); }
    }
}
