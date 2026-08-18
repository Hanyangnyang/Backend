package life.hanyang.core.global.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationFailedEvent;
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
            @Value("${DISCORD_WEBHOOK_URL:${logging.webhook.url:}}") String webhookUrl
    ) {
        this.restClient = builder.build();
        this.webhookUrl = webhookUrl;
    }

    /**
     * 1. 정상 종료 (배포 시 docker compose down 등) 감지
     */
    @EventListener
    public void onContextClosed(ContextClosedEvent event) {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            return;
        }

        log.info("애플리케이션 컨텍스트가 정상 종료됩니다. 셧다운 웹훅 알림을 전송합니다...");
        Map<String, Object> body = Map.of(
                "content", "ℹ️ **[서버 안전 종료]** 백엔드 애플리케이션이 안전하게 종료(Graceful Shutdown)되었습니다."
        );

        sendWebhook(body);
    }

    /**
     * 2. 서버 기동 실패 (DB 연결 오류, Bean 생성 실패 등) 감지
     */
    @EventListener
    public void onApplicationFailed(ApplicationFailedEvent event) {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            return;
        }

        Throwable rootCause = event.getException();
        while (rootCause.getCause() != null && rootCause.getCause() != rootCause) {
            rootCause = rootCause.getCause();
        }

        String errorMessage = rootCause.getMessage() != null ? rootCause.getMessage() : event.getException().toString();
        log.error("애플리케이션 시작 중 치명적인 오류 발생: {}", errorMessage, event.getException());

        Map<String, Object> body = Map.of(
                "content", String.format("🚨 **[서버 기동 실패]** 백엔드 애플리케이션 시작 중 치명적인 오류가 발생했습니다.\n> 📝 **원인**: `%s`", errorMessage)
        );

        sendWebhook(body);
    }

    private void sendWebhook(Map<String, Object> body) {
        try {
            restClient.post()
                    .uri(webhookUrl)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.error("서버 생명주기 웹훅 알림 전송 실패", e);
        }
    }
}
