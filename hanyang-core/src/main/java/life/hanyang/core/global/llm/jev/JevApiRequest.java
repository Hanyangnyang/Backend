package life.hanyang.core.global.llm.jev;

import java.util.Map;

public record JevApiRequest(String model, String state, Map<String, Question> questions) {
    public static final String QUESTION_ID = "registration_allowed";

    public static JevApiRequest of(String model, String state, String instructions) {
        return new JevApiRequest(model, state, Map.of(QUESTION_ID,
                new Question("noul", instructions, Map.of(
                        "true", "모든 검열 기준을 통과하여 등록해도 적절하다.",
                        "false", "유해 표현 또는 검열 기준 위반이 있어 등록하기 부적절하다."))));
    }

    public record Question(String type, String instructions, Map<String, String> criteria) {}
}
