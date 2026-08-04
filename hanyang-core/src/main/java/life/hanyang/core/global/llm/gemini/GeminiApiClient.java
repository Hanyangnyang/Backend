package life.hanyang.core.global.llm.gemini;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class GeminiApiClient {

    private final RestClient restClient;
    private final String apiKey;
    private final String model;

    public GeminiApiClient(
            @Value("${api.gemini.base-url:https://generativelanguage.googleapis.com}") String baseUrl,
            @Value("${api.gemini.key:}") String apiKey,
            @Value("${api.gemini.model:gemini-3.5-flash-lite}") String model
    ) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
        this.apiKey = apiKey;
        this.model = model;
    }

    public String generateContent(String prompt) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Gemini API Key가 설정되지 않았습니다.");
        }

        GeminiApiResponse response = restClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1beta/models/{model}:generateContent")
                        .queryParam("key", apiKey)
                        .build(model))
                .contentType(MediaType.APPLICATION_JSON)
                .body(GeminiApiRequest.of(prompt))
                .retrieve()
                .body(GeminiApiResponse.class);

        if (response == null) {
            throw new IllegalStateException("Gemini API 응답이 null입니다.");
        }

        return response.getGeneratedText();
    }
}
