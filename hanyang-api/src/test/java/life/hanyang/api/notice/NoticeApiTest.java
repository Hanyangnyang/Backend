package life.hanyang.api.notice;

import com.fasterxml.jackson.databind.ObjectMapper;
import life.hanyang.admin.config.security.*;
import life.hanyang.admin.notice.controller.NoticeAdminController;
import life.hanyang.core.auth.JwtProvider;
import life.hanyang.core.global.exception.EntityNotFoundException;
import life.hanyang.core.global.exception.GlobalExceptionHandler;
import life.hanyang.core.notice.service.*;
import life.hanyang.user.notice.controller.NoticeController;
import org.springframework.data.domain.Page;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {NoticeAdminController.class, NoticeController.class})
@Import({SecurityConfig.class, GlobalExceptionHandler.class, NoticeApiTest.Config.class})
@ContextConfiguration(classes = {NoticeAdminController.class, NoticeController.class,
        SecurityConfig.class, GlobalExceptionHandler.class, NoticeApiTest.Config.class})
class NoticeApiTest {
    @Autowired MockMvc mvc;
    @Autowired NoticeService admin;
    @Autowired NoticeQueryService user;
    private static final String BASE = "/api/v1/admin/notices";

    @BeforeEach void resetMocks() { reset(admin, user); }

    @Test
    void allAdminOperationsRequireAdminRole() throws Exception {
        for (String token : List.of("", "user-token")) {
            int expected = token.isEmpty() ? 401 : 403;
            for (String path : List.of(BASE, BASE + "/1", BASE + "/target-options")) {
                mvc.perform(get(path).header("Authorization", "Bearer " + token)).andExpect(status().is(expected));
            }
            mvc.perform(post(BASE).header("Authorization", "Bearer " + token)
                    .contentType("application/json").content("{}")).andExpect(status().is(expected));
            mvc.perform(put(BASE + "/1").header("Authorization", "Bearer " + token)
                    .contentType("application/json").content("{}")).andExpect(status().is(expected));
            mvc.perform(delete(BASE + "/1").header("Authorization", "Bearer " + token)).andExpect(status().is(expected));
        }
        verifyNoInteractions(admin, user);
    }

    @Test
    void publicQueryBindsContextAndDefaultPaginationWithoutAuthentication() throws Exception {
        var context = Map.of("transportType", "cityBus", "stationId", "jungang");
        when(user.list("transport", context, 0, 10)).thenReturn(Page.empty());
        mvc.perform(get("/api/v1/notices").param("pageKey", "transport")
                        .param("context[transportType]", "cityBus").param("context[stationId]", "jungang"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray());
        verify(user).list("transport", context, 0, 10);
    }

    @Test
    void rejectsMissingMalformedUnknownAndRepeatedQueryParameters() throws Exception {
        mvc.perform(get("/api/v1/notices")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/notices").param("pageKey", "menu").param("page", "abc"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/notices").param("pageKey", "menu").param("typo", "re11"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/notices").param("pageKey", "menu").param("context[cafeteriaCode]", "re11", "re12"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/notices").param("pageKey", "menu", "transport"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(user);
    }

    @Test
    void adminEndpointsRouteAndPreserveCommonStatusAndErrorResponses() throws Exception {
        when(admin.options()).thenReturn(List.of());
        mvc.perform(get(BASE + "/target-options").header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data").isArray());
        when(admin.listAdmin("menu", false, 1, 5)).thenReturn(Page.empty());
        mvc.perform(get(BASE).header("Authorization", "Bearer admin-token")
                        .param("pageKey", "menu").param("enabled", "false").param("page", "1").param("size", "5"))
                .andExpect(status().isOk());
        String input = """
                {"content":"공지", "enabled":true, "publishedAt":"2026-11-17T00:00:00Z",
                 "expiresAt":null, "targets":[{"page":"menu", "conditions":{}}]}
                """;
        mvc.perform(post(BASE).header("Authorization", "Bearer admin-token").contentType("application/json").content(input))
                .andExpect(status().isCreated());
        mvc.perform(put(BASE + "/1").header("Authorization", "Bearer admin-token").contentType("application/json").content(input))
                .andExpect(status().isOk());
        mvc.perform(delete(BASE + "/1").header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true));
        verify(admin).delete(1L);
        doThrow(new EntityNotFoundException("없는 공지")).when(admin).delete(99L);
        mvc.perform(delete(BASE + "/99").header("Authorization", "Bearer admin-token"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.success").value(false));
        verify(admin).create(argThat(i -> i.content().equals("공지") && i.targets().get(0).page().equals("menu")));
        verify(admin).update(eq(1L), argThat(i -> i.expiresAt() == null));
    }

    @TestConfiguration
    static class Config {
        @Bean NoticeService noticeService() { return mock(NoticeService.class); }
        @Bean NoticeQueryService noticeQueryService() { return mock(NoticeQueryService.class); }
        @Bean AdminAuthProperties adminAuthProperties() {
            var jwt = new AdminAuthProperties.Jwt();
            jwt.setSecret("test-secret-test-secret-test-secret");
            var p = new AdminAuthProperties(); p.setJwt(jwt); return p;
        }
        @Bean UserDetailsService userDetailsService() {
            return name -> User.withUsername(name).password("unused").roles(name.equals("admin") ? "ADMIN" : "USER").build();
        }
        @Bean JwtAuthenticationFilter jwtAuthenticationFilter(UserDetailsService users, AdminAuthProperties properties) {
            JwtProvider jwt = mock(JwtProvider.class);
            when(jwt.validateToken("user-token", properties.getJwt().getSecret())).thenReturn(true);
            when(jwt.getSubject("user-token", properties.getJwt().getSecret())).thenReturn("user");
            when(jwt.validateToken("admin-token", properties.getJwt().getSecret())).thenReturn(true);
            when(jwt.getSubject("admin-token", properties.getJwt().getSecret())).thenReturn("admin");
            return new JwtAuthenticationFilter(jwt, users, properties);
        }
        @Bean CustomAuthenticationEntryPoint entryPoint(ObjectMapper mapper) { return new CustomAuthenticationEntryPoint(mapper); }
        @Bean CustomAccessDeniedHandler deniedHandler(ObjectMapper mapper) { return new CustomAccessDeniedHandler(mapper); }
    }
}
