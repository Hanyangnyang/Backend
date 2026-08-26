package life.hanyang.core.playlist.dto;

import life.hanyang.core.playlist.domain.PlaylistSongReport;
import life.hanyang.core.playlist.domain.ReportStatus;

import java.time.Instant;
import java.util.UUID;

public record PlaylistSongReportResponse(
        UUID id,
        UUID songId,
        String songTitle,
        String songArtist,
        UUID reporterUserId,
        String reason,
        ReportStatus status,
        String adminMemo,
        Instant reviewedAt,
        Instant createdAt
) {
    public static PlaylistSongReportResponse from(PlaylistSongReport report) {
        return new PlaylistSongReportResponse(
                report.getId(),
                report.getSong().getId(),
                report.getSong().getTitle(),
                report.getSong().getArtist(),
                report.getReporterUserId(),
                report.getReason(),
                report.getStatus(),
                report.getAdminMemo(),
                report.getReviewedAt(),
                report.getCreatedAt()
        );
    }
}
