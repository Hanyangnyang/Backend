package life.hanyang.core.global.llm.gemini;

import java.util.List;

public record GeminiApiRequest(
        List<Content> contents,
        GenerationConfig generationConfig
) {
    public record Content(List<Part> parts) {}

    public record Part(String text) {}

    public record GenerationConfig(Double temperature, Integer maxOutputTokens) {}

    public static GeminiApiRequest of(String promptText) {
        return new GeminiApiRequest(
                List.of(new Content(List.of(new Part(promptText)))),
                new GenerationConfig(0.8, 1024)
        );
    }
}
