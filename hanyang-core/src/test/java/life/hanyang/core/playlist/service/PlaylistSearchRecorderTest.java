package life.hanyang.core.playlist.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import life.hanyang.core.playlist.domain.PlaylistSearchEvent;
import life.hanyang.core.playlist.exception.SpotifyRateLimitException;
import life.hanyang.core.playlist.repository.PlaylistSearchEventRepository;
import org.junit.jupiter.api.Test;
import life.hanyang.core.global.config.AsyncConfig;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class PlaylistSearchRecorderTest {
    @Test
    void normalizesAndBoundsKeywordsWithoutHighCardinalityMetricLabels() {
        var repository = mock(PlaylistSearchEventRepository.class);
        var manager = mock(PlatformTransactionManager.class);
        when(manager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        var metrics = new SimpleMeterRegistry();
        var recorder = new PlaylistSearchRecorder(repository, metrics, new ObjectMapper(), manager, Runnable::run);
        recorder.record("CATALOG", "  POP\n  LOVE\u0000  ", 8, System.nanoTime(), null);
        var event = ArgumentCaptor.forClass(PlaylistSearchEvent.class);
        verify(repository).saveAndFlush(event.capture());
        assertThat(event.getValue().getKeyword()).isEqualTo("pop love");
        assertThat(event.getValue().getResultCount()).isEqualTo(8);
        assertThat(metrics.get("playlist.search.requests").counter().count()).isEqualTo(1);
        assertThat(metrics.getMeters()).allSatisfy(meter ->
                assertThat(meter.getId().getTags()).extracting(tag -> tag.getKey()).containsOnly("source", "outcome"));
        var definition = ArgumentCaptor.forClass(TransactionDefinition.class);
        verify(manager).getTransaction(definition.capture());
        assertThat(definition.getValue().getPropagationBehavior()).isEqualTo(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        recorder.record("CATALOG", "😮".repeat(201), 0, System.nanoTime(), null);
        verify(repository, times(2)).saveAndFlush(event.capture());
        assertThat(event.getValue().getKeyword().codePointCount(0, event.getValue().getKeyword().length())).isEqualTo(200);
    }

    @Test
    void preservesQuotaFailureAndDoesNotThrowWhenRecordingFails() {
        var repository = mock(PlaylistSearchEventRepository.class);
        var manager = mock(PlatformTransactionManager.class);
        when(manager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        doThrow(new IllegalStateException("storage down")).when(repository).saveAndFlush(any());
        var recorder = new PlaylistSearchRecorder(repository, new SimpleMeterRegistry(), new ObjectMapper(), manager, Runnable::run);
        assertThatCode(() -> recorder.record("CATALOG", "악뮤", 0, System.nanoTime(),
                new SpotifyRateLimitException(120, true, null))).doesNotThrowAnyException();
        var event = ArgumentCaptor.forClass(PlaylistSearchEvent.class);
        verify(repository).saveAndFlush(event.capture());
        assertThat(event.getValue().getOutcome()).isEqualTo("FAILURE");
        assertThat(event.getValue().getErrorCode()).isEqualTo("PL006");
        verify(manager).rollback(any());
    }

    @Test
    void slowStorageAndFullQueueNeverBlockSearchOrWriteOnCallerThread() throws Exception {
        var repository = mock(PlaylistSearchEventRepository.class);
        var manager = mock(PlatformTransactionManager.class);
        when(manager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        var metrics = new SimpleMeterRegistry();
        var entered = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var executor = new AsyncConfig().playlistSearchExecutor();
        executor.initialize();
        doAnswer(invocation -> {
            assertThat(Thread.currentThread().getName()).startsWith("playlist-search-");
            entered.countDown();
            assertThat(release.await(5, TimeUnit.SECONDS)).isTrue();
            return invocation.getArgument(0);
        }).when(repository).saveAndFlush(any());
        var recorder = new PlaylistSearchRecorder(repository, metrics, new ObjectMapper(), manager, executor);
        try {
            recorder.record("CATALOG", "first", 1, System.nanoTime(), null);
            assertThat(entered.await(2, TimeUnit.SECONDS)).isTrue();
            for (int i = 0; i < 100; i++) {
                recorder.record("CATALOG", "queued", 1, System.nanoTime(), null);
            }
            // The worker remains blocked; this call must return rather than run a write inline.
            recorder.record("CATALOG", "overflow", 1, System.nanoTime(), null);
            assertThat(executor.getThreadPoolExecutor().getQueue()).hasSize(100);
            assertThat(metrics.get("playlist.search.recording.dropped").counter().count()).isEqualTo(1);
            verify(repository, times(1)).saveAndFlush(any());
            assertThat(metrics.get("playlist.search.requests").counter().count()).isEqualTo(102);
        } finally {
            release.countDown();
            executor.shutdown();
        }
        verify(repository, times(101)).saveAndFlush(any());
    }

}
