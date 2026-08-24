package life.hanyang.core.global.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;
import java.util.Map;

@Slf4j
@Configuration
@EnableCaching
public class CacheConfig implements CachingConfigurer {

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // 모든 Java 8 타입 및 복잡한 규격을 파싱하고 타입 바인딩 처리
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())       // LocalTime, LocalDate 등 지원
                .registerModule(new Jdk8Module())           // Optional 등 지원
                .registerModule(new ParameterNamesModule()) // record 생성자 매핑 지원
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS) // 날짜를 ISO-8601 문자열로 저장
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES) // 역직렬화 시 알 수 없는 필드는 무시 (유연성)
                .activateDefaultTyping(
                        LaissezFaireSubTypeValidator.instance,
                        ObjectMapper.DefaultTyping.EVERYTHING,
                        JsonTypeInfo.As.PROPERTY
                );

        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(12))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(
                        new GenericJackson2JsonRedisSerializer(objectMapper)
                ));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .withInitialCacheConfigurations(Map.of(
                        "readingRoomSeats", config.entryTtl(Duration.ofHours(24)),
                        "weatherSummary", config.entryTtl(Duration.ofMinutes(10)),
                        "weatherBriefing", config.entryTtl(Duration.ofMinutes(30)),
                        "menu", config.entryTtl(Duration.ofHours(12)),
                        "banner", config.entryTtl(Duration.ofHours(12))
                ))
                .build();
    }

    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
                log.warn("Redis 캐시 조회(GET) 실패 - Key: {}, Error: {}", key, exception.getMessage());
            }

            @Override
            public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
                log.warn("Redis 캐시 저장(PUT) 실패 - Key: {}, Error: {}", key, exception.getMessage());
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
                log.warn("Redis 캐시 삭제(EVICT) 실패 - Key: {}, Error: {}", key, exception.getMessage());
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, Cache cache) {
                log.warn("Redis 캐시 초기화(CLEAR) 실패 - Error: {}", exception.getMessage());
            }
        };
    }
}
