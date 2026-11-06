package life.hanyang.core.printer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import life.hanyang.core.campusmap.domain.Campus;
import life.hanyang.core.printer.domain.PrinterReportType;
import life.hanyang.core.printer.event.PrinterReportCreatedEvent;
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
public class PrinterReportWebhookNotifier {
    private static final int MAX_CONTENT_LENGTH = 2000;
    private static final Map<String, String> FIELD_LABELS = Map.of(
            "buildingId", "건물 ID", "detailLocation", "세부 위치", "name", "프린터 이름", "price", "가격",
            "paymentMethod", "결제 방식", "notes", "참고 사항", "broken", "고장 여부", "removed", "철거 여부");
    private final RestClient restClient;
    private final ObjectMapper mapper;
    private final String webhookUrl;

    public PrinterReportWebhookNotifier(RestClient.Builder builder, ObjectMapper mapper,
                                        @Value("${feedback.webhook.url:}") String webhookUrl) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(1));
        factory.setReadTimeout(Duration.ofSeconds(2));
        this.restClient = builder.clone().requestFactory(factory).build();
        this.mapper = mapper;
        this.webhookUrl = webhookUrl;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onReportCreated(PrinterReportCreatedEvent event) {
        if (webhookUrl == null || webhookUrl.isBlank()) return;
        try {
            restClient.post().uri(webhookUrl).contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("content", formatMessage(event), "allowed_mentions", Map.of("parse", List.of())))
                    .retrieve().toBodilessEntity();
            log.info("프린터 제보 웹훅 전송 완료. reportId={}", event.reportId());
        } catch (Exception exception) {
            // 제보는 이미 커밋되었다. URL·사용자 입력을 포함할 수 있는 예외 메시지는 기록하지 않는다.
            log.warn("프린터 제보 웹훅 전송 실패. reportId={}, cause={}",
                    event.reportId(), exception.getClass().getSimpleName());
        }
    }

    private String formatMessage(PrinterReportCreatedEvent event) throws Exception {
        JsonNode proposal = mapper.readTree(event.proposal());
        String type = event.type() == PrinterReportType.NEW ? "신규 프린터" :
                proposal.path("removed").asBoolean(false) ? "철거 제보" : "정보 수정";
        StringBuilder message = new StringBuilder("🖨️ 새로운 프린터 제보가 접수됐어요!\n\n")
                .append("제보 ID: ").append(event.reportId()).append('\n')
                .append("유형: ").append(type).append('\n');
        if (event.printerId() != null) message.append("프린터 ID: ").append(event.printerId()).append('\n');
        message.append("캠퍼스: ").append(event.campus() == Campus.ANSAN ? "ERICA" : "서울").append('\n')
                .append("건물: ").append(event.buildingName()).append('\n')
                .append("위치: ").append(event.detailLocation()).append("\n\n제보 내용:\n");
        proposal.fields().forEachRemaining(entry -> {
            String value = entry.getValue().isNull() ? "정보 없음" : entry.getValue().asText();
            if (entry.getValue().isBoolean()) {
                value = entry.getKey().equals("broken") ? (entry.getValue().asBoolean() ? "고장" : "고장 해제")
                        : (entry.getValue().asBoolean() ? "철거됨" : "미철거");
            }
            message.append(FIELD_LABELS.getOrDefault(entry.getKey(), entry.getKey())).append(": ").append(value).append('\n');
        });
        if (event.description() != null && !event.description().isBlank())
            message.append("\n추가 설명:\n").append(event.description());
        String content = message.toString();
        if (content.codePointCount(0, content.length()) <= MAX_CONTENT_LENGTH) return content;
        return content.substring(0, content.offsetByCodePoints(0, MAX_CONTENT_LENGTH - 1)) + "…";
    }
}
