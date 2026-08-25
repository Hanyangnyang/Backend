package life.hanyang.admin.holiday.scheduler;

import life.hanyang.core.holiday.service.HolidayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class HolidayScheduler {

    private final HolidayService holidayService;

    /**
     * 매주 월요일 새벽 4시에 당해 연도 및 다음 연도 공휴일 데이터를 공공데이터포털에서 동기화
     */
    @Scheduled(cron = "0 0 4 ? * MON")
    public void syncHolidaysWeekly() {
        int currentYear = LocalDate.now().getYear();
        log.info("[HolidayScheduler] 공휴일 정기 동기화 시작 (당해: {}, 익년: {})", currentYear, currentYear + 1);
        try {
            holidayService.syncHolidays(currentYear);
            holidayService.syncHolidays(currentYear + 1);
            log.info("[HolidayScheduler] 공휴일 정기 동기화 완료");
        } catch (Exception e) {
            log.error("[HolidayScheduler] 공휴일 정기 동기화 실패: {}", e.getMessage(), e);
        }
    }
}
