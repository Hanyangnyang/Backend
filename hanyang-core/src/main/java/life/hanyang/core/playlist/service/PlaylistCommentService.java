package life.hanyang.core.playlist.service;

import life.hanyang.core.global.exception.BusinessException;
import life.hanyang.core.global.exception.EntityNotFoundException;
import life.hanyang.core.global.exception.ErrorCode;
import life.hanyang.core.playlist.domain.PlaylistComment;
import life.hanyang.core.playlist.dto.*;
import life.hanyang.core.playlist.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.UUID;
import java.util.ArrayList;
import java.util.Collections;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlaylistCommentService {
    private final PlaylistSongRepository songs;
    private final PlaylistCommentRepository comments;
    private final PlaylistModerationService moderation;
    private final PlaylistNotificationService notifications;
    private final JdbcTemplate jdbc;

    public Page<PlaylistCommentResponse> list(UUID songId, UUID viewerId, Pageable pageable) {
        if (!songs.existsByIdAndDeletedAtIsNull(songId)) throw new EntityNotFoundException("존재하지 않는 추천글입니다.");
        return comments.findBySongIdAndDeletedAtIsNull(songId, pageable)
                .map(comment -> PlaylistCommentResponse.of(comment, viewerId));
    }

    @Transactional
    public PlaylistCommentResponse create(UUID songId, UUID authorId, PlaylistCommentRequest request) {
        // 기기별 잠금으로 서로 다른 추천글에 동시에 작성해도 도배 제한을 우회할 수 없다.
        jdbc.queryForList("select pg_advisory_xact_lock(hashtextextended(?, 0))", "playlist-comment:" + authorId);
        if (comments.countByDeviceIdAndCreatedAtAfter(authorId, Instant.now().minusSeconds(60)) >= 5) {
            throw new BusinessException(ErrorCode.PLAYLIST_COMMENT_RATE_LIMITED);
        }
        var song = songs.findActiveByIdForUpdate(songId)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 추천글입니다."));
        String content = request.content().strip();
        if (content.isBlank() || request.content().length() > PlaylistCommentRequest.MAX_CONTENT_LENGTH) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        // 추천글 잠금 안에서 이력을 읽어 동시 요청이 아직 커밋되지 않은 조각을 건너뛰지 않게 한다.
        var recent = new ArrayList<>(comments.findTop20BySongIdAndDeviceIdAndDeletedAtIsNullOrderByCreatedAtDescIdDesc(songId, authorId));
        Collections.reverse(recent);
        if (!moderation.validateCommentContent(song.getTitle(), song.getArtist(), song.getComment(), content,
                recent.stream().map(PlaylistComment::getContent).toList())) {
            throw new BusinessException(ErrorCode.PLAYLIST_COMMENT_MODERATION_UNAVAILABLE);
        }
        Integer number = song.isOwnedBy(authorId) ? null : comments
                .findFirstBySongIdAndDeviceIdOrderByCreatedAtAscIdAsc(songId, authorId)
                .map(PlaylistComment::getAnonymousNumber).orElseGet(() -> comments.maxAnonymousNumber(songId) + 1);
        // JDBC 알림 INSERT의 FK가 댓글을 참조하므로 같은 트랜잭션에서 먼저 flush한다.
        var comment = comments.saveAndFlush(new PlaylistComment(songId, authorId, number, content));
        notifications.commentCreated(song, comment);
        return PlaylistCommentResponse.of(comment, authorId);
    }

    @Transactional
    public void delete(UUID songId, UUID commentId, UUID authorId) {
        songs.findActiveByIdForUpdate(songId)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 추천글입니다."));
        var comment = comments.findByIdAndSongIdAndDeletedAtIsNull(commentId, songId)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 댓글입니다."));
        if (!comment.getDeviceId().equals(authorId)) throw new BusinessException(ErrorCode.ACCESS_DENIED);
        comment.delete();
        notifications.cancelComment(commentId);
    }
}
