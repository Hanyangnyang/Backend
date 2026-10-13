package life.hanyang.core.menu.service;

import life.hanyang.core.menu.dto.MenuCrawlResultDto;
import life.hanyang.core.menu.entity.MealType;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MenuScrapingServiceTest {

    private final MenuScrapingService service = new MenuScrapingService(null, null, null);

    @Test
    @DisplayName("현재 세로형 메뉴 카드에서 메뉴와 가격을 읽는다")
    void parseVerticalMenuCards() {
        Document document = Jsoup.parse("""
                <div class="menu-group">
                  <div class="menu-group__title"><span>중식</span></div>
                  <div class="menu-grid-2col">
                    <div class="menu-card-v"><div class="menu-card-v__body">
                      <div class="menu-card-v__name">참치와사비마요덮밥 (Rice and Tuna Wasabi Mayonnaise Sauce)</div>
                      <div class="menu-card-v__desc">연근유자청절임/배추김치/일식장국</div>
                      <div class="menu-card-v__bottom"><span class="menu-card-v__price">4,500원</span></div>
                    </div></div>
                  </div>
                </div>
                """);

        var menus = service.parseMenus(document);

        assertEquals(1, menus.size());
        assertEquals(MealType.LUNCH, menus.get(0).mealType());
        assertEquals("참치와사비마요덮밥\n연근유자청절임\n배추김치\n일식장국", menus.get(0).displayMenu());
        assertEquals(4500, menus.get(0).price());
    }

    @Test
    @DisplayName("알 수 없는 페이지나 메뉴 구조는 빈 식단으로 처리하지 않는다")
    void rejectUnrecognizedMenuStructure() {
        for (String html : List.of(
                "<html><body>점검 중</body></html>",
                "<div class='menu-group'><div class='menu-group__title'>중식</div><div class='unknown-card'>메뉴</div></div>",
                "<div class='menu-group'><div class='menu-item'><div class='menu-item__name'>메뉴</div></div></div>",
                "<div class='menu-group'><div class='menu-group__title'>중식</div><div class='menu-card-v'></div></div>",
                "<div class='menu-group'><div class='menu-group__title'>중식</div><div class='menu-card-v'><div class='menu-card-v__name'>메뉴</div></div><div class='menu-card-v'></div></div>")) {
            assertThrows(IllegalStateException.class, () -> service.parseMenus(Jsoup.parse(html)));
        }
    }

    @Test
    @DisplayName("일간 식단 영역의 명시적인 미등록 응답은 빈 식단으로 처리한다")
    void acceptExplicitlyEmptyDailyMenu() {
        Document document = Jsoup.parse("""
                <div id="view-pane-daily">
                  <div class="fac-empty-msg">선택하신 일자(2026-10-18)에 등록된 식단이 없습니다.</div>
                </div>
                """);
        assertEquals(List.of(), service.parseMenus(document));
    }

    @Test
    @DisplayName("새 HY-SQUARE 메뉴 카드를 식사별로 파싱한다")
    void parseNewMenuCards() {
        Document document = Jsoup.parse("""
                <div class="menu-group">
                  <div class="menu-group__title">조식</div>
                  <div class="menu-item">
                    <div class="menu-item__name">[천원의아침밥] 꼬치어묵탕 Fish Cake Skewers and Broth</div>
                    <div class="menu-item__desc">쌀밥<br>도시락김<br>배추김치</div>
                    <span class="menu-item__price">1,000원</span>
                  </div>
                </div>
                <div class="menu-group">
                  <div class="menu-group__title">중식</div>
                  <div class="menu-item">
                    <div class="menu-item__name">매운삼겹살볶음밥 Spicy Fried Rice with Pork Belly</div>
                    <div class="menu-item__desc">꼬들단무지<br>일식장국</div>
                    <span class="menu-item__price">4,500원</span>
                  </div>
                </div>
                """);

        List<MenuCrawlResultDto.MenuDetailDto> menus = service.parseMenus(document);

        assertEquals(2, menus.size());
        assertEquals(MealType.BREAKFAST, menus.get(0).mealType());
        assertEquals("[천원의아침밥]\n꼬치어묵탕\n쌀밥\n도시락김\n배추김치", menus.get(0).displayMenu());
        assertEquals(1000, menus.get(0).price());
        assertEquals(MealType.LUNCH, menus.get(1).mealType());
        assertEquals("매운삼겹살볶음밥\n꼬들단무지\n일식장국", menus.get(1).displayMenu());
        assertEquals(4500, menus.get(1).price());
    }

    @Test
    @DisplayName("설명의 슬래시와 줄바꿈을 분리하고 대표 메뉴명의 슬래시는 유지한다")
    void parseSlashSeparatedDescriptions() {
        Document document = Jsoup.parse("""
                <div class="menu-group">
                  <div class="menu-group__title">조식</div>
                  <div class="menu-item">
                    <div class="menu-item__name">[천원의 아침밥] 토스트/데니쉬빵 (Brunch Set)</div>
                    <div class="menu-item__desc"> / <span>양배추샐러드&amp;드레싱</span> / 흰우유//<br/>바나나<br>양념밥 / </div>
                    <span class="menu-item__price">1,000원</span>
                  </div>
                </div>
                """);

        List<MenuCrawlResultDto.MenuDetailDto> menus = service.parseMenus(document);

        assertEquals(1, menus.size());
        assertEquals("[천원의 아침밥]\n토스트/데니쉬빵\n양배추샐러드&드레싱\n흰우유\n바나나\n양념밥",
                menus.get(0).displayMenu());
        assertEquals(1000, menus.get(0).price());
    }

    @Test
    @DisplayName("시설 정보 표에서 식사별 운영시간을 파싱한다")
    void parseOperatingHours() {
        Document document = Jsoup.parse("""
                <table class="info-table"><tr>
                  <td class="info-label">영업시간</td>
                  <td class="info-value">조식 08:30~09:40 (학기중)<br>중식 11:30 ~ 13:30</td>
                </tr></table>
                """);

        Map<String, String> hours = service.parseOperatingHours(document);

        assertEquals(Map.of("조식", "08:30~09:40", "중식", "11:30~13:30"), hours);
    }
}
