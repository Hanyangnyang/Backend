package life.hanyang.core.menu.entity;

import life.hanyang.core.global.exception.BusinessException;
import life.hanyang.core.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum CafeteriaCode {
    // 기존 클라이언트 API 호환을 위해 RE 코드는 유지하고, 개편된 HY-SQUARE 사이트의 시설 ID를 별도로 매핑한다.
    RE11("re11", "교직원식당", 2),
    RE12("re12", "학생식당", 1),
    RE13("re13", "기숙사식당", 4),
    RE15("re15", "창업보육센터", 3);
    private final String code;
    private final String defaultName;
    private final int facilityId;
    // 크롤링 수신 텍스트나 요청 파라미터(String)를 Enum으로 안전하게 변환
    public static CafeteriaCode fromCode(String code) {
        return Arrays.stream(values())
                .filter(c -> c.code.equalsIgnoreCase(code))
                .findFirst()
                .orElseThrow(() -> new BusinessException("존재하지 않는 식당 코드입니다: " + code, ErrorCode.INVALID_INPUT_VALUE));
    }
}
