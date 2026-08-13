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
        log.info("[WeatherScheduler] 기상청 단기예보 동기화 시작 (3시간 주기 16분)");
        try {
            weatherSyncService.syncVillageFcst();
            log.info("[WeatherScheduler] 기상청 단기예보 동기화 완료");
        } catch (Exception e) {
            log.error("[WeatherScheduler] 기상청 단기예보 동기화 중 오류 발생: {}", e.getMessage(), e);
        }
    }

    // 2. 초단기실황: 매시 18분에 실행 (14:18, 15:18 ...)
    @Scheduled(cron = "0 18 * * * *", zone = "Asia/Seoul")
    public void scheduleUltraSrtNcst() {
        log.info("[WeatherScheduler] 기상청 초단기실황 동기화 시작 (매시 18분)");
        try {
            weatherSyncService.syncUltraSrtNcst();
            log.info("[WeatherScheduler] 기상청 초단기실황 동기화 완료");
        } catch (Exception e) {
            log.error("[WeatherScheduler] 기상청 초단기실황 동기화 중 오류 발생: {}", e.getMessage(), e);
        }
    }

    // 3. 초단기예보: 매시 50분에 실행 (14:50, 15:50 ...)
    @Scheduled(cron = "0 50 * * * *", zone = "Asia/Seoul")
    public void scheduleUltraSrtFcst() {
        log.info("[WeatherScheduler] 기상청 초단기예보 동기화 시작 (매시 50분)");
        try {
            weatherSyncService.syncUltraSrtFcst();
            log.info("[WeatherScheduler] 기상청 초단기예보 동기화 완료");
        } catch (Exception e) {
            log.error("[WeatherScheduler] 기상청 초단기예보 동기화 중 오류 발생: {}", e.getMessage(), e);
        }
    }

    // 4. 미세먼지 실황: 매시 20분에 실행 (14:20, 15:20 ...)
    @Scheduled(cron = "0 20 * * * *", zone = "Asia/Seoul")
    public void scheduleRealtimeFineDust() {
        log.info("[WeatherScheduler] 미세먼지 실황 동기화 시작 (매시 20분)");
        try {
            fineDustSyncService.syncRealtimeFineDust();
            log.info("[WeatherScheduler] 미세먼지 실황 동기화 완료");
        } catch (Exception e) {
            log.error("[WeatherScheduler] 미세먼지 실황 동기화 중 오류 발생: {}", e.getMessage(), e);
        }
    }

    // 5. 자외선지수: 매일 06시 10분, 18시 10분에 실행 (하루 2회)
    @Scheduled(cron = "0 10 6,18 * * *", zone = "Asia/Seoul")
    public void scheduleUvIndex() {
        log.info("[WeatherScheduler] 자외선 지수 동기화 시작 (매일 06시/18시 10분)");
        try {
            uvSyncService.syncUvIndex();
            log.info("[WeatherScheduler] 자외선 지수 동기화 완료");
        } catch (Exception e) {
            log.error("[WeatherScheduler] 자외선 지수 동기화 중 오류 발생: {}", e.getMessage(), e);
        }
    }
}
