package life.hanyang.core.playlist.dto;

import life.hanyang.core.playlist.domain.PlaylistComment;
import java.time.Instant;
import java.util.UUID;

public record PlaylistCommentResponse(UUID id, String content, AuthorRole authorRole,
                                      Integer anonymousNumber, boolean isMine, Instant createdAt) {
    public enum AuthorRole { POST_AUTHOR, ANONYMOUS }

    public static PlaylistCommentResponse of(PlaylistComment comment, UUID viewerId) {
        return new PlaylistCommentResponse(comment.getId(), comment.getContent(),
                comment.getAnonymousNumber() == null ? AuthorRole.POST_AUTHOR : AuthorRole.ANONYMOUS,
                comment.getAnonymousNumber(), comment.getDeviceId().equals(viewerId), comment.getCreatedAt());
    }
}
