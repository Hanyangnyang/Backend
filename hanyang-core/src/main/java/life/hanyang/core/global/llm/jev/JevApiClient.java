package life.hanyang.core.global.llm.jev;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class JevApiClient {
    private final RestClient restClient;
    private final String apiKey;
    private final String model;

    public JevApiClient(
            @Value("${api.jev.base-url:https://api.typesafe.ai}") String baseUrl,
            @Value("${api.jev.key:}") String apiKey,
            @Value("${api.jev.model:jev-1.13.0}") String model,
            @Value("${api.jev.connect-timeout:1s}") Duration connectTimeout,
            @Value("${api.jev.read-timeout:2s}") Duration readTimeout) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeout);
        factory.setReadTimeout(readTimeout);
        restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
        this.apiKey = apiKey;
        this.model = model;
    }

    public double registrationAllowedProbability(String state, String instructions) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Jev API Key가 설정되지 않았습니다.");
        }
        JevApiResponse response = restClient.post().uri("/v1/systemone")
                .headers(headers -> headers.setBearerAuth(apiKey))
                .contentType(MediaType.APPLICATION_JSON)
                .body(JevApiRequest.of(model, state, instructions))
                .retrieve().body(JevApiResponse.class);
        if (response == null) {
            throw new IllegalStateException("Jev API 응답이 null입니다.");
        }
        return response.registrationAllowedProbability();
    }
}
