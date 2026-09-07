package life.hanyang.admin.playlist.scheduler;

import jakarta.annotation.PostConstruct;
import life.hanyang.core.playlist.domain.ChartType;
import life.hanyang.core.playlist.service.PlaylistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class PlaylistChartScheduler {

    private final PlaylistService playlistService;

    /**
     * 서버 시작 시 캐시 및 스냅샷 Warm-up (비어있는 차트 즉시 계산)
     */
    @PostConstruct
    public void warmupChartsOnStartup() {
        log.info("[PlaylistChartScheduler] 서버 기동 차트 Warm-up 시작...");
        for (ChartType chartType : ChartType.values()) {
            warmupChart(chartType);
        }
        log.info("[PlaylistChartScheduler] 서버 기동 차트 Warm-up 완료");
    }

    /**
     * 🔥 실시간 급상승 차트 스케줄러 (매시 40분)
     */
    @Scheduled(cron = "0 40 * * * *", zone = "Asia/Seoul")
    public void scheduleRisingChart() {
        log.info("[PlaylistChartScheduler] 실시간 급상승 차트 정기 집계 시작");
        try {
            playlistService.calculateAndSaveChart(ChartType.RISING, Instant.now());
            log.info("[PlaylistChartScheduler] 실시간 급상승 차트 정기 집계 완료");
        } catch (Exception e) {
            logFailure("실시간 급상승 차트 집계", e);
        }
    }

    /**
     * 📅 주간 차트 스케줄러 (매주 월요일 00:00 KST)
     */
    @Scheduled(cron = "0 0 0 * * MON", zone = "Asia/Seoul")
    public void scheduleWeeklyChart() {
        log.info("[PlaylistChartScheduler] 주간 차트 정기 집계 시작 (지난주 월~일)");
        try {
            playlistService.calculateAndSaveChart(ChartType.WEEKLY, Instant.now());
            log.info("[PlaylistChartScheduler] 주간 차트 정기 집계 완료");
        } catch (Exception e) {
            logFailure("주간 차트 집계", e);
        }
    }

    /**
     * 🏆 월간 차트 스케줄러 (매월 1일 00:00 KST)
     */
    @Scheduled(cron = "0 0 0 1 * *", zone = "Asia/Seoul")
    public void scheduleMonthlyChart() {
        log.info("[PlaylistChartScheduler] 월간 차트 정기 집계 시작 (지난달 1일~말일)");
        try {
            playlistService.calculateAndSaveChart(ChartType.MONTHLY, Instant.now());
            log.info("[PlaylistChartScheduler] 월간 차트 정기 집계 완료");
        } catch (Exception e) {
            logFailure("월간 차트 집계", e);
        }
    }

    private void warmupChart(ChartType chartType) {
        try {
            playlistService.getChart(chartType);
            log.info("[PlaylistChartScheduler] {} 차트 Warm-up 완료", chartType);
        } catch (Exception e) {
            logFailure(chartType + " 차트 Warm-up", e);
        }
    }

    private void logFailure(String operation, Exception exception) {
        Throwable rootCause = rootCauseOf(exception);
        log.error("[PlaylistChartScheduler] {} 실패 | exception={} | rootCause={} | message={}",
                operation,
                exception.getClass().getSimpleName(),
                rootCause.getClass().getSimpleName(),
                rootCause.getMessage());
        log.debug("[PlaylistChartScheduler] {} 상세 예외", operation, exception);
    }

    private Throwable rootCauseOf(Throwable exception) {
        Throwable rootCause = exception;
        while (rootCause.getCause() != null && rootCause.getCause() != rootCause) {
            rootCause = rootCause.getCause();
        }
        return rootCause;
    }
}
