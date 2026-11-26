package life.hanyang.core.playlist.service;

import life.hanyang.core.global.config.CacheConfig;
import life.hanyang.core.global.config.CustomXCacheCaffeineCache;
import life.hanyang.core.playlist.dto.PlaylistArtistResponse;
import life.hanyang.core.playlist.dto.PlaylistRecommendationResponse;
import org.junit.jupiter.api.Test;
import org.springframework.cache.support.SimpleCacheManager;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class PlaylistRecommendationCacheTest {

    @Test
    void recommendationCacheHasFiveMinuteTtlAndStoresFullCard() {
        var manager = (SimpleCacheManager) new CacheConfig().cacheManager();
        manager.afterPropertiesSet();

        var cache = (CustomXCacheCaffeineCache) manager.getCache("playlistRecommendations");
        assertThat(cache).isNotNull();

        long ttlMinutes = cache.getNativeCache().policy().expireAfterWrite()
                .orElseThrow()
                .getExpiresAfter(TimeUnit.MINUTES);
        assertThat(ttlMinutes).isEqualTo(5L);

        var artist = new PlaylistArtistResponse(UUID.randomUUID(), "spotify", "가수", null);
        var response = new PlaylistRecommendationResponse(List.of(new PlaylistRecommendationResponse.Item(
                artist,
                new PlaylistRecommendationResponse.Track("track", "노래", null, List.of(artist)),
                PlaylistRecommendationResponse.Source.DISCOVERY
        )));

        cache.put("v1:test", response);
        assertThat(cache.get("v1:test", PlaylistRecommendationResponse.class)).isEqualTo(response);
    }
}
