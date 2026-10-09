package life.hanyang.core.playlist.service;

import life.hanyang.core.global.exception.BusinessException;
import life.hanyang.core.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PlaylistRegistrationLockTest {
    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    private final ValueOperations<String, String> values = mock(ValueOperations.class);
    private final ScheduledExecutorService scheduler = mock(ScheduledExecutorService.class);
    private final ScheduledFuture<?> future = mock(ScheduledFuture.class);
    private final PlaylistRegistrationLock lock = new PlaylistRegistrationLock(
            redis, Duration.ofSeconds(60), Duration.ofSeconds(10), scheduler);
    private final TestTransactionManager manager = new TestTransactionManager();
    private final TransactionTemplate transaction = new TransactionTemplate(manager);
    private final UUID deviceId = UUID.randomUUID();

    @BeforeEach
    void setup() {
        when(redis.opsForValue()).thenReturn(values);
        when(values.setIfAbsent(anyString(), anyString(), eq(Duration.ofSeconds(60)))).thenReturn(true);
        doReturn(future).when(scheduler).scheduleWithFixedDelay(any(Runnable.class), eq(10000L),
                eq(10000L), eq(TimeUnit.MILLISECONDS));
        when(redis.execute(any(RedisScript.class), anyList(), anyString(), anyString())).thenReturn(1L);
    }

    @Test
    void releasesOnlyAfterCommitAndCancelsRenewal() {
        doAnswer(invocation -> {
            assertThat(manager.committed).isTrue();
            return 1L;
        }).when(redis).execute(any(RedisScript.class), anyList(), anyString());
        transaction.executeWithoutResult(status -> {
            lock.acquireUntilTransactionCompletion(deviceId);
            verify(redis, never()).execute(any(RedisScript.class), anyList(), anyString());
        });
        verify(redis).execute(any(RedisScript.class), anyList(), anyString());
        verify(future).cancel(false);
    }

    @Test
    void releasesAfterRollbackWithoutCommitting() {
        assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
            lock.acquireUntilTransactionCompletion(deviceId);
            throw new IllegalArgumentException("validation failed");
        })).isInstanceOf(IllegalArgumentException.class);
        assertThat(manager.rolledBack).isTrue();
        assertThat(manager.committed).isFalse();
        verify(redis).execute(any(RedisScript.class), anyList(), anyString());
    }

    @Test
    void concurrentRegistrationReturnsConflictWithoutDeletingOwnersKey() {
        when(values.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(false);
        assertFailure(ErrorCode.PLAYLIST_REGISTRATION_IN_PROGRESS);
        verifyNoInteractions(scheduler);
        verify(redis, never()).execute(any(RedisScript.class), anyList(), anyString());
    }

    @Test
    void acquisitionFailureRejectsRegistration() {
        when(values.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                .thenThrow(new IllegalStateException("Redis unavailable"));
        assertFailure(ErrorCode.PLAYLIST_REGISTRATION_UNAVAILABLE);
        assertThat(manager.rolledBack).isTrue();
    }

    @Test
    void unknownAcquisitionResultRejectsRegistration() {
        when(values.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(null);
        assertFailure(ErrorCode.PLAYLIST_REGISTRATION_UNAVAILABLE);
    }

    @Test
    void lostOwnershipBeforeCommitRollsBack() {
        when(redis.execute(any(RedisScript.class), anyList(), anyString(), anyString())).thenReturn(0L);
        assertFailure(ErrorCode.PLAYLIST_REGISTRATION_UNAVAILABLE);
        assertThat(manager.committed).isFalse();
        assertThat(manager.rolledBack).isTrue();
    }

    @Test
    void heartbeatFailureCannotRecoverAndCommitLater() {
        when(redis.execute(any(RedisScript.class), anyList(), anyString(), anyString()))
                .thenThrow(new IllegalStateException("Redis unavailable"));
        assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
            lock.acquireUntilTransactionCompletion(deviceId);
            var task = org.mockito.ArgumentCaptor.forClass(Runnable.class);
            verify(scheduler).scheduleWithFixedDelay(task.capture(), anyLong(), anyLong(), any());
            task.getValue().run();
            when(redis.execute(any(RedisScript.class), anyList(), anyString(), anyString())).thenReturn(1L);
        })).isInstanceOfSatisfying(BusinessException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.PLAYLIST_REGISTRATION_UNAVAILABLE));
        assertThat(manager.rolledBack).isTrue();
    }

    @Test
    void releaseFailureDoesNotChangeCommittedResult() {
        when(redis.execute(any(RedisScript.class), anyList(), anyString()))
                .thenThrow(new IllegalStateException("Redis unavailable"));
        assertThatCode(() -> transaction.executeWithoutResult(status ->
                lock.acquireUntilTransactionCompletion(deviceId))).doesNotThrowAnyException();
        assertThat(manager.committed).isTrue();
    }

    @Test
    void schedulingFailureReleasesAndRollsBack() {
        when(scheduler.scheduleWithFixedDelay(any(), anyLong(), anyLong(), any()))
                .thenThrow(new java.util.concurrent.RejectedExecutionException());
        assertFailure(ErrorCode.PLAYLIST_REGISTRATION_UNAVAILABLE);
        verify(redis, times(1)).execute(any(RedisScript.class), anyList(), anyString());
    }

    @Test
    void requiresTransaction() {
        assertThatThrownBy(() -> lock.acquireUntilTransactionCompletion(deviceId))
                .isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(redis);
    }

    private void assertFailure(ErrorCode code) {
        assertThatThrownBy(() -> transaction.executeWithoutResult(status ->
                lock.acquireUntilTransactionCompletion(deviceId)))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getErrorCode()).isEqualTo(code));
    }

    static class TestTransactionManager extends AbstractPlatformTransactionManager {
        boolean committed;
        boolean rolledBack;
        @Override protected Object doGetTransaction() { return new Object(); }
        @Override protected void doBegin(Object transaction, TransactionDefinition definition) {}
        @Override protected void doCommit(DefaultTransactionStatus status) { committed = true; }
        @Override protected void doRollback(DefaultTransactionStatus status) { rolledBack = true; }
    }
}
