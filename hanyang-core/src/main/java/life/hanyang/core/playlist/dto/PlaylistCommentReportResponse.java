package life.hanyang.core.playlist.dto;

import life.hanyang.core.playlist.domain.*;
import java.time.Instant;
import java.util.UUID;

/** 관리자 전용: 삭제 후에도 신고 당시의 내용을 검토할 수 있다. */
public record PlaylistCommentReportResponse(UUID id, UUID songId, UUID commentId,
        String contentSnapshot, boolean commentDeleted, PlaylistCommentResponse.AuthorRole authorRole,
        Integer anonymousNumber, UUID reporterDeviceId, String reason, ReportStatus status,
        String adminMemo, Instant reviewedAt, Instant createdAt) {
    public static PlaylistCommentReportResponse of(PlaylistCommentReport report) {
        var comment = report.getComment();
        return new PlaylistCommentReportResponse(report.getId(), comment.getSongId(), comment.getId(),
                report.getContentSnapshot(), comment.getDeletedAt() != null,
                comment.getAnonymousNumber() == null ? PlaylistCommentResponse.AuthorRole.POST_AUTHOR : PlaylistCommentResponse.AuthorRole.ANONYMOUS,
                comment.getAnonymousNumber(), report.getReporterDeviceId(), report.getReason(), report.getStatus(),
                report.getAdminMemo(), report.getReviewedAt(), report.getCreatedAt());
    }
}
