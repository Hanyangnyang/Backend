package life.hanyang.core.playlist.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.util.UUID;

@Getter
@Entity
@Table(name = "playlist_comment_reports", uniqueConstraints =
        @UniqueConstraint(name = "uq_playlist_comment_reporter", columnNames = {"comment_id", "reporter_device_id"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlaylistCommentReport {
    @Id
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id", nullable = false)
    private PlaylistComment comment;
    @Column(nullable = false)
    private UUID reporterDeviceId;
    @Column(nullable = false, length = 500)
    private String reason;
    @Column(nullable = false, length = 1000)
    private String contentSnapshot;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReportStatus status;
    @Column(length = 1000)
    private String adminMemo;
    private Instant reviewedAt;
    @Column(nullable = false)
    private Instant createdAt;
    @Column(nullable = false)
    private Instant updatedAt;

    public PlaylistCommentReport(PlaylistComment comment, UUID reporterDeviceId, String reason) {
        this.id = UUID.randomUUID();
        this.comment = comment;
        this.reporterDeviceId = reporterDeviceId;
        this.reason = reason;
        this.contentSnapshot = comment.getContent();
        this.status = ReportStatus.PENDING;
        this.createdAt = Instant.now();
        this.updatedAt = createdAt;
    }

    public void process(ReportStatus status, String adminMemo) {
        this.status = status;
        if (adminMemo != null) this.adminMemo = adminMemo;
        this.reviewedAt = status == ReportStatus.PENDING ? null : Instant.now();
        this.updatedAt = Instant.now();
    }
}
