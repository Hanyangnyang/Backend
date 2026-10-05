package life.hanyang.core.playlist.service;

import life.hanyang.core.playlist.event.PlaylistReportCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class PlaylistReportWebhookNotifier {

    private static final int MAX_CONTENT_LENGTH = 2000;

    private final RestClient restClient;
    private final String webhookUrl;

    public PlaylistReportWebhookNotifier(
            RestClient.Builder builder,
            @Value("${playlist.report.webhook.url:}") String webhookUrl
    ) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(1));
        requestFactory.setReadTimeout(Duration.ofSeconds(2));
        this.restClient = builder.clone().requestFactory(requestFactory).build();
        this.webhookUrl = webhookUrl;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onReportCreated(PlaylistReportCreatedEvent event) {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            return;
        }

        try {
            restClient.post()
                    .uri(webhookUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "content", formatMessage(event),
                            "allowed_mentions", Map.of("parse", List.of())
                    ))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception exception) {
            // 신고는 이미 커밋되었으므로 알림 실패가 접수 결과에 영향을 주지 않는다.
            log.warn("플레이리스트 신고 웹훅 전송 실패. reportId={}, cause={}",
                    event.reportId(), exception.getClass().getSimpleName());
        }
    }

    private String formatMessage(PlaylistReportCreatedEvent event) {
        String message = "🚨 새로운 플레이리스트 신고가 접수됐어요!\n\n"
                + "신고 ID: " + event.reportId() + "\n"
                + "추천글 ID: " + event.songId() + "\n\n"
                + "곡명: " + event.title() + "\n"
                + "가수: " + event.artist() + "\n\n"
                + "추천글 내용:\n" + (event.comment() == null ? "" : event.comment()) + "\n\n"
                + "신고 사유:\n" + event.reason();
        int length = message.codePointCount(0, message.length());
        if (length <= MAX_CONTENT_LENGTH) {
            return message;
        }
        return message.substring(0, message.offsetByCodePoints(0, MAX_CONTENT_LENGTH - 1)) + "…";
    }
}
