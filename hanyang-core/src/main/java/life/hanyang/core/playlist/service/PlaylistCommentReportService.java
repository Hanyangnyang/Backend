package life.hanyang.core.playlist.service;

import life.hanyang.core.global.exception.*;
import life.hanyang.core.playlist.domain.*;
import life.hanyang.core.playlist.dto.*;
import life.hanyang.core.playlist.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlaylistCommentReportService {
    private final PlaylistSongRepository songs;
    private final PlaylistCommentRepository comments;
    private final PlaylistCommentReportRepository reports;
    private final PlaylistNotificationService notifications;

    @Transactional
    public PlaylistCommentReportReceipt report(UUID songId, UUID commentId, UUID reporterId, PlaylistCommentReportRequest request) {
        songs.findActiveByIdForUpdate(songId).orElseThrow(() -> new EntityNotFoundException("존재하지 않는 추천글입니다."));
        var comment = comments.findByIdAndSongIdAndDeletedAtIsNull(commentId, songId)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 댓글입니다."));
        if (request.reason() == null || request.reason().isBlank() || request.reason().length() > 500) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        if (reports.existsByCommentIdAndReporterDeviceId(commentId, reporterId)) {
            throw new BusinessException("이미 신고한 댓글입니다.", ErrorCode.DUPLICATE_RESOURCE);
        }
        return PlaylistCommentReportReceipt.of(reports.saveAndFlush(new PlaylistCommentReport(comment, reporterId, request.reason().strip())));
    }

    public Page<PlaylistCommentReportResponse> list(ReportStatus status, Pageable pageable) {
        return (status == null ? reports.findAllByOrderByCreatedAtDescIdDesc(pageable)
                : reports.findAllByStatusOrderByCreatedAtDescIdDesc(status, pageable)).map(PlaylistCommentReportResponse::of);
    }

    @Transactional
    public PlaylistCommentReportResponse process(UUID reportId, PlaylistReportProcessRequest request) {
        if (request.status() == null || request.adminMemo() != null && request.adminMemo().length() > 1000) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        UUID songId = reports.findSongIdByReportId(reportId)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 신고입니다."));
        // 댓글 삭제 및 다른 신고 처리와 같은 추천글 잠금 순서를 사용한다.
        songs.findByIdForUpdate(songId)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 추천글입니다."));
        var report = reports.findById(reportId).orElseThrow(() -> new EntityNotFoundException("존재하지 않는 신고입니다."));
        report.process(request.status(), request.adminMemo());
        return PlaylistCommentReportResponse.of(report);
    }

    @Transactional
    public void deleteByAdmin(UUID songId, UUID commentId, String reason) {
        if (reason != null && reason.length() > 1000) throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        songs.findByIdForUpdate(songId).orElseThrow(() -> new EntityNotFoundException("존재하지 않는 추천글입니다."));
        var comment = comments.findByIdAndSongId(commentId, songId)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 댓글입니다."));
        if (comment.getDeletedAt() == null) comment.delete();
        notifications.cancelComment(commentId);
        reports.resolvePendingByCommentId(commentId, ReportStatus.REVIEWED,
                reason == null || reason.isBlank() ? "관리자에 의한 댓글 삭제" : reason.strip(), Instant.now(), ReportStatus.PENDING);
    }
}
