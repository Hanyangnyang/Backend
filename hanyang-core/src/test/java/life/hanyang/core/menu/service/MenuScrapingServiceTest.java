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

class MenuScrapingServiceTest {

    private final MenuScrapingService service = new MenuScrapingService(null, null, null);

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
        assertEquals("[천원의아침밥] 꼬치어묵탕\n쌀밥\n도시락김\n배추김치", menus.get(0).displayMenu());
        assertEquals(1000, menus.get(0).price());
        assertEquals(MealType.LUNCH, menus.get(1).mealType());
        assertEquals("매운삼겹살볶음밥\n꼬들단무지\n일식장국", menus.get(1).displayMenu());
        assertEquals(4500, menus.get(1).price());
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
