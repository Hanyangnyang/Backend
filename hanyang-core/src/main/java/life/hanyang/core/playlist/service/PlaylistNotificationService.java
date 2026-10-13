package life.hanyang.core.playlist.service;

import life.hanyang.core.playlist.domain.PlaylistComment;
import life.hanyang.core.playlist.domain.PlaylistSong;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class PlaylistNotificationService {
    private final JdbcTemplate jdbc;

    public void commentCreated(PlaylistSong song, PlaylistComment comment) {
        if (!song.isOwnedBy(comment.getDeviceId())) {
            record(song, "COMMENT", "comment:" + comment.getId(), comment.getId());
        }
    }

    public void firstReaction(PlaylistSong song, UUID actorId) {
        if (!song.isOwnedBy(actorId)) record(song, "FIRST_REACTION", "reaction:" + song.getId(), null);
    }

    private void record(PlaylistSong song, String type, String key, UUID commentId) {
        // 이력은 구독 여부와 무관하게 기록해 나중에 재반응하더라도 최초 이벤트를 재발송하지 않는다.
        jdbc.update("""
                INSERT INTO playlist_notifications (id, event_key, song_id, comment_id, recipient_id, type, status)
                VALUES (?, ?, ?, ?, ?, ?, CASE WHEN EXISTS (
                    SELECT 1 FROM subscriptions WHERE device_id = ? AND topic = 'PLAYLIST_ACTIVITY'
                    AND is_active AND COALESCE(params -> ?, 'true'::jsonb) = 'true'::jsonb
                ) THEN 'PENDING' ELSE 'SKIPPED' END)
                ON CONFLICT (event_key) DO NOTHING
                """, UUID.randomUUID(), key, song.getId(), commentId, song.getDeviceId(), type,
                song.getDeviceId(), type.equals("COMMENT") ? "comments" : "firstReaction");
    }

    public void cancelComment(UUID commentId) {
        jdbc.update("UPDATE playlist_notifications SET status = 'SKIPPED' WHERE comment_id = ? AND status = 'PENDING'", commentId);
    }
}
