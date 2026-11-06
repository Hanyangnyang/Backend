package life.hanyang.core.menu.service;

import life.hanyang.core.global.util.TransactionCacheEvictor;
import life.hanyang.core.menu.dto.MenuCrawlResultDto;
import life.hanyang.core.menu.entity.CafeteriaCode;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MenuTodayRefreshTest {
    private final MenuSaveService saveService = mock(MenuSaveService.class);
    private final TransactionCacheEvictor evictor = mock(TransactionCacheEvictor.class);
    private final MenuScrapingService service = new MenuScrapingService(Runnable::run, saveService, evictor);
    private final LocalDate today = LocalDate.of(2026, 11, 6);
    private final Document valid = Jsoup.parse("""
            <div class="menu-group"><div class="menu-group__title">중식</div>
              <div class="menu-item"><div class="menu-item__name">바뀐 메뉴</div>
                <div class="menu-item__desc">쌀밥</div><span class="menu-item__price">5000원</span>
              </div>
            </div>
            """);
    private final Document empty = Jsoup.parse("""
            <div id="view-pane-daily"><div class="fac-empty-msg">등록된 식단이 없습니다.</div></div>
            """);

    private Connection connection() {
        var connection = mock(Connection.class);
        when(connection.userAgent(anyString())).thenReturn(connection);
        when(connection.timeout(anyInt())).thenReturn(connection);
        return connection;
    }

    @Test
    void refreshFetchesEveryCafeteriaForExactlyTodayAndEvictsCacheAfterSaving() throws Exception {
        var connection = connection();
        when(connection.get()).thenReturn(valid);
        try (var jsoup = mockStatic(Jsoup.class, CALLS_REAL_METHODS)) {
            jsoup.when(() -> Jsoup.connect(anyString())).thenReturn(connection);
            service.scrapeTodayMenus(today).join();
            for (var code : CafeteriaCode.values()) {
                jsoup.verify(() -> Jsoup.connect("https://life.hanyang.ac.kr/theme/pages/facilities/detail.php?id="
                        + code.getFacilityId() + "&date=" + today));
            }
            var saved = ArgumentCaptor.forClass(MenuCrawlResultDto.class);
            verify(saveService, times(CafeteriaCode.values().length)).saveCafeteriaAndMenus(saved.capture());
            assertEquals(List.of(CafeteriaCode.values()), saved.getAllValues().stream().map(MenuCrawlResultDto::code).toList());
            assertTrue(saved.getAllValues().stream().allMatch(dto -> dto.date().equals(today)));
            assertTrue(saved.getAllValues().stream().allMatch(dto -> dto.menus().get(0).displayMenu().contains("바뀐 메뉴")));
            var order = inOrder(saveService, evictor);
            order.verify(saveService, times(CafeteriaCode.values().length)).saveCafeteriaAndMenus(any());
            order.verify(evictor).evictCacheAfterCommit("menu");
        }
    }

    @Test
    void emptyTodayResponsesNeverReachSaveOrDeletePath() throws Exception {
        var connection = connection();
        when(connection.get()).thenReturn(empty);
        try (var jsoup = mockStatic(Jsoup.class, CALLS_REAL_METHODS)) {
            jsoup.when(() -> Jsoup.connect(anyString())).thenReturn(connection);
            service.scrapeTodayMenus(today).join();
            verifyNoInteractions(saveService);
        }
    }

    @Test
    void failedAndInvalidPagesDoNotOverwriteMenusAndOtherCafeteriasStillRefresh() throws Exception {
        var invalid = Jsoup.parse("<html>점검 중</html>");
        var connection = connection();
        when(connection.get()).thenThrow(new IOException("일시적 연결 실패")).thenReturn(invalid, empty, valid);
        try (var jsoup = mockStatic(Jsoup.class, CALLS_REAL_METHODS)) {
            jsoup.when(() -> Jsoup.connect(anyString())).thenReturn(connection);
            service.scrapeTodayMenus(today).join();
            var saved = ArgumentCaptor.forClass(MenuCrawlResultDto.class);
            verify(saveService).saveCafeteriaAndMenus(saved.capture());
            assertEquals(CafeteriaCode.values()[3], saved.getValue().code());
            verify(evictor).evictCacheAfterCommit("menu");
        }
    }

    @Test
    void originalNightlyAndManualScrapingKeepExplicitEmptyResponsePolicy() throws Exception {
        var connection = connection();
        when(connection.get()).thenReturn(empty);
        try (var jsoup = mockStatic(Jsoup.class, CALLS_REAL_METHODS)) {
            jsoup.when(() -> Jsoup.connect(anyString())).thenReturn(connection);
            service.scrapeCafeterias(List.of(CafeteriaCode.RE12), today, today).join();
            var saved = ArgumentCaptor.forClass(MenuCrawlResultDto.class);
            verify(saveService).saveCafeteriaAndMenus(saved.capture());
            assertTrue(saved.getValue().menus().isEmpty());
        }
    }
}
