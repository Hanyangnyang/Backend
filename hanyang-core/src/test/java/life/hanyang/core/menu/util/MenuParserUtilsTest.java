package life.hanyang.core.menu.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MenuParserUtilsTest {

    @Test
    @DisplayName("식단 제목 유효성 검증 테스트")
    void testIsValidTitle() {
        assertTrue(MenuParserUtils.isValidTitle("중식"));
        assertTrue(MenuParserUtils.isValidTitle("석식"));
        assertTrue(MenuParserUtils.isValidTitle("지원식당"));
        assertTrue(MenuParserUtils.isValidTitle("원기회복 탕"));

        assertFalse(MenuParserUtils.isValidTitle("6,000원"));
        assertFalse(MenuParserUtils.isValidTitle("4,500원"));
        assertFalse(MenuParserUtils.isValidTitle("학생식당"));
        assertFalse(MenuParserUtils.isValidTitle("검색"));
    }

    @Test
    @DisplayName("메뉴 텍스트 및 가격 파싱 테스트 (api/menu.js 동등 구현)")
    void testParseMenuSets() {
        String rawText = "연탄직화제육볶음 Charcoal-Grilled Spicy Stir-Fried Pork 살얼음요구르트*1 어묵양파굴소스볶음 쫑상추겉절이 얼갈이된장국 잡곡밥 배추김치 6,000원";
        List<MenuParserUtils.ParsedMenu> sets = MenuParserUtils.parseMenuSets(rawText);

        assertEquals(1, sets.size());
        assertEquals(6000, sets.get(0).price());
        assertTrue(sets.get(0).cleanedMenu().contains("연탄직화제육볶음"));
        assertTrue(sets.get(0).cleanedMenu().contains("살얼음요구르트*1"));
    }

    @Test
    @DisplayName("한영 조합(BBQ치킨, A코너 등) 메뉴 보존 파싱 테스트")
    void testKoreanEnglishMixedMenuParsing() {
        String rawText = "BBQ치킨 A코너 Coke 6,000원";
        List<MenuParserUtils.ParsedMenu> sets = MenuParserUtils.parseMenuSets(rawText);

        assertEquals(1, sets.size());
        assertTrue(sets.get(0).cleanedMenu().contains("BBQ치킨"));
        assertTrue(sets.get(0).cleanedMenu().contains("A코너"));
    }

    @Test
    @DisplayName("천원의아침밥 태그 파싱 테스트")
    void testCheonwonBreakfastParsing() {
        String rawText = "[천원의아침밥] 쌀밥 된장찌개 1,000원";
        List<MenuParserUtils.ParsedMenu> sets = MenuParserUtils.parseMenuSets(rawText);

        assertEquals(1, sets.size());
        assertEquals(1000, sets.get(0).price());
        assertTrue(sets.get(0).cleanedMenu().startsWith("[천원의아침밥]"));
        assertTrue(sets.get(0).cleanedMenu().contains("쌀밥"));
    }

    @Test
    @DisplayName("괄호로 둘러싸인 영문 번역 문구( (Tuna Vegetable) ) 필터링 테스트")
    void testParenthesizedEnglishFiltering() {
        String rawText = "참치생채소비빔밥 (Tuna Vegetable) 청포묵김가루무침 배추김치 우동국 5,500원";
        List<MenuParserUtils.ParsedMenu> sets = MenuParserUtils.parseMenuSets(rawText);

        assertEquals(1, sets.size());
        assertFalse(sets.get(0).cleanedMenu().contains("Tuna"));
        assertFalse(sets.get(0).cleanedMenu().contains("Vegetable"));
        assertTrue(sets.get(0).cleanedMenu().contains("참치생채소비빔밥"));
        assertTrue(sets.get(0).cleanedMenu().contains("청포묵김가루무침"));
    }
}

