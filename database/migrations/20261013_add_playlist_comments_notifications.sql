-- 실행은 대상 DB와 영향 범위를 확인한 후 별도 승인 필요.
BEGIN;
CREATE TABLE playlist_comments (
    id UUID PRIMARY KEY,
    song_id UUID NOT NULL REFERENCES playlist_songs(id),
    device_id UUID NOT NULL,
    anonymous_number INTEGER CHECK (anonymous_number > 0),
    content VARCHAR(1000) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    deleted_at TIMESTAMPTZ
);
CREATE INDEX idx_playlist_comments_song_time ON playlist_comments(song_id, created_at, id);
CREATE INDEX idx_playlist_comments_author ON playlist_comments(song_id, device_id, created_at, id);
CREATE INDEX idx_playlist_comments_device_time ON playlist_comments(device_id, created_at);
ALTER TABLE playlist_comments ENABLE ROW LEVEL SECURITY;

CREATE TABLE playlist_notifications (
    id UUID PRIMARY KEY,
    event_key VARCHAR(100) NOT NULL UNIQUE,
    song_id UUID NOT NULL REFERENCES playlist_songs(id),
    comment_id UUID REFERENCES playlist_comments(id),
    recipient_id UUID NOT NULL,
    type VARCHAR(20) NOT NULL CHECK (type IN ('COMMENT', 'FIRST_REACTION')),
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'SENT', 'SKIPPED', 'FAILED')),
    attempts INTEGER NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    sent_at TIMESTAMPTZ,
    last_error VARCHAR(100)
);
CREATE INDEX idx_playlist_notifications_pending ON playlist_notifications(next_attempt_at)
    WHERE status = 'PENDING';
ALTER TABLE playlist_notifications ENABLE ROW LEVEL SECURITY;

-- 도입 전 다른 사람의 기존 반응은 이력으로 기록해 취소·재반응에도 알림을 보내지 않는다.
INSERT INTO playlist_notifications(id, event_key, song_id, recipient_id, type, status)
SELECT gen_random_uuid(), 'reaction:' || s.id, s.id, s.device_id, 'FIRST_REACTION', 'SKIPPED'
FROM playlist_songs s
WHERE EXISTS (SELECT 1 FROM playlist_song_reactions r WHERE r.song_id = s.id AND r.device_id <> s.device_id);
COMMIT;
