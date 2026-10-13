package life.hanyang.core.playlist.service;

import com.fasterxml.jackson.databind.JsonNode;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

/** 기존 JJWT와 Spring HTTP 클라이언트로 FCM HTTP v1을 호출한다. */
@Component
public class PlaylistFcmSender {
    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    private final RestClient client;
    private final String projectId;
    private final String email;
    private final String pem;
    private String accessToken;
    private Instant expiresAt = Instant.EPOCH;

    public PlaylistFcmSender(RestClient.Builder builder,
            @Value("${playlist.notification.firebase.project-id:}") String projectId,
            @Value("${playlist.notification.firebase.client-email:}") String email,
            @Value("${playlist.notification.firebase.private-key:}") String pem) {
        this.projectId = projectId;
        this.email = email;
        this.pem = pem;
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(2));
        factory.setReadTimeout(Duration.ofSeconds(3));
        client = builder.clone().requestFactory(factory).build();
    }

    public boolean configured() {
        return !projectId.isBlank() && !email.isBlank() && !pem.isBlank();
    }

    public void send(UUID notificationId, String token, String platform, String type, UUID songId, String trackId) {
        String body = type.equals("COMMENT") ? "내 추천글에 새로운 댓글이 달렸어요." : "내 추천글에 첫 감정표현이 달렸어요.";
        String link = "https://hanyang.life/?tab=misc&box=playlist&trackId="
                + java.net.URLEncoder.encode(trackId, java.nio.charset.StandardCharsets.UTF_8) + "&postId=" + songId;
        Map<String, Object> message = new HashMap<>();
        message.put("token", token);
        message.put("data", Map.of("title", "하냥냥 플레이리스트", "body", body, "link", link,
                "notificationId", notificationId.toString()));
        if (!"web".equals(platform)) {
            message.put("notification", Map.of("title", "하냥냥 플레이리스트", "body", body));
            message.put("android", Map.of("priority", "high", "notification", Map.of("sound", "default")));
            message.put("apns", Map.of("headers", Map.of("apns-push-type", "alert", "apns-priority", "10"),
                    "payload", Map.of("aps", Map.of("sound", "default"))));
        }
        client.post().uri("https://fcm.googleapis.com/v1/projects/" + projectId + "/messages:send")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token())
                .contentType(MediaType.APPLICATION_JSON).body(Map.of("message", message))
                .retrieve().toBodilessEntity();
    }

    private synchronized String token() {
        if (accessToken != null && expiresAt.isAfter(Instant.now().plusSeconds(60))) return accessToken;
        try {
            String encoded = pem.replace("\\n", "\n").replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "").replaceAll("\\s", "");
            PrivateKey key = KeyFactory.getInstance("RSA")
                    .generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(encoded)));
            Instant now = Instant.now();
            String assertion = Jwts.builder().issuer(email).audience().add(TOKEN_URL).and()
                    .claim("scope", "https://www.googleapis.com/auth/firebase.messaging")
                    .issuedAt(Date.from(now)).expiration(Date.from(now.plusSeconds(3600)))
                    .signWith(key, Jwts.SIG.RS256).compact();
            var form = new LinkedMultiValueMap<String, String>();
            form.add("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer");
            form.add("assertion", assertion);
            JsonNode response = client.post().uri(TOKEN_URL).contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form).retrieve().body(JsonNode.class);
            if (response == null || response.path("access_token").asText().isBlank()) {
                throw new IllegalStateException("FCM access token missing");
            }
            accessToken = response.path("access_token").asText();
            expiresAt = now.plusSeconds(response.path("expires_in").asLong(3600));
            return accessToken;
        } catch (Exception e) {
            // OAuth 응답이나 계정 정보가 예외 로그에 노출되지 않게 원본 예외는 전달하지 않는다.
            throw new IllegalStateException("FCM authentication failed");
        }
    }
}
