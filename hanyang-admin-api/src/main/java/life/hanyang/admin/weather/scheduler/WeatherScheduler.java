package life.hanyang.admin.weather.scheduler;

import life.hanyang.core.weather.service.FineDustSyncService;
import life.hanyang.core.weather.service.UvSyncService;
import life.hanyang.core.weather.service.WeatherSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WeatherScheduler {

    private final WeatherSyncService weatherSyncService;
    private final FineDustSyncService fineDustSyncService;
    private final UvSyncService uvSyncService;

    // 1. 단기예보: 02, 05, 08, 11, 14, 17, 20, 23시 16분에 실행
    @Scheduled(cron = "0 16 2,5,8,11,14,17,20,23 * * *", zone = "Asia/Seoul")
    public void scheduleVillageFcst() {
        log.info("[Scheduler] Starting scheduled syncVillageFcst...");
        try {
            weatherSyncService.syncVillageFcst();
        } catch (org.springframework.web.client.ResourceAccessException e) {
            log.warn("[Scheduler] VillageFcst API Network/Timeout occurred: {}", e.getMessage());
        } catch (Exception e) {
            log.error("[Scheduler] Error in scheduleVillageFcst", e);
        }
    }

    // 2. 초단기실황: 매시 20분에 실행 (14:20, 15:20 ...)
    @Scheduled(cron = "0 20 * * * *", zone = "Asia/Seoul")
    public void scheduleUltraSrtNcst() {
        log.info("[Scheduler] Starting scheduled syncUltraSrtNcst...");
        try {
            weatherSyncService.syncUltraSrtNcst();
        } catch (org.springframework.web.client.ResourceAccessException e) {
            log.warn("[Scheduler] UltraSrtNcst API Network/Timeout occurred: {}", e.getMessage());
        } catch (Exception e) {
            log.error("[Scheduler] Error in scheduleUltraSrtNcst", e);
        }
    }

    // 3. 초단기예보: 매시 50분에 실행 (14:50, 15:50 ...)
    @Scheduled(cron = "0 50 * * * *", zone = "Asia/Seoul")
    public void scheduleUltraSrtFcst() {
        log.info("[Scheduler] Starting scheduled syncUltraSrtFcst...");
        try {
            weatherSyncService.syncUltraSrtFcst();
        } catch (org.springframework.web.client.ResourceAccessException e) {
            log.warn("[Scheduler] UltraSrtFcst API Network/Timeout occurred: {}", e.getMessage());
        } catch (Exception e) {
            log.error("[Scheduler] Error in scheduleUltraSrtFcst", e);
        }
    }

    // 4. 미세먼지 실황: 매시 20분에 실행 (14:20, 15:20 ...)
    @Scheduled(cron = "0 20 * * * *", zone = "Asia/Seoul")
    public void scheduleRealtimeFineDust() {
        log.info("[Scheduler] Starting scheduled syncRealtimeFineDust...");
        try {
            fineDustSyncService.syncRealtimeFineDust();
        } catch (org.springframework.web.client.ResourceAccessException e) {
            log.warn("[Scheduler] FineDust API Network/Timeout occurred: {}", e.getMessage());
        } catch (Exception e) {
            log.error("[Scheduler] Error in scheduleRealtimeFineDust", e);
        }
    }

    // 5. 자외선지수: 매일 06시 10분, 18시 10분에 실행 (하루 2회)
    @Scheduled(cron = "0 10 6,18 * * *", zone = "Asia/Seoul")
    public void scheduleUvIndex() {
        log.info("[Scheduler] Starting scheduled syncUvIndex...");
        try {
            uvSyncService.syncUvIndex();
        } catch (org.springframework.web.client.ResourceAccessException e) {
            log.warn("[Scheduler] UV API Network/Timeout occurred: {}", e.getMessage());
        } catch (Exception e) {
            log.error("[Scheduler] Error in scheduleUvIndex", e);
        }
    }
}
