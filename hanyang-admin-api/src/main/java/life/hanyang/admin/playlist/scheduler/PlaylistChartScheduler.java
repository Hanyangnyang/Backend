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
        try {
            playlistService.getChart(ChartType.RISING);
            playlistService.getChart(ChartType.WEEKLY);
            playlistService.getChart(ChartType.MONTHLY);
            log.info("[PlaylistChartScheduler] 서버 기동 차트 Warm-up 완료");
        } catch (Exception e) {
            log.warn("[PlaylistChartScheduler] 차트 Warm-up 중 예외 발생 (DB 초기 상태일 수 있음): {}", e.getMessage());
        }
    }

    /**
     * 🔥 실시간 급상승 차트 스케줄러 (매시 정각 00분)
     */
    @Scheduled(cron = "0 0 * * * *", zone = "Asia/Seoul")
    public void scheduleRisingChart() {
        log.info("[PlaylistChartScheduler] 실시간 급상승 차트 정기 집계 시작");
        try {
            playlistService.calculateAndSaveChart(ChartType.RISING, Instant.now());
            log.info("[PlaylistChartScheduler] 실시간 급상승 차트 정기 집계 완료");
        } catch (Exception e) {
            log.error("[PlaylistChartScheduler] 실시간 급상승 차트 집계 실패: {}", e.getMessage(), e);
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
            log.error("[PlaylistChartScheduler] 주간 차트 집계 실패: {}", e.getMessage(), e);
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
            log.error("[PlaylistChartScheduler] 월간 차트 집계 실패: {}", e.getMessage(), e);
        }
    }
}
