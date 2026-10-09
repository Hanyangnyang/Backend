package life.hanyang.core.playlist.service;

import jakarta.annotation.PreDestroy;
import life.hanyang.core.global.exception.BusinessException;
import life.hanyang.core.global.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/** A Redis lease held through transaction completion, including rollback. */
@Slf4j
@Component
public class PlaylistRegistrationLock {
    private static final DefaultRedisScript<Long> RENEW = new DefaultRedisScript<>("""
            if redis.call('GET', KEYS[1]) == ARGV[1] then
                return redis.call('PEXPIRE', KEYS[1], ARGV[2])
            end
            return 0
            """, Long.class);
    private static final DefaultRedisScript<Long> RELEASE = new DefaultRedisScript<>("""
            if redis.call('GET', KEYS[1]) == ARGV[1] then
                return redis.call('DEL', KEYS[1])
            end
            return 0
            """, Long.class);

    private final StringRedisTemplate redis;
    private final Duration ttl;
    private final Duration renewalInterval;
    private final ScheduledExecutorService scheduler;

    @Autowired
    public PlaylistRegistrationLock(StringRedisTemplate redis,
            @Value("${playlist.registration.lock-ttl:60s}") Duration ttl,
            @Value("${playlist.registration.lock-renewal-interval:10s}") Duration renewalInterval) {
        this(redis, ttl, renewalInterval, newScheduler());
    }

    PlaylistRegistrationLock(StringRedisTemplate redis, Duration ttl, Duration renewalInterval,
                             ScheduledExecutorService scheduler) {
        if (renewalInterval.toMillis() < 1 || ttl.toMillis() < 3 * renewalInterval.toMillis()) {
            throw new IllegalArgumentException("Lock TTL must be at least three positive renewal intervals");
        }
        this.redis = redis;
        this.ttl = ttl;
        this.renewalInterval = renewalInterval;
        this.scheduler = scheduler;
    }

    private static ScheduledExecutorService newScheduler() {
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(2, task -> {
            Thread thread = new Thread(task, "playlist-registration-lock");
            thread.setDaemon(true);
            return thread;
        });
        executor.setRemoveOnCancelPolicy(true);
        return executor;
    }

    public void acquireUntilTransactionCompletion(UUID deviceId) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()
                || !TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException("Registration lock requires an active transaction");
        }
        String key = "playlist:registration:{" + deviceId + "}:lock";
        String token = UUID.randomUUID().toString();
        Boolean acquired;
        try {
            acquired = redis.opsForValue().setIfAbsent(key, token, ttl);
        } catch (RuntimeException exception) {
            throw new BusinessException(ErrorCode.PLAYLIST_REGISTRATION_UNAVAILABLE, exception);
        }
        if (Boolean.FALSE.equals(acquired)) {
            throw new BusinessException(ErrorCode.PLAYLIST_REGISTRATION_IN_PROGRESS);
        }
        if (!Boolean.TRUE.equals(acquired)) {
            throw new BusinessException(ErrorCode.PLAYLIST_REGISTRATION_UNAVAILABLE);
        }

        Lease lease = new Lease(key, token);
        try {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void beforeCommit(boolean readOnly) {
                    lease.checkBeforeCommit();
                }

                @Override
                public void afterCompletion(int status) {
                    lease.release();
                }
            });
            lease.start();
        } catch (RuntimeException exception) {
            lease.release();
            throw new BusinessException(ErrorCode.PLAYLIST_REGISTRATION_UNAVAILABLE, exception);
        }
    }

    private final class Lease {
        private final String key;
        private final String token;
        private ScheduledFuture<?> renewal;
        private boolean lost;
        private boolean closed;

        private Lease(String key, String token) {
            this.key = key;
            this.token = token;
        }

        synchronized void start() {
            renewal = scheduler.scheduleWithFixedDelay(this::renew, renewalInterval.toMillis(),
                    renewalInterval.toMillis(), TimeUnit.MILLISECONDS);
        }

        synchronized void renew() {
            if (closed || lost) return;
            try {
                lost = !Long.valueOf(1).equals(redis.execute(RENEW, List.of(key), token,
                        Long.toString(ttl.toMillis())));
            } catch (RuntimeException exception) {
                lost = true;
                log.warn("[PlaylistRegistrationLock] 잠금 갱신 실패: 등록 커밋 중단", exception);
            }
        }

        synchronized void checkBeforeCommit() {
            renew();
            if (closed || lost) {
                throw new BusinessException(ErrorCode.PLAYLIST_REGISTRATION_UNAVAILABLE);
            }
        }

        synchronized void release() {
            if (closed) return;
            closed = true;
            if (renewal != null) renewal.cancel(false);
            try {
                redis.execute(RELEASE, List.of(key), token);
            } catch (RuntimeException exception) {
                // Do not turn an already committed registration into a failed HTTP response.
                // The key expires even if Redis is unavailable during cleanup.
                log.warn("[PlaylistRegistrationLock] 잠금 해제 실패: TTL 만료 대기", exception);
            }
        }
    }

    @PreDestroy
    public void shutdown() {
        scheduler.shutdownNow();
    }
}
