package life.hanyang.core.feedback.service;

import life.hanyang.core.feedback.domain.Feedback;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Slf4j
@Component
public class FeedbackWebhookNotifier {

    private final RestClient restClient;
    private final String webhookUrl;

    public FeedbackWebhookNotifier(
            RestClient.Builder restClientBuilder,
            @Value("${feedback.webhook.url:}") String webhookUrl
    ) {
        this.restClient = restClientBuilder.build();
        this.webhookUrl = webhookUrl;
    }

    public void notify(Feedback feedback) {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            log.debug("피드백 웹훅 URL이 설정되지 않아 알림을 건너뜁니다. feedbackId={}", feedback.getId());
            return;
        }

        try {
            restClient.post()
                    .uri(webhookUrl)
                    .body(Map.of("content", formatMessage(feedback)))
                    .retrieve()
                    .toBodilessEntity();
            log.info("피드백 웹훅 알림 전송 완료. feedbackId={}", feedback.getId());
        } catch (Exception e) {
            log.error("피드백 웹훅 알림 전송 실패. feedbackId={}, cause={}, message={}",
                    feedback.getId(), e.getClass().getSimpleName(), e.getMessage());
        }
    }

    private String formatMessage(Feedback feedback) {
        return "새로운 피드백이 등록되었습니다.\n"
                + "ID: " + feedback.getId() + "\n"
                + "카테고리: " + feedback.getCategory() + "\n"
                + "유형: " + feedback.getFeedbackType() + "\n"
                + "내용: " + feedback.getContent();
    }
}
