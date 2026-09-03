package life.hanyang.core.playlist.service;

import life.hanyang.core.playlist.client.SpotifyApiClient;
import life.hanyang.core.playlist.dto.SpotifyTrackSearchResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class SpotifyTrackSearchService {

    public static final int DEFAULT_SEARCH_LIMIT = 8;

    private final SpotifyApiClient spotifyApiClient;

    @Value("${api.spotify.market:KR}")
    private String market;

    @Cacheable(
            cacheNames = "spotifyTrackSearch",
            key = "#root.target.cacheKey(#keyword, #limit)",
            unless = "#result.isEmpty()"
    )
    public List<SpotifyTrackSearchResponse> searchTracks(String keyword, int limit) {
        return spotifyApiClient.searchTracks(keyword.strip(), limit);
    }

    public String cacheKey(String keyword, int limit) {
        String normalized = normalize(keyword);
        return market + ":" + limit + ":" + sha256(normalized);
    }

    static String normalize(String keyword) {
        if (keyword == null) {
            return "";
        }
        return Normalizer.normalize(keyword, Normalizer.Form.NFKC)
                .strip()
                .replaceAll("\\s+", " ")
                .toLowerCase(Locale.ROOT);
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))
            );
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 알고리즘을 사용할 수 없습니다.", exception);
        }
    }
}
