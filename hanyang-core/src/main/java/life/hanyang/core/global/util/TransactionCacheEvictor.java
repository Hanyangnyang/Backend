package life.hanyang.core.global.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionCacheEvictor {

    private final CacheManager cacheManager;

    /**
     * DB 트랜잭션이 성공적으로 커밋(afterCommit)된 직후에만 안전하게 캐시를 비웁니다.
     */
    public void evictCacheAfterCommit(String cacheName) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    doEvict(cacheName);
                }
            });
        } else {
            doEvict(cacheName);
        }
    }

    private void doEvict(String cacheName) {
        try {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
            }
        } catch (Exception e) {
            log.warn("[TransactionCacheEvictor] '{}' 캐시 무효화 중 오류 발생 (예외 흡수됨): {}", cacheName, e.getMessage());
        }
    }
}
