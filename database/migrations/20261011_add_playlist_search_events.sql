-- 개발 DB에서 확인 후 운영 DB에 적용. 실행은 별도 승인 필요.
CREATE TABLE playlist_search_events (
    id UUID PRIMARY KEY,
    searched_at TIMESTAMPTZ NOT NULL,
    source VARCHAR(16) NOT NULL CHECK (source IN ('SONGS', 'CATALOG')),
    keyword VARCHAR(200) NOT NULL,
    result_count BIGINT NOT NULL CHECK (result_count >= 0),
    outcome VARCHAR(16) NOT NULL CHECK (outcome IN ('SUCCESS', 'FAILURE')),
    error_code VARCHAR(32),
    duration_ms BIGINT NOT NULL CHECK (duration_ms >= 0)
);
CREATE INDEX idx_playlist_search_events_time ON playlist_search_events (searched_at DESC);
ALTER TABLE playlist_search_events ENABLE ROW LEVEL SECURITY;
-- anon/authenticated 공개 정책은 추가하지 않음. 백엔드 계정 권한은 적용 전에 확인.
