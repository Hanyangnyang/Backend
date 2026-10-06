package life.hanyang.core.playlist.service;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PlaylistModerationMetricsTest {
    @Test
    void accumulatesValidUsageAndIgnoresUnavailableCounts() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        PlaylistModerationMetrics metrics = new PlaylistModerationMetrics(registry);
        metrics.recordTokens("test-model", "input", 20);
        metrics.recordTokens("test-model", "input", 30);
        metrics.recordTokens("test-model", "output", null);
        metrics.recordTokens("test-model", "thoughts", -1);
        assertThat(registry.get("playlist.moderation.tokens").tag("type", "input")
                .counter().count()).isEqualTo(50);
        assertThat(registry.find("playlist.moderation.tokens").tag("type", "output").counter()).isNull();
        assertThat(registry.find("playlist.moderation.tokens").tag("type", "thoughts").counter()).isNull();
    }
}
