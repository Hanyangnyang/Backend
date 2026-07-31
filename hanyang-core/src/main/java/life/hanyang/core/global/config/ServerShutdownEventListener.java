package life.hanyang.core.global.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Slf4j
@Component
public class ServerShutdownEventListener {

    private final RestClient restClient;
    private final String webhookUrl;

    public ServerShutdownEventListener(
            RestClient.Builder builder,
            @Value("${logging.webhook.url:}") String webhookUrl
    ) {
        this.restClient = builder.build();
        this.webhookUrl = webhookUrl;
    }

    @EventListener
    public void onContextClosed(ContextClosedEvent event) {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            return;
        }

        log.warn("Application context is closing. Sending shutdown webhook alert...");
        Map<String, Object> body = Map.of(
                "content", "🚨 **[서버 다운/셧다운 경고]** 백엔드 애플리케이션 컨텍스트가 종료되었습니다."
        );

        try {
            restClient.post()
                    .uri(webhookUrl)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.error("Failed to send shutdown webhook alert", e);
        }
    }
}
