package life.hanyang.core.playlist.service;

import life.hanyang.core.global.exception.BusinessException;
import life.hanyang.core.global.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class PlaylistRegistrationGuard {
    private static final DefaultRedisScript<Long> RECORD_FAILURE = new DefaultRedisScript<>("""
            if redis.call('EXISTS', KEYS[2]) == 1 then return 0 end
            local count = redis.call('INCR', KEYS[1])
            if count == 1 then redis.call('PEXPIRE', KEYS[1], ARGV[1]) end
            if count >= tonumber(ARGV[3]) then
                redis.call('SET', KEYS[2], '1', 'PX', ARGV[2])
                redis.call('DEL', KEYS[1])
            end
            return count
            """, Long.class);

    private final StringRedisTemplate redis;
    private final Duration failureWindow;
    private final Duration cooldown;
    private final int failureLimit;

    public PlaylistRegistrationGuard(StringRedisTemplate redis,
            @Value("${playlist.registration.failure-window:1h}") Duration failureWindow,
            @Value("${playlist.registration.cooldown:30m}") Duration cooldown,
            @Value("${playlist.registration.failure-limit:3}") int failureLimit) {
        if (failureWindow.isNegative() || failureWindow.isZero() || failureWindow.toMillis() < 1
                || cooldown.isNegative() || cooldown.isZero() || cooldown.toMillis() < 1 || failureLimit < 1) {
            throw new IllegalArgumentException("Registration guard settings must be positive");
        }
        this.redis = redis;
        this.failureWindow = failureWindow;
        this.cooldown = cooldown;
        this.failureLimit = failureLimit;
    }

    public void checkBlocked(UUID deviceId) {
        long remaining = remainingSeconds(deviceId);
        if (remaining > 0) {
            throw new BusinessException("부적절한 내용으로 반복 거절되어 등록이 제한되었습니다. "
                    + remaining + "초 후 다시 시도해 주세요.", ErrorCode.PLAYLIST_REGISTRATION_COOLDOWN);
        }
    }

    public boolean isBlocked(UUID deviceId) {
        return getBlockedUntil(deviceId) != null;
    }

    private long remainingSeconds(UUID deviceId) {
        Instant blockedUntil = getBlockedUntil(deviceId);
        if (blockedUntil == null) return 0;
        long millis = Duration.between(Instant.now(), blockedUntil).toMillis();
        return millis <= 0 ? 0 : (millis + 999) / 1000;
    }

    public Instant getBlockedUntil(UUID deviceId) {
        Instant queriedAt = Instant.now();
        try {
            Long ttl = redis.getExpire(key(deviceId, "blocked"), TimeUnit.MILLISECONDS);
            if (ttl == null || ttl <= 0) return null;
            Instant blockedUntil = queriedAt.plusMillis(ttl);
            return blockedUntil.isAfter(Instant.now()) ? blockedUntil : null;
        } catch (RuntimeException exception) {
            log.warn("[PlaylistRegistrationGuard] Redis 조회 실패: 등록 제한 생략", exception);
            return null;
        }
    }

    public void recordFailure(UUID deviceId) {
        try {
            redis.execute(RECORD_FAILURE,
                    List.of(key(deviceId, "failures"), key(deviceId, "blocked")),
                    Long.toString(failureWindow.toMillis()), Long.toString(cooldown.toMillis()),
                    Integer.toString(failureLimit));
        } catch (RuntimeException exception) {
            log.warn("[PlaylistRegistrationGuard] Redis 집계 실패: 기존 콘텐츠 거절 결과 유지", exception);
        }
    }

    private String key(UUID deviceId, String suffix) {
        // Both keys share a Redis Cluster hash slot for atomic Lua execution.
        return "playlist:registration:{" + deviceId + "}:" + suffix;
    }
}
