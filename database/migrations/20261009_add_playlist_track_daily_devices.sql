-- 곡·기기·한국 날짜별 재생 집계를 최대 1회로 제한한다.
-- 기존 시간별 재생수는 유지하며 적용 이전 기기별 재생 이력은 복원하지 않는다.
CREATE TABLE playlist_track_daily_devices (
    track_id VARCHAR(255) NOT NULL REFERENCES playlist_tracks(track_id),
    device_id UUID NOT NULL,
    play_date DATE NOT NULL,
    PRIMARY KEY (track_id, device_id, play_date)
);
