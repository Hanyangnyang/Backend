package life.hanyang.core.menu.util;

import java.util.ArrayList;
import java.util.List;

public class MenuParserUtils {

    // 예외 처리할 타이틀 키워드 목록 (api/menu.js 기준)
    private static final List<String> IGNORE_TITLE_KEYWORDS = List.of(
            "검색", "댓글", "창업보육", "학생식당", "교직원", "창의인재", "푸드코트", "위치", "조회"
    );
    /**
     * 식단 제목(h3)이 유효한지 검증하는 메서드
     */
    public static boolean isValidTitle(String title) {
        if (title == null || title.isBlank() || title.contains("원")) {
            return false;
        }
        return IGNORE_TITLE_KEYWORDS.stream().noneMatch(title::contains);
    }

    public static ParsedMenu cleanUpMenuText(String rawText) {
        if (rawText == null || rawText.isBlank() || rawText.contains("확인 가능합니다")) {
            return new ParsedMenu("", "");
        }
        // 2. [천원의아침밥] 태그 뒤 띄어쓰기 보정
        String text = rawText.replaceAll("(\\[천원의아침밥\\])([^\\s])", "$1 $2");
        // 3. 쌍따옴표 제거 후 공백(\\s+) 단위로 나눔
        String[] tokens = text.replace("\"", "").split("\\s+");
        List<String> items = new ArrayList<>();
        String extractedPrice = "";
        for (String token : tokens) {
            String trimmed = token.trim();
            // 단독으로 남은 '&' 나 빈 문자열 제거
            if (trimmed.isEmpty() || trimmed.equals("&")) {
                continue;
            }
            // 영문(알파벳)이 포함된 불필요한 토큰 제거 (JS/HTML 속성 등)
            if (trimmed.matches(".*[a-zA-Z].*")) {
                continue;
            }
            // 가격 텍스트 (예: "6000원", "5,500원") 추출
            if (trimmed.matches(".*\\d+.*원.*")) {
                extractedPrice = trimmed;
                continue;
            }
            // 유효한 음식 명칭/태그 추가
            items.add(trimmed);
        }
        // 4. 줄바꿈(\n) 단위로 결합된 텍스트 생성 ("김치찌개\n밥\n쌀")
        String cleanMenuText = String.join("\n", items);
        return new ParsedMenu(cleanMenuText, extractedPrice);
    }
    // 결과 전달용 DTO/Record
    public record ParsedMenu(String cleanedMenu, String price) {}
}
