-- 운영/개발 DB 대상 확인 후 별도 적용. CONCURRENTLY는 트랜잭션 밖에서 각 문장을 실행한다.
-- 검색 결과는 동일하며 포함 검색용 인덱스만 추가한다. 디스크/쓰기 비용이 증가한다.
CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_playlist_tracks_title_trgm
    ON playlist_tracks USING gin (lower(title) gin_trgm_ops);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_playlist_tracks_artist_trgm
    ON playlist_tracks USING gin (lower(artist) gin_trgm_ops);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_playlist_artists_name_trgm
    ON playlist_artists USING gin (lower(name) gin_trgm_ops);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_playlist_songs_comment_active_trgm
    ON playlist_songs USING gin (lower(comment) gin_trgm_ops)
    WHERE deleted_at IS NULL;
