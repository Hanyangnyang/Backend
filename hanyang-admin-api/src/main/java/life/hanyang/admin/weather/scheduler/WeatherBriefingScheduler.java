package life.hanyang.admin.weather.scheduler;

import life.hanyang.core.weather.service.WeatherBriefingService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class WeatherBriefingScheduler {
    private final WeatherBriefingService weatherBriefingService;

    /**
     * 매시 20분 0초에 실행 (초단기실황 API 수집 완료 직후)
     */
    @Scheduled(cron = "0 20 * * * *", zone = "Asia/Seoul")
    public void scheduleWeatherBriefingGenration() {
        log.info("[WeatherBriefingScheduler] 날씨 LLM 브리핑 정기 생성 시작");
        try {
            weatherBriefingService.generateAndSaveBriefing("ANSAN");
            log.info("[WeatherBriefingScheduler] 날씨 LLM 브리핑 정기 생성 완료");
        } catch (Exception e) {
            log.error("[WeatherBriefingScheduler] 날씨 LLM 브리핑 생성 중 오류 발생: {}", e.getMessage(), e);
        }
    }
}