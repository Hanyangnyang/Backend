package life.hanyang.core.global.llm.jev;

import java.util.Map;

public record JevApiResponse(Map<String, Answer> answers) {
    public double registrationAllowedProbability() {
        Answer answer = answers == null ? null : answers.get(JevApiRequest.QUESTION_ID);
        if (answer == null || !"noul".equals(answer.type()) || answer.noul() == null
                || !Double.isFinite(answer.noul()) || answer.noul() < 0 || answer.noul() > 1) {
            throw new IllegalStateException("Jev 등록 허용 확률 응답이 유효하지 않습니다.");
        }
        return answer.noul();
    }

    public record Answer(String type, Double noul) {}
}
