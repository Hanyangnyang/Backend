package life.hanyang.core.playlist.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientResponseException;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlaylistNotificationDispatcher {
    private final JdbcTemplate jdbc;
    private final PlaylistFcmSender sender;
    private final ObjectMapper mapper;

    @Transactional
    public boolean dispatchNext() {
        if (!sender.configured()) return false;
        var rows = jdbc.queryForList("""
                SELECT * FROM playlist_notifications
                WHERE status = 'PENDING' AND next_attempt_at <= now()
                ORDER BY next_attempt_at, id LIMIT 1 FOR UPDATE SKIP LOCKED
                """);
        if (rows.isEmpty()) return false;
        var event = rows.get(0);
        UUID id = (UUID) event.get("id");
        String type = (String) event.get("type");
        // 구독 해제, 글/댓글 삭제는 발송 직전에도 확인한다.
        var targets = jdbc.queryForList("""
                SELECT d.fcm_token, d.platform, s.track_id FROM playlist_songs s
                JOIN subscriptions sub ON sub.device_id = s.device_id AND sub.topic = 'PLAYLIST_ACTIVITY'
                JOIN devices d ON d.id = sub.device_id
                WHERE s.id = ? AND s.deleted_at IS NULL AND sub.is_active
                AND COALESCE(sub.params -> ?, 'true'::jsonb) = 'true'::jsonb
                AND (?::uuid IS NULL OR EXISTS (
                    SELECT 1 FROM playlist_comments c WHERE c.id = ? AND c.deleted_at IS NULL))
                """, event.get("song_id"), type.equals("COMMENT") ? "comments" : "firstReaction",
                event.get("comment_id"), event.get("comment_id"));
        if (targets.isEmpty()) {
            jdbc.update("UPDATE playlist_notifications SET status = 'SKIPPED' WHERE id = ?", id);
            return true;
        }
        var target = targets.get(0);
        try {
            sender.send(id, (String) target.get("fcm_token"), (String) target.get("platform"), type,
                    (UUID) event.get("song_id"), (String) target.get("track_id"));
            jdbc.update("UPDATE playlist_notifications SET status = 'SENT', sent_at = now(), attempts = attempts + 1, last_error = NULL WHERE id = ?", id);
        } catch (Exception e) {
            int attempts = ((Number) event.get("attempts")).intValue() + 1;
            String error = e.getClass().getSimpleName();
            boolean terminal = false;
            if (e instanceof RestClientResponseException response) {
                error = "HTTP_" + response.getStatusCode().value();
                try {
                    JsonNode details = mapper.readTree(response.getResponseBodyAsString()).path("error").path("details");
                    for (JsonNode detail : details) {
                        if ("UNREGISTERED".equals(detail.path("errorCode").asText())) terminal = true;
                    }
                } catch (Exception ignored) { /* 오류 본문을 로그에 출력하지 않는다. */ }
            }
            jdbc.update("""
                    UPDATE playlist_notifications SET attempts = ?, status = ?, last_error = ?,
                    next_attempt_at = now() + (? * interval '1 second') WHERE id = ?
                    """, attempts, terminal || attempts >= 5 ? "FAILED" : "PENDING", error,
                    Math.min(3600, 30 * (1 << attempts)), id);
            log.warn("플레이리스트 알림 발송 실패. notificationId={}, attempts={}, error={}", id, attempts, error);
        }
        // FCM 성공 후 DB 커밋 전 프로세스가 종료되면 재발송 가능: at-least-once 전달이다.
        return true;
    }
}
