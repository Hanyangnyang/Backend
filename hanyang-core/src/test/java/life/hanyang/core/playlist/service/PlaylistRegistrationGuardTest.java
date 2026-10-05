package life.hanyang.core.playlist.service;

import life.hanyang.core.global.exception.BusinessException;
import life.hanyang.core.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PlaylistRegistrationGuardTest {
    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    private final PlaylistRegistrationGuard guard = new PlaylistRegistrationGuard(
            redis, Duration.ofHours(1), Duration.ofMinutes(30), 3);
    private final UUID deviceId = UUID.randomUUID();

    @Test
    void returnsUtcExpiryFromRedisTtl() {
        when(redis.getExpire(anyString(), eq(TimeUnit.MILLISECONDS))).thenReturn(1800000L);
        Instant before = Instant.now().plusSeconds(1800);
        Instant expiry = guard.getBlockedUntil(deviceId);
        Instant after = Instant.now().plusSeconds(1800);
        assertThat(expiry).isBetween(before, after);
        verify(redis).getExpire(anyString(), eq(TimeUnit.MILLISECONDS));
    }

    @Test
    void activeCooldownReturns429WithRoundedRemainingSeconds() {
        when(redis.getExpire(anyString(), eq(TimeUnit.MILLISECONDS))).thenReturn(1501L);
        assertThatThrownBy(() -> guard.checkBlocked(deviceId))
                .isInstanceOfSatisfying(BusinessException.class, e -> {
                    assertThat(e.getErrorCode()).isEqualTo(ErrorCode.PLAYLIST_REGISTRATION_COOLDOWN);
                    assertThat(e.getMessage()).contains("2초");
                });
        assertThat(guard.isBlocked(deviceId)).isTrue();
    }

    @Test
    void expiredOrMissingCooldownAllowsRegistration() {
        when(redis.getExpire(anyString(), eq(TimeUnit.MILLISECONDS))).thenReturn(-2L, 0L, -1L);
        guard.checkBlocked(deviceId);
        guard.checkBlocked(deviceId);
        assertThat(guard.isBlocked(deviceId)).isFalse();
        assertThat(guard.getBlockedUntil(deviceId)).isNull();
    }

    @Test
    void redisReadFailureAllowsRegistrationAndStatus() {
        when(redis.getExpire(anyString(), eq(TimeUnit.MILLISECONDS)))
                .thenThrow(new IllegalStateException("Redis unavailable"));
        assertThatCode(() -> guard.checkBlocked(deviceId)).doesNotThrowAnyException();
        assertThat(guard.isBlocked(deviceId)).isFalse();
        assertThat(guard.getBlockedUntil(deviceId)).isNull();
    }

    @Test
    @SuppressWarnings("unchecked")
    void recordsFailureAtomicallyWithOneHourWindowAndThirtyMinuteCooldown() {
        guard.recordFailure(deviceId);
        verify(redis).execute(any(RedisScript.class), eq(List.of(
                "playlist:registration:{" + deviceId + "}:failures",
                "playlist:registration:{" + deviceId + "}:blocked")),
                eq("3600000"), eq("1800000"), eq("3"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void redisWriteFailureDoesNotReplaceModerationResult() {
        when(redis.execute(any(RedisScript.class), anyList(), any(), any(), any()))
                .thenThrow(new IllegalStateException("Redis unavailable"));
        assertThatCode(() -> guard.recordFailure(deviceId)).doesNotThrowAnyException();
    }
}
