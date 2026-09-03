BEGIN;

ALTER TABLE playlist_tracks
    ADD COLUMN IF NOT EXISTS like_count INTEGER NOT NULL DEFAULT 0;

CREATE TABLE IF NOT EXISTS playlist_track_likes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    track_id VARCHAR NOT NULL REFERENCES playlist_tracks(track_id),
    device_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_playlist_track_likes_track_device UNIQUE (track_id, device_id)
);

-- UNIQUE(track_id, device_id)는 track_id 선행 인덱스이므로 곡별 존재 확인·집계에도 사용된다.
CREATE INDEX IF NOT EXISTS idx_playlist_track_likes_device_created
    ON playlist_track_likes (device_id, created_at DESC);

-- 기존 게시물 좋아요를 곡 단위로 보존한다. 삭제된 게시물은 이관하지 않는다.
-- 같은 기기가 같은 곡의 여러 게시물에 누른 좋아요는 하나로 합친다.
INSERT INTO playlist_track_likes (track_id, device_id, created_at)
SELECT s.track_id, l.device_id, MIN(l.created_at)
FROM playlist_song_likes l
JOIN playlist_songs s ON s.id = l.song_id
WHERE s.deleted_at IS NULL
GROUP BY s.track_id, l.device_id
ON CONFLICT (track_id, device_id) DO NOTHING;

-- 화면 조회는 COUNT 대신 이 카운터를 사용한다.
UPDATE playlist_tracks SET like_count = 0;
UPDATE playlist_tracks t
SET like_count = counts.like_count
FROM (
    SELECT track_id, COUNT(*)::INTEGER AS like_count
    FROM playlist_track_likes
    GROUP BY track_id
) counts
WHERE t.track_id = counts.track_id;

COMMIT;
