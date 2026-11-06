package life.hanyang.admin.menu.scheduler;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import life.hanyang.core.holiday.domain.DayType;
import life.hanyang.core.holiday.dto.DateInfoResponse;
import life.hanyang.core.holiday.service.HolidayService;
import life.hanyang.core.menu.repository.MenuRepository;
import life.hanyang.core.menu.service.MenuScrapingService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.TreeSet;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import java.time.ZoneId;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MenuSchedulerTest {
    private final MenuScrapingService scrapingService = mock(MenuScrapingService.class);
    private final HolidayService holidayService = mock(HolidayService.class);
    private final MenuRepository menuRepository = mock(MenuRepository.class);
    private final MenuScheduler scheduler = new MenuScheduler(scrapingService, holidayService, menuRepository);
    private final Logger logger = (Logger) LoggerFactory.getLogger(MenuScheduler.class);
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();
    private final LocalDate today = LocalDate.of(2026, 10, 14);

    MenuSchedulerTest() {
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void detachAppender() {
        logger.detachAppender(appender);
        appender.stop();
    }

    @Test
    void alertsOnceForBothMissingWeekdays() {
        weekday(today);
        weekday(today.plusDays(1));

        scheduler.checkMissingMenus(today);

        var errors = appender.list.stream().filter(event -> event.getLevel() == Level.ERROR).toList();
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).getFormattedMessage().contains(today.toString()));
        assertTrue(errors.get(0).getFormattedMessage().contains(today.plusDays(1).toString()));
        verify(holidayService).getDateInfo(today);
        verify(holidayService).getDateInfo(today.plusDays(1));
        verifyNoMoreInteractions(holidayService);
    }

    @Test
    void skipsWeekendsHolidaysAndRegisteredClosures() {
        for (DayType dayType : new DayType[]{DayType.WEEKEND, DayType.HOLIDAY, DayType.NO_OPERATION}) {
            doAnswer(invocation -> DateInfoResponse.of(invocation.getArgument(0), dayType, null))
                    .when(holidayService).getDateInfo(any());
            scheduler.checkMissingMenus(today);
        }
        verifyNoInteractions(menuRepository);
        assertTrue(appender.list.isEmpty());
    }

    @Test
    void doesNotAlertWhenMenusExist() {
        weekday(today);
        weekday(today.plusDays(1));
        when(menuRepository.existsByDate(any())).thenReturn(true);

        scheduler.checkMissingMenus(today);

        assertTrue(appender.list.isEmpty());
    }

    @Test
    void alertsOnlyForMissingDate() {
        weekday(today);
        weekday(today.plusDays(1));
        when(menuRepository.existsByDate(today)).thenReturn(true);

        scheduler.checkMissingMenus(today);

        assertEquals(1, appender.list.size());
        assertFalse(appender.list.get(0).getFormattedMessage().contains(today.toString()));
        assertTrue(appender.list.get(0).getFormattedMessage().contains(today.plusDays(1).toString()));
    }

    @Test
    void checksOnlyAfterScrapingCompletes() {
        var completion = new CompletableFuture<Void>();
        when(scrapingService.scrapeCafeterias(isNull(), any(), any())).thenReturn(completion);
        when(holidayService.getDateInfo(any())).thenAnswer(invocation ->
                DateInfoResponse.of(invocation.getArgument(0), DayType.WEEKDAY, null));
        when(menuRepository.existsByDate(any())).thenReturn(true);
        LocalDate before = LocalDate.now(ZoneId.of("Asia/Seoul"));

        scheduler.scheduleAutoMenuScrape();

        verifyNoInteractions(holidayService, menuRepository);
        completion.complete(null);
        LocalDate after = LocalDate.now(ZoneId.of("Asia/Seoul"));
        var dates = org.mockito.ArgumentCaptor.forClass(LocalDate.class);
        verify(menuRepository, times(2)).existsByDate(dates.capture());
        LocalDate checkedToday = dates.getAllValues().get(0);
        assertTrue(checkedToday.equals(before) || checkedToday.equals(after));
        assertEquals(checkedToday.plusDays(1), dates.getAllValues().get(1));
    }

    @Test
    void todayRefreshSchedulesExactlyTwentyFiveRunsFrom0645Through1845() throws Exception {
        var date = LocalDate.of(2026, 11, 7); // 주말도 실행
        var times = new TreeSet<LocalTime>();
        for (String name : new String[]{"scheduleTodayMenuRefreshAtFortyFive", "scheduleTodayMenuRefreshAtFifteen"}) {
            var scheduled = MenuScheduler.class.getMethod(name).getAnnotation(Scheduled.class);
            assertNotNull(scheduled);
            assertEquals("Asia/Seoul", scheduled.zone());
            var expression = CronExpression.parse(scheduled.cron());
            LocalDateTime next = expression.next(date.atStartOfDay().minusSeconds(1));
            while (next != null && next.toLocalDate().equals(date)) {
                assertTrue(times.add(next.toLocalTime()), "중복 실행 시각");
                next = expression.next(next);
            }
        }
        assertEquals(25, times.size());
        var expected = java.util.stream.IntStream.range(0, 25)
                .mapToObj(i -> LocalTime.of(6, 45).plusMinutes(i * 30L)).toList();
        assertEquals(expected, times.stream().toList());
    }

    @Test
    void todayRefreshUsesKoreanTodayAndSkipsBothSchedulesUntilCompletion() {
        var completion = new CompletableFuture<Void>();
        when(scrapingService.scrapeTodayMenus(any())).thenReturn(completion);
        var before = LocalDate.now(ZoneId.of("Asia/Seoul"));
        scheduler.scheduleTodayMenuRefreshAtFortyFive();
        scheduler.scheduleTodayMenuRefreshAtFifteen();
        var after = LocalDate.now(ZoneId.of("Asia/Seoul"));
        var dates = org.mockito.ArgumentCaptor.forClass(LocalDate.class);
        verify(scrapingService).scrapeTodayMenus(dates.capture());
        assertTrue(dates.getValue().equals(before) || dates.getValue().equals(after));
        verifyNoInteractions(holidayService, menuRepository);
        completion.complete(null);
        scheduler.scheduleTodayMenuRefreshAtFifteen();
        verify(scrapingService, times(2)).scrapeTodayMenus(any());
    }

    @Test
    void failedTodayRefreshReleasesOverlapGuard() {
        var completion = new CompletableFuture<Void>();
        when(scrapingService.scrapeTodayMenus(any())).thenReturn(completion, CompletableFuture.completedFuture(null));
        scheduler.scheduleTodayMenuRefreshAtFortyFive();
        completion.completeExceptionally(new IllegalStateException("수집 작업 실패"));
        scheduler.scheduleTodayMenuRefreshAtFifteen();
        verify(scrapingService, times(2)).scrapeTodayMenus(any());
    }

    @Test
    void synchronousTodayRefreshFailureAlsoReleasesOverlapGuard() {
        when(scrapingService.scrapeTodayMenus(any())).thenThrow(new IllegalStateException("실행기 거부"))
                .thenReturn(CompletableFuture.completedFuture(null));
        assertDoesNotThrow(scheduler::scheduleTodayMenuRefreshAtFortyFive);
        scheduler.scheduleTodayMenuRefreshAtFifteen();
        verify(scrapingService, times(2)).scrapeTodayMenus(any());
    }

    private void weekday(LocalDate date) {
        when(holidayService.getDateInfo(date)).thenReturn(DateInfoResponse.of(date, DayType.WEEKDAY, null));
    }
}
