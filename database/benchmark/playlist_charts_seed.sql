-- develop DB 전용 차트 인덱스 벤치마크 데이터
-- 실행 전 playlist_tracks에 실제 Spotify 트랙이 최소 100개 있어야 합니다.
-- 이 스크립트는 playlist_tracks의 실제 track_id를 재사용합니다.

BEGIN;

DO $$
DECLARE
    track_count integer;
BEGIN
    SELECT count(*) INTO track_count FROM playlist_tracks;
    IF track_count < 100 THEN
        RAISE EXCEPTION 'playlist_tracks 데이터가 100개 미만입니다: %', track_count;
    END IF;
END $$;

WITH tracks AS (
    SELECT track_id, row_number() OVER (ORDER BY track_id) - 1 AS track_index
    FROM playlist_tracks
), track_total AS (
    SELECT count(*)::bigint AS total FROM tracks
), samples AS (
    SELECT snapshot_no, rank_no
    FROM generate_series(0, 999) AS snapshot(snapshot_no)
    CROSS JOIN generate_series(1, 100) AS rank(rank_no)
)
INSERT INTO playlist_charts (
    id,
    chart_type,
    genre,
    snapshot_time,
    start_period,
    end_period,
    rank,
    track_id,
    total_score,
    created_at
)
SELECT
    gen_random_uuid(),
    'RISING',
    'KPOP',
    TIMESTAMPTZ '2000-01-01 00:00:00+00' + (snapshot_no * INTERVAL '1 hour'),
    TIMESTAMPTZ '1999-12-31 23:00:00+00' + (snapshot_no * INTERVAL '1 hour'),
    TIMESTAMPTZ '2000-01-01 00:00:00+00' + (snapshot_no * INTERVAL '1 hour'),
    rank_no,
    (
        SELECT track_id
        FROM tracks
        WHERE track_index = ((snapshot_no * 100 + rank_no - 1) % track_total.total)
    ),
    100000 - rank_no,
    now()
FROM samples
CROSS JOIN track_total;

COMMIT;

-- 검증
SELECT count(*) AS benchmark_rows
FROM playlist_charts
WHERE chart_type = 'RISING'
  AND genre = 'KPOP'
  AND snapshot_time >= TIMESTAMPTZ '2000-01-01 00:00:00+00'
  AND snapshot_time < TIMESTAMPTZ '2000-02-12 00:00:00+00';

-- 정리 시 아래 조건으로 이번 벤치마크 데이터만 삭제합니다.
-- DELETE FROM playlist_charts
-- WHERE chart_type = 'RISING'
--   AND genre = 'KPOP'
--   AND snapshot_time >= TIMESTAMPTZ '2000-01-01 00:00:00+00'
--   AND snapshot_time < TIMESTAMPTZ '2000-02-12 00:00:00+00';
