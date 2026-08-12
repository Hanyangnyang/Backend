package life.hanyang.admin.library.scheduler;

import life.hanyang.core.library.service.LibraryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LibraryScheduler {

    private final LibraryService libraryService;

    /**
     * 서버 부팅 완료 직후 1회 즉시 실행 (캐시 웜업)
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("[Scheduler] 서버 부팅 완료 - 도서관 좌석 캐시 최초 웜업(Warm-up) 실행");
        scheduleReadingRoomSeatsRefresh();
    }

    /**
     * 24시간 내내 3분마다 도서관 좌석 캐시 주기적 갱신
     */
    @Scheduled(cron = "0 */3 * * * *", zone = "Asia/Seoul")
    public void scheduleReadingRoomSeatsRefresh() {
        log.info("[Scheduler] 도서관 열람실 잔여 좌석 캐시 주기적 갱신 시작...");
        try {
            libraryService.refreshReadingRoomSeats();
        } catch (Exception e) {
            log.error("[Scheduler] 도서관 좌석 캐시 갱신 스케줄러 에러: ", e);
        }
    }
}
