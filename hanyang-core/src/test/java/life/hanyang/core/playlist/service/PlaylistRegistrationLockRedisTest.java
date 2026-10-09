package life.hanyang.core.playlist.service;

import life.hanyang.core.global.exception.BusinessException;
import life.hanyang.core.global.exception.ErrorCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/** Run against an isolated, disposable local Redis using PLAYLIST_LOCK_TEST_REDIS_PORT. */
@EnabledIfEnvironmentVariable(named = "PLAYLIST_LOCK_TEST_REDIS_PORT", matches = "[0-9]+")
class PlaylistRegistrationLockRedisTest {
    private LettuceConnectionFactory connection;
    private StringRedisTemplate redis;
    private PlaylistRegistrationLock lock;
    private ScheduledExecutorService scheduler;
    private final Set<String> keys = new HashSet<>();

    @BeforeEach
    void setup() {
        connection = new LettuceConnectionFactory("127.0.0.1",
                Integer.parseInt(System.getenv("PLAYLIST_LOCK_TEST_REDIS_PORT")));
        connection.afterPropertiesSet();
        redis = new StringRedisTemplate(connection);
        scheduler = mock(ScheduledExecutorService.class);
        lock = new PlaylistRegistrationLock(redis, Duration.ofSeconds(60), Duration.ofSeconds(10), scheduler);
    }

    @AfterEach
    void cleanup() {
        for (String key : keys) redis.delete(key);
        lock.shutdown();
        connection.destroy();
    }

    @Test
    void sameDeviceIsRejectedWhileOtherDeviceCanRegisterAndOwnerCanRetryAfterCommit() {
        UUID first = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        String key = key(first);
        key(other);
        transaction().executeWithoutResult(status -> {
            lock.acquireUntilTransactionCompletion(first);
            assertThat(redis.hasKey(key)).isTrue();
            CompletableFuture.runAsync(() -> {
                assertThatThrownBy(() -> transaction().executeWithoutResult(second ->
                        lock.acquireUntilTransactionCompletion(first)))
                        .isInstanceOfSatisfying(BusinessException.class,
                                e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.PLAYLIST_REGISTRATION_IN_PROGRESS));
                transaction().executeWithoutResult(second -> lock.acquireUntilTransactionCompletion(other));
            }).orTimeout(5, TimeUnit.SECONDS).join();
            assertThat(redis.hasKey(key)).isTrue();
        });
        assertThat(redis.hasKey(key)).isFalse();
        transaction().executeWithoutResult(status -> lock.acquireUntilTransactionCompletion(first));
    }

    @Test
    void lostOwnerCannotCommitOrDeleteNewOwnersKey() {
        UUID device = UUID.randomUUID();
        String key = key(device);
        assertThatThrownBy(() -> transaction().executeWithoutResult(status -> {
            lock.acquireUntilTransactionCompletion(device);
            // Simulate expiration followed by another request acquiring this device's key.
            redis.opsForValue().set(key, "new-owner", Duration.ofSeconds(60));
        })).isInstanceOfSatisfying(BusinessException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.PLAYLIST_REGISTRATION_UNAVAILABLE));
        assertThat(redis.opsForValue().get(key)).isEqualTo("new-owner");
    }

    @Test
    void heartbeatExtendsOnlyOwnedLeaseAndRollbackReleasesIt() {
        UUID device = UUID.randomUUID();
        String key = key(device);
        transaction().executeWithoutResult(status -> {
            lock.acquireUntilTransactionCompletion(device);
            String token = redis.opsForValue().get(key);
            redis.expire(key, Duration.ofSeconds(5));
            var task = org.mockito.ArgumentCaptor.forClass(Runnable.class);
            verify(scheduler).scheduleWithFixedDelay(task.capture(), anyLong(), anyLong(), any());
            task.getValue().run();
            assertThat(redis.getExpire(key, TimeUnit.SECONDS)).isGreaterThan(50L);
            assertThat(redis.opsForValue().get(key)).isEqualTo(token);
            status.setRollbackOnly();
        });
        assertThat(redis.hasKey(key)).isFalse();
    }

    private String key(UUID device) {
        String key = "playlist:registration:{" + device + "}:lock";
        keys.add(key);
        return key;
    }

    private TransactionTemplate transaction() {
        return new TransactionTemplate(new PlaylistRegistrationLockTest.TestTransactionManager());
    }
}
