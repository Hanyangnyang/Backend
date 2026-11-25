package life.hanyang.core.global.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import lombok.extern.slf4j.Slf4j;
import java.time.Duration;
import java.util.List;

@Slf4j
@Configuration
@EnableCaching
public class CacheConfig {

    // 캐시 기본 속성 값
    private static final Duration DEFAULT_TTL = Duration.ofHours(12);
    private static final long DEFAULT_MAX_SIZE = 200;

    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager(){
            @Override
            protected Cache getMissingCache(String name) {
                log.warn("[CacheConfig] 미등록 캐시 '{}'를 기본 설정(TTL={}, maxSize={})으로 자동 생성합니다.",
                        name, DEFAULT_TTL, DEFAULT_MAX_SIZE);
                return buildCache(name, DEFAULT_TTL, DEFAULT_MAX_SIZE);
            }
        };
        cacheManager.setCaches(List.of(
                // 단일·고정 키 캐시
                buildCache("readingRoomSeats", Duration.ofHours(24), 50),
                buildCache("weatherSummary", Duration.ofMinutes(10), 50),
                buildCache("weatherBriefing", Duration.ofMinutes(30), 50),
                buildCache("banner", Duration.ofHours(12), 50),
                buildCache("gymPeriod", Duration.ofHours(12), 50),
                buildCache("merchant", Duration.ofHours(12), 50),
                buildCache("partnership", Duration.ofHours(12), 50),

                // 제한적 조합 키 캐시
                buildCache("holidayDateInfo", Duration.ofHours(24), 200),
                buildCache("holidays", Duration.ofHours(24), 200),
                buildCache("academicOperationStatus", Duration.ofHours(24), 200),
                buildCache("academicPeriods", Duration.ofHours(24), 200),
                buildCache("playlistChart", Duration.ofHours(24), 200),
                buildCache("shuttleTimetable", Duration.ofHours(12), 200),

                // 고카디널리티 키 캐시 (EC2 메모리 제약을 고려해 상한 엄격 적용)
                buildCache("playlistRecommendations", Duration.ofMinutes(5), 500),
                buildCache("spotifyTrackSearch", Duration.ofMinutes(10), 500),
                buildCache("menu", Duration.ofHours(12), 500),
                buildCache("subwayTimetable", Duration.ofHours(12), 500)
        ));
        return cacheManager;
    }

    private Cache buildCache(String name, Duration ttl, long maxSize) {
        return new CustomXCacheCaffeineCache(
                name,
                Caffeine.newBuilder()
                        .expireAfterWrite(ttl)
                        .maximumSize(maxSize)
                        .recordStats()
                        .build()
        );
    }
}
