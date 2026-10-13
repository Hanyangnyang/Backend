package life.hanyang.core.playlist.service;

import life.hanyang.core.global.exception.*;
import life.hanyang.core.playlist.domain.*;
import life.hanyang.core.playlist.dto.*;
import life.hanyang.core.playlist.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlaylistCommentReportServiceTest {
    @Mock PlaylistSongRepository songs;
    @Mock PlaylistCommentRepository comments;
    @Mock PlaylistCommentReportRepository reports;
    @Mock PlaylistNotificationService notifications;
    @InjectMocks PlaylistCommentReportService service;
    final UUID songId = UUID.randomUUID();
    final UUID reporter = UUID.randomUUID();
    final PlaylistComment comment = new PlaylistComment(songId, UUID.randomUUID(), 2, "신고 당시 내용");

    private void activeComment() {
        when(songs.findActiveByIdForUpdate(songId)).thenReturn(Optional.of(PlaylistSong.builder().build()));
        when(comments.findByIdAndSongIdAndDeletedAtIsNull(comment.getId(), songId)).thenReturn(Optional.of(comment));
    }

    @Test void storesSnapshotAndReturnsOnlyReceipt() {
        activeComment();
        when(reports.saveAndFlush(any())).thenAnswer(call -> call.getArgument(0));
        var receipt = service.report(songId, comment.getId(), reporter, new PlaylistCommentReportRequest("  욕설  "));
        var saved = ArgumentCaptor.forClass(PlaylistCommentReport.class);
        verify(reports).saveAndFlush(saved.capture());
        assertThat(receipt.status()).isEqualTo(ReportStatus.PENDING);
        assertThat(saved.getValue().getReason()).isEqualTo("욕설");
        comment.delete();
        assertThat(saved.getValue().getContentSnapshot()).isEqualTo("신고 당시 내용");
    }

    @Test void duplicateIsConflictEvenAfterPreviousReportProcessed() {
        activeComment();
        when(reports.existsByCommentIdAndReporterDeviceId(comment.getId(), reporter)).thenReturn(true);
        assertThatThrownBy(() -> service.report(songId, comment.getId(), reporter, new PlaylistCommentReportRequest("욕설")))
                .isInstanceOf(BusinessException.class).extracting("errorCode").isEqualTo(ErrorCode.DUPLICATE_RESOURCE);
        verify(reports, never()).saveAndFlush(any());
    }

    @Test void deletedOrWrongSongCommentCannotBeReported() {
        when(songs.findActiveByIdForUpdate(songId)).thenReturn(Optional.of(PlaylistSong.builder().build()));
        assertThatThrownBy(() -> service.report(songId, comment.getId(), reporter, new PlaylistCommentReportRequest("욕설")))
                .isInstanceOf(EntityNotFoundException.class);
        verifyNoInteractions(reports);
    }

    @Test void adminDeleteClearsContentResolvesPendingReportsAndCancelsPush() {
        when(songs.findByIdForUpdate(songId)).thenReturn(Optional.of(PlaylistSong.builder().build()));
        when(comments.findByIdAndSongId(comment.getId(), songId)).thenReturn(Optional.of(comment));
        service.deleteByAdmin(songId, comment.getId(), "유해 댓글");
        assertThat(comment.getDeletedAt()).isNotNull();
        assertThat(comment.getContent()).isEmpty();
        assertThat(comment.getAnonymousNumber()).isEqualTo(2);
        verify(notifications).cancelComment(comment.getId());
        verify(reports).resolvePendingByCommentId(eq(comment.getId()), eq(ReportStatus.REVIEWED), eq("유해 댓글"), any(), eq(ReportStatus.PENDING));
    }

    @Test void adminCanResolveReportsForAlreadyDeletedComment() {
        comment.delete();
        when(songs.findByIdForUpdate(songId)).thenReturn(Optional.of(PlaylistSong.builder().build()));
        when(comments.findByIdAndSongId(comment.getId(), songId)).thenReturn(Optional.of(comment));
        service.deleteByAdmin(songId, comment.getId(), null);
        verify(reports).resolvePendingByCommentId(eq(comment.getId()), eq(ReportStatus.REVIEWED), anyString(), any(), eq(ReportStatus.PENDING));
    }

    @Test void processLocksSongBeforeLoadingReportAndDoesNotDeleteComment() {
        var report = new PlaylistCommentReport(comment, reporter, "욕설");
        when(reports.findSongIdByReportId(report.getId())).thenReturn(Optional.of(songId));
        when(songs.findByIdForUpdate(songId)).thenReturn(Optional.of(PlaylistSong.builder().build()));
        when(reports.findById(report.getId())).thenReturn(Optional.of(report));
        var result = service.process(report.getId(), new PlaylistReportProcessRequest(ReportStatus.DISMISSED, "문제 없음"));
        assertThat(result.status()).isEqualTo(ReportStatus.DISMISSED);
        assertThat(result.reviewedAt()).isNotNull();
        assertThat(comment.getDeletedAt()).isNull();
        var order = inOrder(songs, reports);
        order.verify(reports).findSongIdByReportId(report.getId());
        order.verify(songs).findByIdForUpdate(songId);
        order.verify(reports).findById(report.getId());
        verifyNoInteractions(notifications);
    }
}
