package life.hanyang.core.playlist.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;
import java.time.Instant;
import java.util.UUID;

@Getter
@Entity
@Table(name = "playlist_search_events", indexes = {
        @Index(name = "idx_playlist_search_events_time", columnList = "searched_at DESC")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlaylistSearchEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "searched_at", nullable = false)
    private Instant searchedAt;
    @Column(nullable = false, length = 16)
    private String source;
    @Column(nullable = false, length = 200)
    private String keyword;
    @Column(name = "result_count", nullable = false)
    private long resultCount;
    @Column(nullable = false, length = 16)
    private String outcome;
    @Column(name = "error_code", length = 32)
    private String errorCode;
    @Column(name = "duration_ms", nullable = false)
    private long durationMs;

    public PlaylistSearchEvent(String source, String keyword, long resultCount, String outcome,
                               String errorCode, long durationMs) {
        this.searchedAt = Instant.now();
        this.source = source;
        this.keyword = keyword;
        this.resultCount = resultCount;
        this.outcome = outcome;
        this.errorCode = errorCode;
        this.durationMs = durationMs;
    }
}
