package life.hanyang.core.playlist.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Getter
@Entity
@Table(
        name = "playlist_track_hourly_plays",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_track_hourly_plays", columnNames = {"track_id", "play_hour"})
        },
        indexes = {
                @Index(name = "idx_track_hourly_plays_hour_count", columnList = "play_hour, play_count DESC"),
                @Index(name = "idx_track_hourly_plays_track_hour", columnList = "track_id, play_hour")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlaylistTrackHourlyPlay {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "track_id", nullable = false)
    private String trackId;

    @Column(name = "play_hour", nullable = false)
    private Instant playHour;

    @ColumnDefault("0")
    @Column(name = "play_count", nullable = false)
    private Integer playCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Builder
    public PlaylistTrackHourlyPlay(String trackId, Instant playHour, Integer playCount) {
        this.trackId = trackId;
        this.playHour = playHour;
        this.playCount = (playCount != null) ? playCount : 0;
    }

    public void incrementPlayCount() {
        this.playCount = (this.playCount == null ? 0 : this.playCount) + 1;
    }
}
