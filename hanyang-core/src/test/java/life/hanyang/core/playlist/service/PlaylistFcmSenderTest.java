package life.hanyang.core.playlist.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyPairGenerator;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class PlaylistFcmSenderTest {
    @Test void signsOAuthAssertionCachesTokenAndMatchesExistingPlatformPayloads() throws Exception {
        var keys = KeyPairGenerator.getInstance("RSA");
        keys.initialize(2048);
        var pair = keys.generateKeyPair();
        String pem = "-----BEGIN PRIVATE KEY-----\\n" + Base64.getEncoder().encodeToString(pair.getPrivate().getEncoded())
                + "\\n-----END PRIVATE KEY-----";
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        var wrapper = mock(RestClient.Builder.class);
        when(wrapper.clone()).thenReturn(wrapper);
        when(wrapper.requestFactory(any())).thenReturn(builder);
        var sender = new PlaylistFcmSender(wrapper, "test-project", "test@example.com", pem);
        server.expect(requestTo("https://oauth2.googleapis.com/token"))
                .andExpect(request -> {
                    String form = ((org.springframework.mock.http.client.MockClientHttpRequest) request).getBodyAsString();
                    String assertion = Arrays.stream(form.split("&")).filter(part -> part.startsWith("assertion="))
                            .map(part -> URLDecoder.decode(part.substring(10), StandardCharsets.UTF_8)).findFirst().orElseThrow();
                    var claims = Jwts.parser().verifyWith(pair.getPublic()).build().parseSignedClaims(assertion).getPayload();
                    assertThat(claims.getIssuer()).isEqualTo("test@example.com");
                    assertThat(claims.getAudience()).contains("https://oauth2.googleapis.com/token");
                    assertThat(claims.get("scope")).isEqualTo("https://www.googleapis.com/auth/firebase.messaging");
                }).andRespond(withSuccess("{\"access_token\":\"fake-access\",\"expires_in\":3600}", MediaType.APPLICATION_JSON));
        UUID song = UUID.randomUUID();
        for (String platform : List.of("web", "ios", "android")) {
            server.expect(requestTo("https://fcm.googleapis.com/v1/projects/test-project/messages:send"))
                    .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer fake-access"))
                    .andExpect(request -> {
                        var message = new ObjectMapper().readTree(((org.springframework.mock.http.client.MockClientHttpRequest) request)
                                .getBodyAsString()).path("message");
                        assertThat(message.path("data").path("link").asText()).contains("postId=" + song);
                        assertThat(message.path("data").path("notificationId").asText()).isNotBlank();
                        assertThat(message.has("notification")).isEqualTo(!platform.equals("web"));
                        assertThat(message.has("apns")).isEqualTo(!platform.equals("web"));
                    }).andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        }
        for (String platform : List.of("web", "ios", "android")) {
            sender.send(UUID.randomUUID(), "fake-device-token", platform, "COMMENT", song, "track");
        }
        server.verify();
    }
}
