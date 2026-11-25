package life.hanyang.core.global.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
// 외부 레디스 무료티어를 계속 활성화시키기 위해 등록하는 스케줄러
public class RedisKeepAliveScheduler {

    private static final String KEEP_ALIVE_KEY = "infra:redis:keepalive";
    private static final Duration KEEP_ALIVE_TTL = Duration.ofDays(2);

    private final StringRedisTemplate stringRedisTemplate;

    @Scheduled(cron = "0 0 4 * * *", zone = "Asia/Seoul")
    public void keepAlive(){
        try{
            stringRedisTemplate.opsForValue()
                    .set(KEEP_ALIVE_KEY, Instant.now().toString(), KEEP_ALIVE_TTL);
            log.info("[RedisKeepAliveScheduler] Redis keep-alive 기록 완료");
        } catch (Exception e) {
            log.warn("[RedisKeepAliveScheduler] Redis keep-alive 실패 (예외 흡수됨): {}", e.getMessage());
        }
    }
}
