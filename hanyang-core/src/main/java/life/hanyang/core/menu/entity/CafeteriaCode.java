package life.hanyang.core.menu.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum CafeteriaCode {
    RE11("re11", "교직원식당"),
    RE12("re12", "학생식당"),
    RE13("re13", "기숙사식당"),
    RE15("re15", "창업보육센터");
    private final String code;
    private final String defaultName;
    // 크롤링 수신 텍스트나 요청 파라미터(String)를 Enum으로 안전하게 변환
    public static CafeteriaCode fromCode(String code) {
        return Arrays.stream(values())
                .filter(c -> c.code.equalsIgnoreCase(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 식당 코드입니다: " + code));
    }
}