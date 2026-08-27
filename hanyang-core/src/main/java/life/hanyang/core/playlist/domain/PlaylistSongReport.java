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
@Table(name = "playlist_song_reports", indexes = {
        @Index(name = "idx_playlist_song_reports_status_created", columnList = "status, created_at"),
        @Index(name = "idx_playlist_song_reports_created_at", columnList = "created_at")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlaylistSongReport {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "song_id", nullable = false)
    private PlaylistSong song;

    @Column(name = "reporter_device_id", nullable = false)
    private UUID reporterDeviceId;

    @Column(name = "reason", nullable = false, columnDefinition = "TEXT")
    private String reason;

    @ColumnDefault("'PENDING'")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ReportStatus status = ReportStatus.PENDING;

    @Column(name = "admin_memo", columnDefinition = "TEXT")
    private String adminMemo;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Builder
    public PlaylistSongReport(PlaylistSong song, UUID reporterDeviceId, String reason) {
        this.song = song;
        this.reporterDeviceId = reporterDeviceId;
        this.reason = reason;
        this.status = ReportStatus.PENDING;
    }

    public void process(ReportStatus status, String adminMemo) {
        if (status != null) {
            this.status = status;
        }
        if (adminMemo != null) {
            this.adminMemo = adminMemo;
        }
        this.reviewedAt = Instant.now();
    }
}
