package life.hanyang.core.playlist.service;

import life.hanyang.core.global.exception.BusinessException;
import life.hanyang.core.global.exception.ErrorCode;
import life.hanyang.core.playlist.domain.*;
import life.hanyang.core.playlist.dto.*;
import life.hanyang.core.playlist.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlaylistCommentServiceTest {
    @Mock PlaylistSongRepository songs;
    @Mock PlaylistCommentRepository comments;
    @Mock PlaylistModerationService moderation;
    @Mock PlaylistNotificationService notifications;
    @Mock JdbcTemplate jdbc;
    @InjectMocks PlaylistCommentService service;
    final UUID songId = UUID.randomUUID();
    final UUID owner = UUID.randomUUID();
    final UUID visitor = UUID.randomUUID();
    PlaylistSong song;

    @BeforeEach
    void setup() {
        song = PlaylistSong.builder().deviceId(owner).build();
        lenient().when(moderation.validateCommentContent(any(), any(), any(), any(), anyList())).thenReturn(true);
    }

    private PlaylistCommentResponse create(UUID author) {
        when(songs.findActiveByIdForUpdate(songId)).thenReturn(Optional.of(song));
        when(comments.saveAndFlush(any())).thenAnswer(call -> call.getArgument(0));
        return service.create(songId, author, new PlaylistCommentRequest("  좋은 노래네요  "));
    }

    @Test void postAuthorDoesNotConsumeAnonymousNumber() {
        var result = create(owner);
        assertThat(result.authorRole()).isEqualTo(PlaylistCommentResponse.AuthorRole.POST_AUTHOR);
        assertThat(result.anonymousNumber()).isNull();
        assertThat(result.isMine()).isTrue();
        assertThat(result.content()).isEqualTo("좋은 노래네요");
        verify(comments, never()).maxAnonymousNumber(any());
    }

    @Test void nextVisitorGetsNextNumberIncludingDeletedHistory() {
        when(comments.maxAnonymousNumber(songId)).thenReturn(3);
        var result = create(visitor);
        assertThat(result.authorRole()).isEqualTo(PlaylistCommentResponse.AuthorRole.ANONYMOUS);
        assertThat(result.anonymousNumber()).isEqualTo(4);
        assertThat(result.isMine()).isTrue();
    }

    @Test void returningVisitorKeepsNumberAfterDeletingAllComments() {
        var previous = new PlaylistComment(songId, visitor, 2, "이전 댓글");
        previous.delete();
        when(comments.findFirstBySongIdAndDeviceIdOrderByCreatedAtAscIdAsc(songId, visitor))
                .thenReturn(Optional.of(previous));
        assertThat(create(visitor).anonymousNumber()).isEqualTo(2);
        verify(comments, never()).maxAnonymousNumber(any());
    }

    @Test void isMineDependsOnViewerWithoutExposingDeviceId() {
        var comment = new PlaylistComment(songId, visitor, 1, "댓글");
        assertThat(PlaylistCommentResponse.of(comment, visitor).isMine()).isTrue();
        assertThat(PlaylistCommentResponse.of(comment, owner).isMine()).isFalse();
        assertThat(PlaylistCommentResponse.of(comment, null).isMine()).isFalse();
        assertThat(PlaylistCommentResponse.of(comment, owner).anonymousNumber()).isEqualTo(1);
    }

    @Test void cannotDeleteAnotherDevicesComment() {
        var comment = new PlaylistComment(songId, visitor, 1, "댓글");
        when(songs.findActiveByIdForUpdate(songId)).thenReturn(Optional.of(song));
        when(comments.findByIdAndSongIdAndDeletedAtIsNull(comment.getId(), songId)).thenReturn(Optional.of(comment));
        assertThatThrownBy(() -> service.delete(songId, comment.getId(), owner))
                .isInstanceOf(BusinessException.class).extracting("errorCode").isEqualTo(ErrorCode.ACCESS_DENIED);
        assertThat(comment.getDeletedAt()).isNull();
        verifyNoInteractions(notifications);
    }

    @Test void deleteClearsContentButKeepsNumberAndCancelsPendingNotification() {
        var comment = new PlaylistComment(songId, visitor, 2, "댓글");
        when(songs.findActiveByIdForUpdate(songId)).thenReturn(Optional.of(song));
        when(comments.findByIdAndSongIdAndDeletedAtIsNull(comment.getId(), songId)).thenReturn(Optional.of(comment));
        service.delete(songId, comment.getId(), visitor);
        assertThat(comment.getContent()).isEmpty();
        assertThat(comment.getDeletedAt()).isNotNull();
        assertThat(comment.getAnonymousNumber()).isEqualTo(2);
        verify(notifications).cancelComment(comment.getId());
    }

    @Test void rateLimitRejectsBeforeModerationOrSave() {
        when(comments.countByDeviceIdAndCreatedAtAfter(eq(visitor), any())).thenReturn(5L);
        assertThatThrownBy(() -> service.create(songId, visitor, new PlaylistCommentRequest("댓글")))
                .isInstanceOf(BusinessException.class).extracting("errorCode")
                .isEqualTo(ErrorCode.PLAYLIST_COMMENT_RATE_LIMITED);
        verifyNoInteractions(songs, moderation, notifications);
        verify(comments, never()).saveAndFlush(any());
    }

    @Test void moderationRejectionDoesNotSaveOrNotify() {
        when(songs.findActiveByIdForUpdate(songId)).thenReturn(Optional.of(song));
        when(moderation.validateCommentContent(null, null, null, "댓글", List.of()))
                .thenThrow(new BusinessException(ErrorCode.PLAYLIST_INAPPROPRIATE_COMMENT));
        assertThatThrownBy(() -> service.create(songId, visitor, new PlaylistCommentRequest("댓글")))
                .isInstanceOf(BusinessException.class);
        verify(comments, never()).saveAndFlush(any());
        verifyNoInteractions(notifications);
    }

    @Test void acceptsExactly200Characters() {
        when(songs.findActiveByIdForUpdate(songId)).thenReturn(Optional.of(song));
        when(comments.saveAndFlush(any())).thenAnswer(call -> call.getArgument(0));
        String content = "가".repeat(200);
        assertThat(service.create(songId, owner, new PlaylistCommentRequest(content)).content()).isEqualTo(content);
        verify(moderation).validateCommentContent(null, null, null, content, List.of());
        verify(moderation, never()).validateSongContent(any(), any(), any());
    }

    @Test void historyIsPassedChronologicallyForTheSameAuthorAndSong() {
        var first = new PlaylistComment(songId, visitor, 1, "시원하네요");
        var second = new PlaylistComment(songId, visitor, 1, "발라드 좋아요");
        var third = new PlaylistComment(songId, visitor, 1, "새로운 곡이네");
        when(comments.findTop20BySongIdAndDeviceIdAndDeletedAtIsNullOrderByCreatedAtDescIdDesc(songId, visitor))
                .thenReturn(List.of(third, second, first));
        when(songs.findActiveByIdForUpdate(songId)).thenReturn(Optional.of(song));
        when(moderation.validateCommentContent(null, null, null, "끼가 넘치네요",
                List.of("시원하네요", "발라드 좋아요", "새로운 곡이네")))
                .thenThrow(new BusinessException(ErrorCode.PLAYLIST_INAPPROPRIATE_COMMENT));
        assertThatThrownBy(() -> service.create(songId, visitor, new PlaylistCommentRequest("끼가 넘치네요")))
                .isInstanceOf(BusinessException.class);
        verify(comments, never()).saveAndFlush(any());
        verifyNoInteractions(notifications);
    }

    @Test void rejects201CharactersBeforeCallingModeration() {
        when(songs.findActiveByIdForUpdate(songId)).thenReturn(Optional.of(song));
        assertThatThrownBy(() -> service.create(songId, visitor, new PlaylistCommentRequest("가".repeat(201))))
                .isInstanceOf(BusinessException.class).extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
        verify(comments, never()).saveAndFlush(any());
        verifyNoInteractions(moderation, notifications);
    }

    @Test void moderationOutageDoesNotPublishUncheckedComment() {
        when(songs.findActiveByIdForUpdate(songId)).thenReturn(Optional.of(song));
        when(moderation.validateCommentContent(null, null, null, "댓글", List.of())).thenReturn(false);
        assertThatThrownBy(() -> service.create(songId, visitor, new PlaylistCommentRequest("댓글")))
                .isInstanceOf(BusinessException.class).extracting("errorCode")
                .isEqualTo(ErrorCode.PLAYLIST_COMMENT_MODERATION_UNAVAILABLE);
        verify(comments, never()).saveAndFlush(any());
        verifyNoInteractions(notifications);
    }
}
