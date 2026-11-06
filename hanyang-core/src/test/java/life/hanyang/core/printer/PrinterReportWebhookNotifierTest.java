package life.hanyang.core.printer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import life.hanyang.core.campusmap.domain.Campus;
import life.hanyang.core.printer.domain.PrinterReportType;
import life.hanyang.core.printer.event.PrinterReportCreatedEvent;
import life.hanyang.core.printer.service.PrinterReportWebhookNotifier;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.event.TransactionalEventListenerFactory;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClient;
import java.net.InetSocketAddress;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import static org.assertj.core.api.Assertions.*;

class PrinterReportWebhookNotifierTest {
    private HttpServer server;
    private String webhookUrl;
    private int responseStatus = 204;
    private final AtomicInteger requests = new AtomicInteger();
    private final AtomicReference<JsonNode> payload = new AtomicReference<>();

    @BeforeEach
    void start() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/feedback-webhook", exchange -> {
            requests.incrementAndGet();
            payload.set(new ObjectMapper().readTree(exchange.getRequestBody()));
            exchange.sendResponseHeaders(responseStatus, -1);
            exchange.close();
        });
        server.start();
        webhookUrl = "http://127.0.0.1:" + server.getAddress().getPort() + "/feedback-webhook";
    }
    @AfterEach void stop() { server.stop(0); }
    private PrinterReportWebhookNotifier notifier(String url) {
        return new PrinterReportWebhookNotifier(RestClient.builder(), new ObjectMapper(), url);
    }
    private PrinterReportCreatedEvent event(PrinterReportType type, String proposal, String description) {
        return new PrinterReportCreatedEvent(UUID.randomUUID(), type,
                type == PrinterReportType.NEW ? null : UUID.randomUUID(), Campus.ANSAN,
                "학술정보관", "1층 로비", proposal, description);
    }

    @Test
    void sendsNewReportWithContextAndDisablesMentions() {
        var event = event(PrinterReportType.NEW, "{\"price\":\"A4 흑백 100원\",\"name\":null}", "@everyone 확인 부탁드립니다.");
        notifier(webhookUrl).onReportCreated(event);
        assertThat(requests.get()).isEqualTo(1);
        assertThat(payload.get().path("content").asText()).contains("신규 프린터", event.reportId().toString(),
                "ERICA", "학술정보관", "1층 로비", "가격: A4 흑백 100원", "프린터 이름: 정보 없음", "@everyone 확인 부탁드립니다.");
        assertThat(payload.get().path("allowed_mentions").path("parse").isArray()).isTrue();
        assertThat(payload.get().path("allowed_mentions").path("parse").size()).isZero();
    }

    @Test
    void labelsUpdatesAndRemovalReportsAndPreservesExplicitFalse() {
        notifier(webhookUrl).onReportCreated(event(PrinterReportType.UPDATE, "{\"broken\":false}", null));
        assertThat(payload.get().path("content").asText()).contains("유형: 정보 수정", "고장 여부: 고장 해제");
        notifier(webhookUrl).onReportCreated(event(PrinterReportType.UPDATE, "{\"removed\":true}", null));
        assertThat(payload.get().path("content").asText()).contains("유형: 철거 제보", "철거 여부: 철거됨");
    }

    @Test
    void skipsUnconfiguredWebhooks() {
        for (String url : new String[]{null, "", " "})
            notifier(url).onReportCreated(event(PrinterReportType.NEW, "{}", null));
        assertThat(requests.get()).isZero();
    }

    @Test
    void webhookOrFormattingFailureDoesNotPropagateToReportSubmission() {
        responseStatus = 500;
        assertThatCode(() -> notifier(webhookUrl).onReportCreated(event(PrinterReportType.NEW, "{}", null)))
                .doesNotThrowAnyException();
        assertThat(requests.get()).isEqualTo(1);
        assertThatCode(() -> notifier(webhookUrl).onReportCreated(event(PrinterReportType.NEW, "bad json", null)))
                .doesNotThrowAnyException();
        assertThat(requests.get()).isEqualTo(1);
    }

    @Test
    void truncatesLongContentWithoutSplittingEmoji() {
        notifier(webhookUrl).onReportCreated(event(PrinterReportType.NEW, "{}", "😀".repeat(3000)));
        var content = payload.get().path("content").asText();
        assertThat(content.codePointCount(0, content.length())).isEqualTo(2000);
        assertThat(content).endsWith("…");
        assertThat(content).doesNotContain("\uFFFD");
    }

    @Test
    void onlyCommittedReportsSendAndWebhooksFailWithoutChangingCommitResult() {
        try (var context = new AnnotationConfigApplicationContext()) {
            context.registerBean(TransactionalEventListenerFactory.class);
            context.registerBean(PrinterReportWebhookNotifier.class, () -> notifier(webhookUrl));
            context.refresh();
            var transaction = new TransactionTemplate(new TestTransactionManager());
            context.publishEvent(event(PrinterReportType.NEW, "{}", null));
            transaction.executeWithoutResult(status -> {
                context.publishEvent(event(PrinterReportType.NEW, "{}", null));
                status.setRollbackOnly();
            });
            assertThat(requests.get()).isZero();
            transaction.executeWithoutResult(status -> {
                context.publishEvent(event(PrinterReportType.UPDATE, "{}", null));
                assertThat(requests.get()).isZero();
            });
            assertThat(requests.get()).isEqualTo(1);
            responseStatus = 500;
            assertThatCode(() -> transaction.executeWithoutResult(status ->
                    context.publishEvent(event(PrinterReportType.NEW, "{}", null)))).doesNotThrowAnyException();
            assertThat(requests.get()).isEqualTo(2);
        }
    }
    private static class TestTransactionManager extends AbstractPlatformTransactionManager {
        @Override protected Object doGetTransaction() { return new Object(); }
        @Override protected void doBegin(Object transaction, TransactionDefinition definition) {}
        @Override protected void doCommit(DefaultTransactionStatus status) {}
        @Override protected void doRollback(DefaultTransactionStatus status) {}
    }
}
