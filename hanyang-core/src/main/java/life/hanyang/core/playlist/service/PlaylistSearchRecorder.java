package life.hanyang.core.playlist.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import life.hanyang.core.global.exception.BusinessException;
import life.hanyang.core.playlist.domain.PlaylistSearchEvent;
import life.hanyang.core.playlist.repository.PlaylistSearchEventRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.LinkedHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

@Slf4j
@Component
public class PlaylistSearchRecorder {
    private final PlaylistSearchEventRepository repository;
    private final MeterRegistry registry;
    private final ObjectMapper mapper;
    private final TransactionTemplate transaction;
    private final Executor executor;

    public PlaylistSearchRecorder(PlaylistSearchEventRepository repository, MeterRegistry registry,
                                  ObjectMapper mapper, PlatformTransactionManager manager,
                                  @Qualifier("playlistSearchExecutor") Executor executor) {
        this.executor = executor;
        this.repository = repository;
        this.registry = registry;
        this.mapper = mapper;
        this.transaction = new TransactionTemplate(manager);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        transaction.setTimeout(2);
    }

    public void record(String source, String keyword, long resultCount, long startedAt, RuntimeException failure) {
        // Observability failures must never change a search response or its surrounding transaction.
        try {
            String normalized = SpotifyTrackSearchService.normalize(keyword);
            normalized = normalized.replaceAll("[\\p{Cc}\\p{Cf}]", " ").strip();
            normalized = normalized.substring(0, normalized.offsetByCodePoints(0,
                    Math.min(200, normalized.codePointCount(0, normalized.length()))));
            String outcome = failure == null ? "SUCCESS" : "FAILURE";
            String errorCode = failure == null ? null : failure instanceof BusinessException business
                    ? business.getErrorCode().getCode() : "INTERNAL_ERROR";
            long elapsed = System.nanoTime() - startedAt;
            registry.counter("playlist.search.requests", "source", source, "outcome", outcome).increment();
            registry.timer("playlist.search.duration", "source", source, "outcome", outcome)
                    .record(elapsed, TimeUnit.NANOSECONDS);
            var event = new PlaylistSearchEvent(source, normalized, resultCount, outcome, errorCode,
                    TimeUnit.NANOSECONDS.toMillis(elapsed));
            try {
                executor.execute(() -> persist(event));
            } catch (RejectedExecutionException exception) {
                registry.counter("playlist.search.recording.dropped", "source", source).increment();
            }
        } catch (RuntimeException exception) {
            log.warn("[PlaylistSearchEvent] 기록 실패 - source={}, exception={}", source,
                    exception.getClass().getSimpleName());
        }
    }

    private void persist(PlaylistSearchEvent event) {
        try {
            var fields = new LinkedHashMap<String, Object>();
            fields.put("event", "playlist_search");
            fields.put("source", event.getSource());
            fields.put("keyword", event.getKeyword());
            fields.put("resultCount", event.getResultCount());
            fields.put("outcome", event.getOutcome());
            fields.put("errorCode", event.getErrorCode());
            fields.put("durationMs", event.getDurationMs());
            log.info("[PlaylistSearchEvent] {}", mapper.writeValueAsString(fields));
        } catch (RuntimeException | JsonProcessingException exception) {
            registry.counter("playlist.search.recording.failed", "stage", "log").increment();
        }
        try {
            transaction.executeWithoutResult(status -> repository.saveAndFlush(event));
        } catch (RuntimeException exception) {
            registry.counter("playlist.search.recording.failed", "stage", "database").increment();
            log.warn("[PlaylistSearchEvent] DB 기록 실패 - source={}, exception={}", event.getSource(),
                    exception.getClass().getSimpleName());
        }
    }

}
