package life.hanyang.admin.menu.scheduler;

import life.hanyang.core.menu.service.MenuScrapingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class MenuScheduler {

    private final MenuScrapingService menuScrapingService;

    // 매일 01:00 (어제 ~ 7일 뒤 식단 자동 스크래핑)
    @Scheduled(cron = "0 0 1 * * *", zone = "Asia/Seoul")
    public void scheduleAutoMenuScrape() {
        log.info("[MenuScheduler] 식단 자동 스크래핑 시작 (어제 ~ 7일 뒤)");
        try {
            LocalDate start = LocalDate.now().minusDays(1);
            LocalDate end = LocalDate.now().plusDays(7);
            menuScrapingService.scrapeCafeterias(null, start, end);
            log.info("[MenuScheduler] 식단 자동 스크래핑 요청 완료");
        } catch (Exception e) {
            log.error("[MenuScheduler] 식단 자동 스크래핑 중 오류 발생: {}", e.getMessage(), e);
        }
    }
}
