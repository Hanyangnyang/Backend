package life.hanyang.admin.weather.scheduler;

import life.hanyang.core.weather.service.WeatherBriefingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WeatherBriefingScheduler {
    private final WeatherBriefingService weatherBriefingService;

    /**
     * 매시 22분 0초에 실행 (기상청 및 대기질 데이터 수집 완료 직후)
     */
    @Scheduled(cron = "0 22 * * * *", zone = "Asia/Seoul")
    public void scheduleWeatherBriefingGeneration() {
        log.info("[WeatherBriefingScheduler] 날씨 LLM 브리핑 정기 생성 시작");
        try {
            weatherBriefingService.generateAndSaveBriefing("ANSAN");
            log.info("[WeatherBriefingScheduler] 날씨 LLM 브리핑 정기 생성 완료");
        } catch (Exception e) {
            log.error("[WeatherBriefingScheduler] 날씨 LLM 브리핑 생성 중 오류 발생: {}", e.getMessage(), e);
        }
    }
}