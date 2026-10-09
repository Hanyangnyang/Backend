-- 개발/운영 DB 대상 확인과 승인 후 별도 적용. 데이터 변경 없이 기기별 최근 재생 조회를 지원한다.
-- CONCURRENTLY는 Auto-commit 상태에서 트랜잭션 밖에 실행한다.
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_playlist_daily_devices_device_date
    ON playlist_track_daily_devices (device_id, play_date DESC, track_id);
