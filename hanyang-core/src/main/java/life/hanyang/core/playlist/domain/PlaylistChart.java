package life.hanyang.core.playlist.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Getter
@Entity
@Table(
        name = "playlist_charts",
        indexes = {
                @Index(name = "idx_playlist_charts_lookup", columnList = "chart_type, genre, snapshot_time DESC, rank")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlaylistChart {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "chart_type", nullable = false, length = 20)
    private ChartType chartType;

    @Enumerated(EnumType.STRING)
    @Column(name = "genre", length = 30)
    private Genre genre;

    @Column(name = "snapshot_time", nullable = false)
    private Instant snapshotTime;

    @Column(name = "start_period", nullable = false)
    private Instant startPeriod;

    @Column(name = "end_period", nullable = false)
    private Instant endPeriod;

    @Column(name = "rank", nullable = false)
    private Integer rank;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "track_id", nullable = false)
    private PlaylistTrack track;

    @Column(name = "total_score", nullable = false)
    private Long totalScore = 0L;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Builder
    public PlaylistChart(
            ChartType chartType,
            Genre genre,
            Instant snapshotTime,
            Instant startPeriod,
            Instant endPeriod,
            Integer rank,
            PlaylistTrack track,
            Long totalScore
    ) {
        this.chartType = chartType;
        this.genre = genre;
        this.snapshotTime = snapshotTime;
        this.startPeriod = startPeriod;
        this.endPeriod = endPeriod;
        this.rank = rank;
        this.track = track;
        this.totalScore = (totalScore != null) ? totalScore : 0L;
    }
}
