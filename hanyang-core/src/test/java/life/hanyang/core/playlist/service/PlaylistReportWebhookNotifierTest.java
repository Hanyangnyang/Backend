package life.hanyang.core.playlist.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import life.hanyang.core.playlist.event.PlaylistReportCreatedEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.event.TransactionalEventListenerFactory;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class PlaylistReportWebhookNotifierTest {
    private HttpServer server;
    private String webhookUrl;
    private final AtomicInteger requests = new AtomicInteger();
    private final AtomicReference<JsonNode> payload = new AtomicReference<>();
    private int responseStatus = 204;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/webhook", exchange -> {
            requests.incrementAndGet();
            payload.set(new ObjectMapper().readTree(exchange.getRequestBody()));
            exchange.sendResponseHeaders(responseStatus, -1);
            exchange.close();
        });
        server.start();
        webhookUrl = "http://127.0.0.1:" + server.getAddress().getPort() + "/webhook";
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    private PlaylistReportCreatedEvent event(String reason) {
        return new PlaylistReportCreatedEvent(
                UUID.randomUUID(), UUID.randomUUID(), "LOVE SONG", "유다빈밴드",
                "과제할 때 들으면 극락\n시험 기간에 추천합니다.", reason);
    }

    private PlaylistReportWebhookNotifier notifier() {
        return new PlaylistReportWebhookNotifier(RestClient.builder(), webhookUrl);
    }

    @Test
    void sendsJsonWithRequestedMessageAndDisablesMentions() {
        PlaylistReportCreatedEvent event = event("@everyone\n확인 부탁드립니다.");
        notifier().onReportCreated(event);
        assertThat(requests.get()).isEqualTo(1);
        assertThat(payload.get().get("content").asText()).isEqualTo(
                "🚨 새로운 플레이리스트 신고가 접수됐어요!\n\n"
                        + "신고 ID: " + event.reportId() + "\n"
                        + "추천글 ID: " + event.songId() + "\n\n"
                        + "곡명: LOVE SONG\n가수: 유다빈밴드\n\n"
                        + "추천글 내용:\n과제할 때 들으면 극락\n시험 기간에 추천합니다.\n\n"
                        + "신고 사유:\n@everyone\n확인 부탁드립니다.");
        assertThat(payload.get().path("allowed_mentions").path("parse").isArray()).isTrue();
        assertThat(payload.get().path("allowed_mentions").path("parse").size()).isZero();
    }

    @Test
    void skipsWhenUrlIsMissing() {
        new PlaylistReportWebhookNotifier(RestClient.builder(), "").onReportCreated(event("신고"));
        assertThat(requests.get()).isZero();
    }

    @Test
    void webhookFailureDoesNotPropagate() {
        responseStatus = 500;
        assertThatCode(() -> notifier().onReportCreated(event("신고"))).doesNotThrowAnyException();
        assertThat(requests.get()).isEqualTo(1);
    }

    @Test
    void truncatesLongReasonsToDiscordContentLimit() {
        notifier().onReportCreated(event("😀".repeat(3000)));
        String content = payload.get().get("content").asText();
        assertThat(content.codePointCount(0, content.length())).isEqualTo(2000);
        assertThat(content).endsWith("…");
    }

    @Test
    void onlySendsAfterCommitAndSkipsRollback() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(TransactionalEventListenerFactory.class);
            context.registerBean(PlaylistReportWebhookNotifier.class, this::notifier);
            context.refresh();
            TransactionTemplate transaction = new TransactionTemplate(new TestTransactionManager());
            transaction.executeWithoutResult(status -> {
                context.publishEvent(event("롤백되는 신고"));
                status.setRollbackOnly();
            });
            assertThat(requests.get()).isZero();
            transaction.executeWithoutResult(status -> {
                context.publishEvent(event("정상 신고"));
                assertThat(requests.get()).isZero();
            });
            assertThat(requests.get()).isEqualTo(1);
        }
    }

    private static class TestTransactionManager extends AbstractPlatformTransactionManager {
        @Override
        protected Object doGetTransaction() { return new Object(); }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {}

        @Override
        protected void doCommit(DefaultTransactionStatus status) {}

        @Override
        protected void doRollback(DefaultTransactionStatus status) {}
    }
}
