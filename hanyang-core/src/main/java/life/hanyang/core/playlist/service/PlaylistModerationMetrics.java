package life.hanyang.core.playlist.service;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class PlaylistModerationMetrics {
    private final MeterRegistry registry;

    public void recordResult(String jevResult, String geminiResult) {
        registry.counter("playlist.moderation", "jev_result", jevResult,
                "gemini_result", geminiResult).increment();
    }

    public void recordCall(String provider, String result, long startedAt) {
        registry.timer("playlist.moderation.api", "provider", provider, "result", result)
                .record(System.nanoTime() - startedAt, TimeUnit.NANOSECONDS);
    }

    public void recordTokens(String model, String type, Integer count) {
        if (count != null && count >= 0) {
            registry.counter("playlist.moderation.tokens", "provider", "gemini",
                    "model", model, "type", type).increment(count);
        }
    }
}
