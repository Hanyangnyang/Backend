package life.hanyang.core.playlist.service;

import life.hanyang.core.global.config.CacheConfig;
import life.hanyang.core.playlist.dto.PlaylistArtistResponse;
import life.hanyang.core.playlist.dto.PlaylistRecommendationResponse;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.cache.RedisCache;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class PlaylistRecommendationCacheTest {
    @Test
    void recommendationCacheHasFiveMinuteTtlAndRoundTripsFullCard() {
        var manager = (RedisCacheManager) new CacheConfig().cacheManager(mock(RedisConnectionFactory.class));
        manager.afterPropertiesSet();
        var cache = (RedisCache) manager.getCache("playlistRecommendations");
        assertThat(cache.getCacheConfiguration().getTtl()).isEqualTo(Duration.ofMinutes(5));
        var artist = new PlaylistArtistResponse(UUID.randomUUID(), "spotify", "가수", null);
        var response = new PlaylistRecommendationResponse(List.of(new PlaylistRecommendationResponse.Item(artist,
                new PlaylistRecommendationResponse.Track("track", "노래", null, List.of(artist)),
                PlaylistRecommendationResponse.Source.DISCOVERY)));
        var serialization = cache.getCacheConfiguration().getValueSerializationPair();
        assertThat(serialization.read(serialization.write(response))).isEqualTo(response);
    }
}
