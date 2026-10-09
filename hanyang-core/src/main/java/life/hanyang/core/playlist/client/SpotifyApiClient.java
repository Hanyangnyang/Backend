package life.hanyang.core.playlist.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import life.hanyang.core.playlist.dto.SpotifyTrackSearchResponse;
import life.hanyang.core.playlist.dto.SpotifyArtistMetadata;
import life.hanyang.core.playlist.dto.PlaylistArtistResponse;
import java.util.stream.Collectors;
import life.hanyang.core.playlist.exception.SpotifyRateLimitException;
import life.hanyang.core.playlist.exception.SpotifyServiceUnavailableException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

@Component
public class SpotifyApiClient {

    private static final Duration TOKEN_EXPIRY_MARGIN = Duration.ofSeconds(30);

    private final RestClient accountsClient;
    private final RestClient apiClient;
    private final String clientId;
    private final String clientSecret;
    private final String market;

    private volatile AccessToken cachedAccessToken;

    public SpotifyApiClient(
            RestClient.Builder builder,
            @Value("${api.spotify.accounts-base-url:https://accounts.spotify.com}") String accountsBaseUrl,
            @Value("${api.spotify.base-url:https://api.spotify.com/v1}") String apiBaseUrl,
            @Value("${api.spotify.client-id:}") String clientId,
            @Value("${api.spotify.client-secret:}") String clientSecret,
            @Value("${api.spotify.market:KR}") String market,
            @Value("${api.spotify.connect-timeout:1s}") Duration connectTimeout,
            @Value("${api.spotify.read-timeout:2s}") Duration readTimeout
    ) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeout);
        requestFactory.setReadTimeout(readTimeout);

        this.accountsClient = builder.clone()
                .baseUrl(accountsBaseUrl)
                .requestFactory(requestFactory)
                .build();
        this.apiClient = builder.clone()
                .baseUrl(apiBaseUrl)
                .defaultHeader(HttpHeaders.ACCEPT_LANGUAGE, "ko-KR,ko;q=0.9")
                .requestFactory(requestFactory)
                .build();
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.market = market;
    }

    public List<SpotifyTrackSearchResponse> searchTracks(String keyword, int limit) {
        ensureCredentialsConfigured();
        return executeWithAccessToken(accessToken -> mapSearchResults(
                fetchTracks(keyword, limit, accessToken)
        ));
    }

    public List<SpotifyArtistMetadata> getTrackArtists(String trackId) {
        ensureCredentialsConfigured();
        if (trackId == null || !trackId.matches("[A-Za-z0-9]{22}")) {
            throw new SpotifyServiceUnavailableException();
        }
        return executeWithAccessToken(accessToken -> {
            // Client credentials require an explicit market. Reject a substituted track ID below.
            SpotifyTrackItem track = apiClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/tracks/{id}")
                            .queryParam("market", market).build(trackId))
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .retrieve().body(SpotifyTrackItem.class);
            if (track == null || !trackId.equals(track.id()) || track.artists() == null || track.artists().isEmpty()) {
                throw new SpotifyServiceUnavailableException();
            }

            List<SpotifyArtistMetadata> artists = new ArrayList<>();
            for (SpotifyArtist reference : track.artists()) {
                if (reference == null || reference.id() == null || !reference.id().matches("[A-Za-z0-9]{22}")) {
                    throw new SpotifyServiceUnavailableException();
                }
                SpotifyArtist artist = apiClient.get()
                        .uri("/artists/{id}", reference.id())
                        .headers(headers -> headers.setBearerAuth(accessToken))
                        .retrieve().body(SpotifyArtist.class);
                if (artist == null || !reference.id().equals(artist.id()) || !StringUtils.hasText(artist.name())) {
                    throw new SpotifyServiceUnavailableException();
                }
                String imageUrl = artist.images() == null ? null : artist.images().stream()
                        .filter(Objects::nonNull).map(SpotifyImage::url)
                        .filter(StringUtils::hasText).findFirst().orElse(null);
                artists.add(new SpotifyArtistMetadata(artist.id(), artist.name(), imageUrl));
            }
            // Return only after every artist has been fetched successfully.
            return List.copyOf(artists);
        });
    }

    private <T> T executeWithAccessToken(Function<String, T> request) {
        try {
            return request.apply(getAccessToken());
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() != 401) {
                throw mapApiException(exception);
            }

            cachedAccessToken = null;
            try {
                return request.apply(getAccessToken());
            } catch (RestClientResponseException retryException) {
                throw mapApiException(retryException);
            } catch (RestClientException retryException) {
                throw new SpotifyServiceUnavailableException(retryException);
            }
        } catch (RestClientException exception) {
            throw new SpotifyServiceUnavailableException(exception);
        }
    }

    private List<SpotifyTrackItem> fetchTracks(String keyword, int limit, String accessToken) {
        SpotifySearchResponse response = apiClient.get()
                .uri(uriBuilder -> {
                    var builder = uriBuilder
                            .path("/search")
                            .queryParam("q", keyword)
                            .queryParam("type", "track")
                            .queryParam("limit", limit);
                    builder.queryParam("market", market);
                    return builder.build();
                })
                .headers(headers -> headers.setBearerAuth(accessToken))
                .retrieve()
                .body(SpotifySearchResponse.class);

        if (response == null || response.tracks() == null || response.tracks().items() == null) {
            return List.of();
        }
        return response.tracks().items();
    }

    private List<SpotifyTrackSearchResponse> mapSearchResults(List<SpotifyTrackItem> items) {
        List<SpotifyTrackSearchResponse> tracks = new ArrayList<>();
        int position = 0;
        for (SpotifyTrackItem item : items) {
            int rank = ++position;
            if (item == null || !StringUtils.hasText(item.id()) || !StringUtils.hasText(item.name())) {
                continue;
            }

            List<PlaylistArtistResponse> artists = item.artists() == null ? List.of() : item.artists().stream()
                    .filter(Objects::nonNull).filter(artist -> StringUtils.hasText(artist.name()))
                    .map(artist -> new PlaylistArtistResponse(null, artist.id(), artist.name(), null)).toList();
            tracks.add(new SpotifyTrackSearchResponse(item.id(), item.name(),
                    artists.stream().map(PlaylistArtistResponse::name).collect(Collectors.joining(", ")),
                    albumArtUrl(item), rank, artists));
        }
        return tracks;
    }

    private String albumArtUrl(SpotifyTrackItem item) {
        return item.album() == null || item.album().images() == null
                ? null
                : item.album().images().stream()
                        .filter(Objects::nonNull)
                        .map(SpotifyImage::url)
                        .filter(StringUtils::hasText)
                        .findFirst()
                        .orElse(null);
    }

    private RuntimeException mapApiException(RestClientResponseException exception) {
        if (exception.getStatusCode().value() == 429) {
            return new SpotifyRateLimitException(parseRetryAfter(exception), exception);
        }
        return new SpotifyServiceUnavailableException(exception);
    }

    private long parseRetryAfter(RestClientResponseException exception) {
        String retryAfter = exception.getResponseHeaders() == null
                ? null
                : exception.getResponseHeaders().getFirst(HttpHeaders.RETRY_AFTER);
        try {
            return retryAfter == null ? 1 : Long.parseLong(retryAfter);
        } catch (NumberFormatException ignored) {
            return 1;
        }
    }

    private String getAccessToken() {
        AccessToken current = cachedAccessToken;
        if (current != null && current.expiresAt().isAfter(Instant.now().plus(TOKEN_EXPIRY_MARGIN))) {
            return current.value();
        }

        synchronized (this) {
            current = cachedAccessToken;
            if (current != null && current.expiresAt().isAfter(Instant.now().plus(TOKEN_EXPIRY_MARGIN))) {
                return current.value();
            }

            try {
                SpotifyTokenResponse response = accountsClient.post()
                        .uri("/api/token")
                        .headers(headers -> headers.setBasicAuth(clientId, clientSecret))
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .body("grant_type=client_credentials")
                        .retrieve()
                        .body(SpotifyTokenResponse.class);

                if (response == null || !StringUtils.hasText(response.accessToken())) {
                    throw new SpotifyServiceUnavailableException();
                }

                long expiresIn = Math.max(response.expiresIn(), 60);
                cachedAccessToken = new AccessToken(response.accessToken(), Instant.now().plusSeconds(expiresIn));
                return cachedAccessToken.value();
            } catch (RestClientException exception) {
                throw new SpotifyServiceUnavailableException(exception);
            }
        }
    }

    private void ensureCredentialsConfigured() {
        if (!StringUtils.hasText(clientId) || !StringUtils.hasText(clientSecret)) {
            throw new SpotifyServiceUnavailableException();
        }
    }

    private record AccessToken(String value, Instant expiresAt) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SpotifyTokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("expires_in") long expiresIn
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SpotifySearchResponse(SpotifyTracks tracks) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SpotifyTracks(List<SpotifyTrackItem> items) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SpotifyTrackItem(
            String id,
            String name,
            List<SpotifyArtist> artists,
            SpotifyAlbum album
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SpotifyArtist(String id, String name, List<SpotifyImage> images) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SpotifyAlbum(List<SpotifyImage> images) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SpotifyImage(String url) {
    }
}
