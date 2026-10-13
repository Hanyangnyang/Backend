package life.hanyang.core.playlist.dto;

import life.hanyang.core.playlist.domain.PlaylistCommentReport;
import life.hanyang.core.playlist.domain.ReportStatus;
import java.time.Instant;
import java.util.UUID;

public record PlaylistCommentReportReceipt(UUID id, ReportStatus status, Instant createdAt) {
    public static PlaylistCommentReportReceipt of(PlaylistCommentReport report) {
        return new PlaylistCommentReportReceipt(report.getId(), report.getStatus(), report.getCreatedAt());
    }
}
