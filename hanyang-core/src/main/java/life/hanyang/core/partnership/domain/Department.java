package life.hanyang.core.partnership.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Department {
    HANYANGNYANG("하냥냥"), //하냥냥 자체 제휴
    COUNCIL("학생회"), //학생회
    LIONS("LIONS\\n칼리지"), //LIONS 컬리지
    COMMUNICATION("커뮤니케이션&컬쳐대학"), //커컬대
    ENGINEERING("공학대학"), //공학대
    MEDICINE("약학대학"), //약학대
    DESIGN("디자인대학"), //디자인대
    GLOBAL("글로벌문화통상대학"), //글로벌통상대
    BUSINESS("경상대학"), //경상대
    SW("소프트웨어융합대학"), //소융대
    ART("예체능대학"), //예체능대
    ADVANCEDCONVERGENCE("첨단융합대학"); //첨융대

    private final String koreanName;

    Department(String koreanName) {
        this.koreanName = koreanName;
    }

    @JsonCreator
    public static Department fromKoreanName(String value) {
        if (value == null) {
            return null;
        }
        for (Department dept : Department.values()) {
            if (dept.koreanName.equals(value) || dept.name().equalsIgnoreCase(value)) {
                return dept;
            }
        }
        return null;
    }

    @JsonValue
    public String getKoreanName() {
        return koreanName;
    }
}
