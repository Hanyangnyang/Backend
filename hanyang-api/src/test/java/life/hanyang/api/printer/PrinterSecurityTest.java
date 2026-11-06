package life.hanyang.api.printer;

import com.fasterxml.jackson.databind.ObjectMapper;
import life.hanyang.admin.config.security.*;
import life.hanyang.admin.printer.controller.PrinterAdminController;
import life.hanyang.core.auth.JwtProvider;
import life.hanyang.core.printer.service.PrinterService;
import life.hanyang.user.printer.controller.PrinterController;
import org.springframework.data.domain.Page;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import java.util.UUID;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {PrinterAdminController.class, PrinterController.class})
@Import({SecurityConfig.class, PrinterSecurityTest.Config.class})
@ContextConfiguration(classes = {PrinterAdminController.class, PrinterController.class, SecurityConfig.class, PrinterSecurityTest.Config.class})
class PrinterSecurityTest {
    @Autowired MockMvc mvc;
    @Autowired PrinterService service;
    private static final String BASE = "/api/v1/admin/campus-map";

    @BeforeEach
    void resetServiceMock() { reset(service); }

    @Test
    void allAdminEndpointsRequireAuthentication() throws Exception {
        String id = UUID.randomUUID().toString();
        mvc.perform(get(BASE + "/printers")).andExpect(status().isUnauthorized());
        mvc.perform(post(BASE + "/printers").contentType("application/json").content("{}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(patch(BASE + "/printers/" + id).contentType("application/json").content("{}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(delete(BASE + "/printers/" + id).param("expectedVersion", "0"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get(BASE + "/printer-reports")).andExpect(status().isUnauthorized());
        mvc.perform(get(BASE + "/printer-reports/" + id)).andExpect(status().isUnauthorized());
        for (String action : new String[]{"approve", "reject"}) {
            mvc.perform(post(BASE + "/printer-reports/" + id + "/" + action).contentType("application/json").content("{}"))
                    .andExpect(status().isUnauthorized());
        }
        verifyNoInteractions(service);
    }

    @Test
    void authenticatedNonAdminCannotReadReports() throws Exception {
        mvc.perform(get(BASE + "/printer-reports").header("Authorization", "Bearer user-token"))
                .andExpect(status().isForbidden());
        mvc.perform(get(BASE + "/printers").header("Authorization", "Bearer user-token"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test
    void approvalUsesAuthenticatedAdminIdentity() throws Exception {
        UUID reportId = UUID.randomUUID();
        mvc.perform(post(BASE + "/printer-reports/" + reportId + "/approve")
                        .header("Authorization", "Bearer admin-token").contentType("application/json")
                        .content("{\"printer\":{\"buildingId\":\"b-1\",\"detailLocation\":\"1층\"}}"))
                .andExpect(status().isOk());
        verify(service).approve(eq(reportId), any(), eq("admin"));
    }

    @Test
    void publicPrinterListAndDeviceReportsDoNotRequireAdminAuthentication() throws Exception {
        when(service.list(null, null, 0, 20)).thenReturn(Page.empty());
        mvc.perform(get("/api/v1/campus-map/printers")).andExpect(status().isOk());
        mvc.perform(post("/api/v1/campus-map/printer-reports").contentType("application/json").content("""
                {"reporterDeviceId":"%s","printer":{"buildingId":"b-1","detailLocation":"1층"}}
                """.formatted(UUID.randomUUID())))
                .andExpect(status().isCreated());
        verify(service).reportNew(any());
    }

    @Test
    void adminListPassesOptionalRemovalFilter() throws Exception {
        when(service.listAdmin(null, null, null, 0, 20)).thenReturn(Page.empty());
        when(service.listAdmin(null, null, true, 0, 20)).thenReturn(Page.empty());
        when(service.listAdmin(null, null, false, 0, 20)).thenReturn(Page.empty());
        mvc.perform(get(BASE + "/printers").header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk());
        for (String removed : new String[]{"true", "false"}) {
            mvc.perform(get(BASE + "/printers").param("removed", removed).header("Authorization", "Bearer admin-token"))
                    .andExpect(status().isOk());
        }
        verify(service).listAdmin(null, null, null, 0, 20);
        verify(service).listAdmin(null, null, true, 0, 20);
        verify(service).listAdmin(null, null, false, 0, 20);
    }

    @TestConfiguration
    static class Config {
        @Bean PrinterService printerService() { return mock(PrinterService.class); }
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
