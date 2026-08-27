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
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Entity
@Table(
        name = "playlist_track_daily_plays",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_track_daily_plays", columnNames = {"track_id", "play_date"})
        },
        indexes = {
                @Index(name = "idx_track_daily_plays_date_count", columnList = "play_date, play_count DESC")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlaylistTrackDailyPlay {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "track_id", nullable = false)
    private String trackId;

    @Column(name = "play_date", nullable = false)
    private LocalDate playDate;

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
    public PlaylistTrackDailyPlay(String trackId, LocalDate playDate, Integer playCount) {
        this.trackId = trackId;
        this.playDate = playDate;
        this.playCount = (playCount != null) ? playCount : 0;
    }

    public static PlaylistTrackDailyPlay createFirstPlay(String trackId, LocalDate playDate) {
        return PlaylistTrackDailyPlay.builder()
                .trackId(trackId)
                .playDate(playDate)
                .playCount(1)
                .build();
    }

    public void incrementPlayCount() {
        this.playCount = (this.playCount == null ? 0 : this.playCount) + 1;
    }
}
