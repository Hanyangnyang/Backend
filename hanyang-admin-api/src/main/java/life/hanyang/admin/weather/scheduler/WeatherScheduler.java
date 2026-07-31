package life.hanyang.admin.weather.scheduler;

import life.hanyang.core.weather.service.weatherSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WeatherScheduler {

    private final weatherSyncService weatherSyncService;

    // 1. 단기예보: 02, 05, 08, 11, 14, 17, 20, 23시 15분에 실행
    @Scheduled(cron = "0 42 2,5,8,12,14,17,20,23 * * *", zone = "Asia/Seoul")
    public void scheduleVillageFcst() {
        log.info("[Scheduler] Starting scheduled syncVillageFcst...");
        try {
            weatherSyncService.syncVillageFcst();
        } catch (Exception e) {
            log.error("[Scheduler] Error in scheduleVillageFcst", e);
        }
    }

    // 2. 초단기실황: 매시 20분에 실행 (14:20, 15:20 ...)
    @Scheduled(cron = "0 44 * * * *", zone = "Asia/Seoul")
    public void scheduleUltraSrtNcst() {
        log.info("[Scheduler] Starting scheduled syncUltraSrtNcst...");
        try {
            weatherSyncService.syncUltraSrtNcst();
        } catch (Exception e) {
            log.error("[Scheduler] Error in scheduleUltraSrtNcst", e);
        }
    }

    // 3. 초단기예보: 매시 50분에 실행 (14:50, 15:50 ...)
    @Scheduled(cron = "0 45 * * * *", zone = "Asia/Seoul")
    public void scheduleUltraSrtFcst() {
        log.info("[Scheduler] Starting scheduled syncUltraSrtFcst...");
        try {
            weatherSyncService.syncUltraSrtFcst();
        } catch (Exception e) {
            log.error("[Scheduler] Error in scheduleUltraSrtFcst", e);
        }
    }
}
