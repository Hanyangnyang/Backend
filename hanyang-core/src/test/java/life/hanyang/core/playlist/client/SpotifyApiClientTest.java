package life.hanyang.core.playlist.client;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import life.hanyang.core.playlist.dto.SpotifyTrackSearchResponse;
import life.hanyang.core.playlist.exception.SpotifyRateLimitException;
import life.hanyang.core.playlist.exception.SpotifyServiceUnavailableException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SpotifyApiClientTest {

    private HttpServer server;
    private String baseUrl;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    @DisplayName("Client Credentials 토큰으로 Spotify 곡 검색 응답을 순위 DTO로 변환한다")
    void searchTracks_Success() {
        server.createContext("/api/token", exchange -> respond(exchange, 200, """
                {"access_token":"access-token","token_type":"Bearer","expires_in":3600}
                """));
        server.createContext("/v1/search", exchange -> {
            assertThat(exchange.getRequestHeaders().getFirst("Authorization")).isEqualTo("Bearer access-token");
            assertThat(exchange.getRequestHeaders().getFirst("Accept-Language")).isEqualTo("ko-KR,ko;q=0.9");
            assertThat(exchange.getRequestURI().getQuery()).contains("type=track", "market=KR", "limit=10");
            respond(exchange, 200, """
                    {
                      "tracks": {
                        "items": [
                          {
                            "id": "track-1",
                            "name": "Love Lee",
                            "artists": [{"name": "AKMU"}, {"name": "IU"}],
                            "album": {"images": [{"url": "https://i.scdn.co/image/cover"}]}
                          }
                        ]
                      }
                    }
                    """);
        });

        SpotifyApiClient client = createClient("client-id", "client-secret");

        List<SpotifyTrackSearchResponse> tracks = client.searchTracks("악뮤", 10);

        assertThat(tracks).containsExactly(new SpotifyTrackSearchResponse(
                "track-1", "Love Lee", "AKMU", "https://i.scdn.co/image/cover", 1
        ));
    }

    @Test
    @DisplayName("Spotify 429 응답의 Retry-After 값을 예외에 보존한다")
    void searchTracks_PreservesRetryAfter_WhenRateLimited() {
        createTokenEndpoint();
        server.createContext("/v1/search", exchange -> {
            exchange.getResponseHeaders().set("Retry-After", "17");
            respond(exchange, 429, "{\"error\":{\"status\":429}}");
        });

        assertThatThrownBy(() -> createClient("client-id", "client-secret").searchTracks("악뮤", 8))
                .isInstanceOf(SpotifyRateLimitException.class)
                .extracting("retryAfterSeconds")
                .isEqualTo(17L);
    }

    @Test
    @DisplayName("Spotify 인증 정보가 없으면 서비스 이용 불가 예외를 반환한다")
    void searchTracks_ThrowsException_WhenCredentialsMissing() {
        SpotifyApiClient client = createClient("", "");

        assertThatThrownBy(() -> client.searchTracks("악뮤", 10))
                .isInstanceOf(SpotifyServiceUnavailableException.class);
    }

    private SpotifyApiClient createClient(String clientId, String clientSecret) {
        return new SpotifyApiClient(
                RestClient.builder(),
                baseUrl,
                baseUrl + "/v1",
                clientId,
                clientSecret,
                "KR",
                Duration.ofSeconds(1),
                Duration.ofSeconds(1)
        );
    }

    private void createTokenEndpoint() {
        server.createContext("/api/token", exchange -> respond(exchange, 200, """
                {"access_token":"access-token","token_type":"Bearer","expires_in":3600}
                """));
    }

    private void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
