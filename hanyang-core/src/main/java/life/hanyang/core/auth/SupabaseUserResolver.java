package life.hanyang.core.auth;

import com.fasterxml.jackson.databind.JsonNode;
import life.hanyang.core.global.exception.BusinessException;
import life.hanyang.core.global.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import java.time.Duration;
import java.util.UUID;

/** Supabase Auth 서버에서 검증한 사용자만 댓글의 작성자/조회자로 인정한다. */
@Component
public class SupabaseUserResolver {
    private final RestClient client;
    private final String url;
    private final String apiKey;

    public SupabaseUserResolver(RestClient.Builder builder,
            @Value("${supabase.auth.url:}") String url,
            @Value("${supabase.auth.anon-key:}") String apiKey) {
        this.url = url.replaceAll("/+$", "");
        this.apiKey = apiKey;
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(2));
        factory.setReadTimeout(Duration.ofSeconds(3));
        this.client = builder.clone().requestFactory(factory).build();
    }

    public UUID optional(String authorization) {
        return authorization == null ? null : required(authorization);
    }

    public UUID required(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ") || authorization.substring(7).isBlank()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        if (url.isBlank() || apiKey.isBlank()) {
            throw new BusinessException("댓글 인증 설정이 필요합니다.", ErrorCode.PLAYLIST_AUTH_UNAVAILABLE);
        }
        JsonNode user;
        try {
            user = client.get().uri(url + "/auth/v1/user")
                    .header("apikey", apiKey).header(HttpHeaders.AUTHORIZATION, authorization)
                    .retrieve().body(JsonNode.class);
        } catch (RestClientResponseException e) {
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED || e.getStatusCode() == HttpStatus.FORBIDDEN) {
                throw new BusinessException(ErrorCode.UNAUTHORIZED);
            }
            throw new BusinessException(ErrorCode.PLAYLIST_AUTH_UNAVAILABLE);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.PLAYLIST_AUTH_UNAVAILABLE);
        }
        try {
            return UUID.fromString(user.path("id").asText());
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
    }
}
