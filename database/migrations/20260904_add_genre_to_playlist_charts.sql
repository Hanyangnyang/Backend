BEGIN;

ALTER TABLE playlist_charts
    ADD COLUMN IF NOT EXISTS genre VARCHAR(30);

ALTER TABLE playlist_charts
    DROP CONSTRAINT IF EXISTS uk_playlist_chart_rank;

DROP INDEX IF EXISTS idx_playlist_charts_lookup;

CREATE INDEX IF NOT EXISTS idx_playlist_charts_lookup
    ON playlist_charts (chart_type, genre, snapshot_time DESC, rank);

-- PostgreSQL UNIQUE 제약은 NULL을 서로 다른 값으로 취급한다.
-- 따라서 전체 차트(NULL)와 장르 차트(non-NULL)를 별도 부분 유니크 인덱스로 보호한다.
CREATE UNIQUE INDEX IF NOT EXISTS uk_playlist_chart_overall_rank
    ON playlist_charts (chart_type, snapshot_time, rank)
    WHERE genre IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_playlist_chart_genre_rank
    ON playlist_charts (chart_type, snapshot_time, genre, rank)
    WHERE genre IS NOT NULL;

-- 차트의 기간 조건 및 장르 존재 여부 조회를 위한 복합 인덱스
CREATE INDEX IF NOT EXISTS idx_playlist_track_likes_chart_period
    ON playlist_track_likes (created_at, track_id);

CREATE INDEX IF NOT EXISTS idx_playlist_song_reactions_chart_period
    ON playlist_song_reactions (created_at, song_id, device_id);

CREATE INDEX IF NOT EXISTS idx_playlist_song_genres_chart_lookup
    ON playlist_song_genres (genre, song_id);

COMMIT;
