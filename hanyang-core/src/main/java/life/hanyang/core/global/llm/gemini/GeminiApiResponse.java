package life.hanyang.core.global.llm.gemini;

import java.util.List;

public record GeminiApiResponse(
        List<Candidate> candidates
) {
    public record Candidate(Content content) {}

    public record Content(List<Part> parts) {}

    public record Part(String text) {}

    public String getGeneratedText() {
        if (candidates != null && !candidates.isEmpty()) {
            Candidate candidate = candidates.get(0);
            if (candidate.content() != null && candidate.content().parts() != null && !candidate.content().parts().isEmpty()) {
                return candidate.content().parts().get(0).text().trim();
            }
        }
        throw new IllegalStateException("Gemini API 응답에서 텍스트를 추출할 수 없습니다.");
    }
}
