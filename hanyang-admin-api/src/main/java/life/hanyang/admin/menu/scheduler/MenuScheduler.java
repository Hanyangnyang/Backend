package life.hanyang.admin.menu.scheduler;

import life.hanyang.core.menu.service.MenuScrapingService;
import life.hanyang.core.menu.repository.MenuRepository;
import life.hanyang.core.holiday.service.HolidayService;
import life.hanyang.core.holiday.domain.DayType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
@RequiredArgsConstructor
public class MenuScheduler {

    private final MenuScrapingService menuScrapingService;
    private final HolidayService holidayService;
    private final MenuRepository menuRepository;
    private final AtomicBoolean todayRefreshRunning = new AtomicBoolean();

    // 매일 01:00 (어제 ~ 7일 뒤 식단 자동 스크래핑)
    @Scheduled(cron = "0 0 1 * * *", zone = "Asia/Seoul")
    public void scheduleAutoMenuScrape() {
        log.info("[MenuScheduler] 식단 자동 스크래핑 시작 (어제 ~ 7일 뒤)");
        try {
            LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
            LocalDate start = today.minusDays(1);
            LocalDate end = today.plusDays(7);
            menuScrapingService.scrapeCafeterias(null, start, end)
                    .thenRun(() -> checkMissingMenus(today))
                    .exceptionally(error -> {
                        log.error("[MenuScheduler] 식단 수집 후 누락 검사 실패", error);
                        return null;
                    });
            log.info("[MenuScheduler] 식단 자동 스크래핑 요청 완료");
        } catch (Exception e) {
            log.error("[MenuScheduler] 식단 자동 스크래핑 중 오류 발생: {}", e.getMessage(), e);
        }
    }

    // 매일 06:45~18:45, 30분 간격. 각 메서드에 단일 @Scheduled를 두어 활성 배포 색상 검사도 적용한다.
    @Scheduled(cron = "0 45 6-18 * * *", zone = "Asia/Seoul")
    public void scheduleTodayMenuRefreshAtFortyFive() {
        refreshTodayMenus();
    }

    @Scheduled(cron = "0 15 7-18 * * *", zone = "Asia/Seoul")
    public void scheduleTodayMenuRefreshAtFifteen() {
        refreshTodayMenus();
    }

    private void refreshTodayMenus() {
        if (!todayRefreshRunning.compareAndSet(false, true)) {
            log.warn("[MenuScheduler] 이전 당일 식단 갱신이 진행 중이므로 이번 실행을 건너뜁니다.");
            return;
        }
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        log.info("[MenuScheduler] 당일 식단 갱신 시작: date={}", today);
        try {
            menuScrapingService.scrapeTodayMenus(today).whenComplete((ignored, error) -> {
                todayRefreshRunning.set(false);
                if (error != null) {
                    log.error("[MenuScheduler] 당일 식단 갱신 작업 실패: date={}", today, error);
                } else {
                    log.info("[MenuScheduler] 당일 식단 갱신 작업 종료: date={}", today);
                }
            });
        } catch (Exception error) {
            todayRefreshRunning.set(false);
            log.error("[MenuScheduler] 당일 식단 갱신 요청 실패: date={}", today, error);
        }
    }

    void checkMissingMenus(LocalDate today) {
        // 미래 식단 미등록으로 인한 불필요한 알림을 줄이기 위해 오늘과 내일만 검사한다.
        List<LocalDate> missingDates = List.of(today, today.plusDays(1)).stream()
                .filter(date -> holidayService.getDateInfo(date).dayType() == DayType.WEEKDAY)
                .filter(date -> !menuRepository.existsByDate(date))
                .toList();
        if (!missingDates.isEmpty()) {
            // 운영 환경의 기존 ERROR 로그 웹훅을 통해 날짜별 알림을 한 건으로 전송한다.
            log.error("[학식 누락] 평일인데 전체 식당의 메뉴가 없습니다. dates={}", missingDates);
        }
    }
}
