-- playlist_comments 생성 후 적용. 실제 DB 실행은 별도 승인 필요.
CREATE TABLE playlist_comment_reports (
    id UUID PRIMARY KEY,
    comment_id UUID NOT NULL REFERENCES playlist_comments(id),
    reporter_device_id UUID NOT NULL,
    reason VARCHAR(500) NOT NULL,
    content_snapshot VARCHAR(1000) NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'REVIEWED', 'DISMISSED')),
    admin_memo VARCHAR(1000),
    reviewed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_playlist_comment_reporter UNIQUE (comment_id, reporter_device_id)
);
CREATE INDEX idx_playlist_comment_reports_status_time ON playlist_comment_reports(status, created_at DESC, id DESC);
CREATE INDEX idx_playlist_comment_reports_time ON playlist_comment_reports(created_at DESC, id DESC);
ALTER TABLE playlist_comment_reports ENABLE ROW LEVEL SECURITY;
-- 공개 RLS 정책은 추가하지 않음. 조회·처리는 백엔드와 관리자 인증 API를 통해 수행.
