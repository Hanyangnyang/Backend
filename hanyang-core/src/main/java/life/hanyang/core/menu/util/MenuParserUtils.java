package life.hanyang.core.menu.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class MenuParserUtils {

    // 예외 처리할 타이틀 키워드 목록 (api/menu.js 기준)
    private static final List<String> IGNORE_TITLE_KEYWORDS = List.of(
            "검색", "댓글", "창업보육", "학생식당", "교직원", "창의인재", "푸드코트", "위치", "조회"
    );

    // 숫자 또는 특수기호가 섞이고 "원"으로 끝나는 가격 패턴 (예: 6,000원, 4,500원)
    private static final Pattern PRICE_TITLE_PATTERN = Pattern.compile(".*[0-9,.~\\s]+원$");

    /**
     * 식단 제목(h3)이 유효한지 검증하는 메서드
     */
    public static boolean isValidTitle(String title) {
        if (title == null || title.isBlank() || PRICE_TITLE_PATTERN.matcher(title.trim()).matches()) {
            return false;
        }
        return IGNORE_TITLE_KEYWORDS.stream().noneMatch(title::contains);
    }

    /**
     * rawText를 파싱하여 개별 식단 세트(메뉴 내용, 가격) 리스트로 반환 (api/menu.js 구현과 동등)
     */
    public static List<ParsedMenu> parseMenuSets(String rawText) {
        List<ParsedMenu> result = new ArrayList<>();
        if (rawText == null || rawText.isBlank() || rawText.length() <= 5 || rawText.contains("확인 가능합니다")) {
            return result;
        }

        // 1. [천원의아침밥] 뒤에 띄어쓰기 없으면 강제로 공백 추가
        String text = rawText.replaceAll("(\\[천원의아침밥\\])([^\\s])", "$1 $2");

        // 2. 쌍따옴표 제거 후 공백 단위 분할 및 필터링
        String[] tokens = text.replace("\"", "").split("\\s+");
        List<String> rawItems = new ArrayList<>();
        for (String token : tokens) {
            String trimmed = token.trim();
            if (trimmed.isEmpty() || trimmed.equals("&")) {
                continue;
            }
            // 순수 영문(영문 번역 문구) 단어만 필터링하고, 한글+영문 조합("BBQ치킨", "A코너" 등)은 삭제하지 않고 보존
            if (trimmed.matches("^[a-zA-Z\\-]+$")) {
                continue;
            }
            rawItems.add(trimmed);
        }

        // 3. 가격(숫자+원) 기준으로 세트 분할
        List<SetItem> sets = new ArrayList<>();
        List<String> currentItems = new ArrayList<>();

        for (String item : rawItems) {
            if (item.matches(".*\\d+.*원.*")) {
                sets.add(new SetItem(new ArrayList<>(currentItems), item));
                currentItems.clear();
            } else {
                currentItems.add(item);
            }
        }
        if (!currentItems.isEmpty()) {
            sets.add(new SetItem(currentItems, ""));
        }

        // 4. 각 세트별 포맷팅 (HTML 태그 없이 줄바꿈으로만 구분된 순수 텍스트 생성)
        for (SetItem set : sets) {
            if (!set.items().isEmpty()) {
                String displayMenu = String.join("\n", set.items());
                result.add(new ParsedMenu(displayMenu, set.price()));
            }
        }

        return result;
    }

    public static ParsedMenu cleanUpMenuText(String rawText) {
        List<ParsedMenu> sets = parseMenuSets(rawText);
        if (sets.isEmpty()) {
            return new ParsedMenu("", "");
        }
        return sets.get(0);
    }

    private record SetItem(List<String> items, String price) {}

    // 결과 전달용 DTO/Record
    public record ParsedMenu(String cleanedMenu, String price) {}
}

