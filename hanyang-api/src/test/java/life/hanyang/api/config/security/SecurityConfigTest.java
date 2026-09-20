package life.hanyang.api.config.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import life.hanyang.admin.config.security.AdminAuthProperties;
import life.hanyang.admin.config.security.CustomAccessDeniedHandler;
import life.hanyang.admin.config.security.CustomAuthenticationEntryPoint;
import life.hanyang.admin.config.security.JwtAuthenticationFilter;
import life.hanyang.admin.config.security.SecurityConfig;
import life.hanyang.core.auth.JwtProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SecurityConfigTest.TestController.class)
@Import({SecurityConfig.class, SecurityConfigTest.TestSecurityConfiguration.class})
@ContextConfiguration(classes = {
        SecurityConfigTest.TestController.class,
        SecurityConfig.class,
        SecurityConfigTest.TestSecurityConfiguration.class
})
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void userApiIsAccessibleWithoutAdminAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/user/ping"))
                .andExpect(status().isOk());
    }

    @Test
    void adminApiRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/admin/ping"))
                .andExpect(status().isUnauthorized());
    }

    @RestController
    @RequestMapping("/api/v1")
    static class TestController {

        @GetMapping("/user/ping")
        ResponseEntity<Void> userPing() {
            return ResponseEntity.ok().build();
        }

        @GetMapping("/admin/ping")
        ResponseEntity<Void> adminPing() {
            return ResponseEntity.ok().build();
        }
    }

    @TestConfiguration
    static class TestSecurityConfiguration {

        @Bean
        AdminAuthProperties adminAuthProperties() {
            AdminAuthProperties.Jwt jwt = new AdminAuthProperties.Jwt();
            jwt.setSecret("test-secret-test-secret-test-secret");

            AdminAuthProperties properties = new AdminAuthProperties();
            properties.setJwt(jwt);
            return properties;
        }

        @Bean
        UserDetailsService userDetailsService() {
            return username -> null;
        }

        @Bean
        JwtAuthenticationFilter jwtAuthenticationFilter(
                AdminAuthProperties adminAuthProperties,
                UserDetailsService userDetailsService
        ) {
            return new JwtAuthenticationFilter(mock(JwtProvider.class), userDetailsService, adminAuthProperties);
        }

        @Bean
        CustomAuthenticationEntryPoint customAuthenticationEntryPoint(ObjectMapper objectMapper) {
            return new CustomAuthenticationEntryPoint(objectMapper);
        }

        @Bean
        CustomAccessDeniedHandler customAccessDeniedHandler(ObjectMapper objectMapper) {
            return new CustomAccessDeniedHandler(objectMapper);
        }
    }
}
