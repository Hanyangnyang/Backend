package life.hanyang.core.playlist.repository;

import life.hanyang.core.playlist.domain.PlaylistTrackHourlyPlay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlaylistTrackHourlyPlayRepository extends JpaRepository<PlaylistTrackHourlyPlay, UUID> {

    Optional<PlaylistTrackHourlyPlay> findByTrackIdAndPlayHour(String trackId, Instant playHour);

    @Modifying
    @Query(value = """
            INSERT INTO playlist_track_hourly_plays (id, track_id, play_hour, play_count, created_at, updated_at)
            VALUES (gen_random_uuid(), :trackId, :playHour, 1, now(), now())
            ON CONFLICT (track_id, play_hour)
            DO UPDATE SET play_count = playlist_track_hourly_plays.play_count + 1, updated_at = now()
            """, nativeQuery = true)
    void upsertHourlyPlayCount(@Param("trackId") String trackId, @Param("playHour") Instant playHour);

    /**
     * 🔥 실시간 급상승 차트 집계 (최근 24시간 베이스 풀 + 직전 3시간 부스터)
     */
    @Query(value = """
            WITH hourly_plays AS (
                SELECT 
                    track_id,
                    SUM(CASE WHEN play_hour >= :h24 AND play_hour < :upperBound THEN play_count ELSE 0 END) AS play_24h,
                    SUM(CASE WHEN play_hour >= :h3 AND play_hour < :upperBound THEN play_count ELSE 0 END) AS play_3h
                FROM playlist_track_hourly_plays
                WHERE play_hour >= :h24 AND play_hour < :upperBound
                GROUP BY track_id
            ),
            song_stats AS (
                SELECT 
                    s.track_id,
                    COUNT(s.id) AS total_posts,
                    COUNT(CASE WHEN s.created_at >= :h24 AND s.created_at < :upperBound THEN 1 END) AS posts_24h,
                    COUNT(CASE WHEN s.created_at >= :h3 AND s.created_at < :upperBound THEN 1 END) AS posts_3h
                FROM playlist_songs s
                WHERE s.deleted_at IS NULL
                GROUP BY s.track_id
            ),
            like_stats AS (
                SELECT 
                    s.track_id,
                    COUNT(CASE WHEN l.created_at >= :h24 AND l.created_at < :upperBound THEN 1 END) AS likes_24h,
                    COUNT(CASE WHEN l.created_at >= :h3 AND l.created_at < :upperBound THEN 1 END) AS likes_3h
                FROM playlist_song_likes l
                JOIN playlist_songs s ON l.song_id = s.id
                WHERE s.deleted_at IS NULL AND l.created_at >= :h24 AND l.created_at < :upperBound
                GROUP BY s.track_id
            )
            SELECT 
                t.track_id,
                t.title,
                t.artist,
                t.album_art_url,
                (
                    COALESCE(ls.likes_24h, 0) * 3
                  + COALESCE(hp.play_24h, 0) * 1
                  + COALESCE(ss.posts_24h, 0) * 1
                  + COALESCE(ls.likes_3h, 0) * 5
                  + COALESCE(hp.play_3h, 0) * 2
                  + COALESCE(ss.posts_3h, 0) * 1
                ) AS total_score
            FROM playlist_tracks t
            LEFT JOIN hourly_plays hp ON t.track_id = hp.track_id
            LEFT JOIN song_stats ss ON t.track_id = ss.track_id
            LEFT JOIN like_stats ls ON t.track_id = ls.track_id
            WHERE COALESCE(ss.total_posts, 0) > 0
              AND (
                    COALESCE(ls.likes_24h, 0) * 3
                  + COALESCE(hp.play_24h, 0) * 1
                  + COALESCE(ss.posts_24h, 0) * 1
                  + COALESCE(ls.likes_3h, 0) * 5
                  + COALESCE(hp.play_3h, 0) * 2
                  + COALESCE(ss.posts_3h, 0) * 1
              ) > 0
            ORDER BY total_score DESC, ls.likes_24h DESC NULLS LAST, hp.play_24h DESC NULLS LAST, t.created_at DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Object[]> findRisingChartRaw(
            @Param("h24") Instant h24,
            @Param("h3") Instant h3,
            @Param("upperBound") Instant upperBound,
            @Param("limit") int limit
    );

    /**
     * 📅 주간 인기 차트 집계 (완결된 지난주 월~일)
     */
    @Query(value = """
            WITH weekly_plays AS (
                SELECT 
                    track_id,
                    SUM(play_count) AS play_7d
                FROM playlist_track_hourly_plays
                WHERE play_hour >= :startPeriod AND play_hour < :endPeriod
                GROUP BY track_id
            ),
            song_stats AS (
                SELECT 
                    s.track_id,
                    COUNT(s.id) AS total_posts,
                    COUNT(CASE WHEN s.created_at >= :startPeriod AND s.created_at < :endPeriod THEN 1 END) AS posts_7d
                FROM playlist_songs s
                WHERE s.deleted_at IS NULL
                GROUP BY s.track_id
            ),
            like_stats AS (
                SELECT 
                    s.track_id,
                    COUNT(l.id) AS likes_7d
                FROM playlist_song_likes l
                JOIN playlist_songs s ON l.song_id = s.id
                WHERE s.deleted_at IS NULL AND l.created_at >= :startPeriod AND l.created_at < :endPeriod
                GROUP BY s.track_id
            )
            SELECT 
                t.track_id,
                t.title,
                t.artist,
                t.album_art_url,
                (
                    COALESCE(ls.likes_7d, 0) * 5
                  + COALESCE(wp.play_7d, 0) * 1
                  + COALESCE(ss.posts_7d, 0) * 1
                ) AS total_score
            FROM playlist_tracks t
            LEFT JOIN weekly_plays wp ON t.track_id = wp.track_id
            LEFT JOIN song_stats ss ON t.track_id = ss.track_id
            LEFT JOIN like_stats ls ON t.track_id = ls.track_id
            WHERE COALESCE(ss.total_posts, 0) > 0
              AND (
                    COALESCE(ls.likes_7d, 0) * 5
                  + COALESCE(wp.play_7d, 0) * 1
                  + COALESCE(ss.posts_7d, 0) * 1
              ) > 0
            ORDER BY total_score DESC, ls.likes_7d DESC NULLS LAST, wp.play_7d DESC NULLS LAST, t.created_at DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Object[]> findWeeklyChartRaw(
            @Param("startPeriod") Instant startPeriod,
            @Param("endPeriod") Instant endPeriod,
            @Param("limit") int limit
    );

    /**
     * 🏆 월간 인기 차트 집계 (완결된 지난달 1일~말일)
     */
    @Query(value = """
            WITH monthly_plays AS (
                SELECT 
                    track_id,
                    SUM(play_count) AS play_30d
                FROM playlist_track_hourly_plays
                WHERE play_hour >= :startPeriod AND play_hour < :endPeriod
                GROUP BY track_id
            ),
            song_stats AS (
                SELECT 
                    s.track_id,
                    COUNT(s.id) AS total_posts,
                    COUNT(CASE WHEN s.created_at >= :startPeriod AND s.created_at < :endPeriod THEN 1 END) AS posts_30d
                FROM playlist_songs s
                WHERE s.deleted_at IS NULL
                GROUP BY s.track_id
            ),
            like_stats AS (
                SELECT 
                    s.track_id,
                    COUNT(l.id) AS likes_30d
                FROM playlist_song_likes l
                JOIN playlist_songs s ON l.song_id = s.id
                WHERE s.deleted_at IS NULL AND l.created_at >= :startPeriod AND l.created_at < :endPeriod
                GROUP BY s.track_id
            )
            SELECT 
                t.track_id,
                t.title,
                t.artist,
                t.album_art_url,
                (
                    COALESCE(ls.likes_30d, 0) * 5
                  + COALESCE(mp.play_30d, 0) * 1
                  + COALESCE(ss.posts_30d, 0) * 2
                ) AS total_score
            FROM playlist_tracks t
            LEFT JOIN monthly_plays mp ON t.track_id = mp.track_id
            LEFT JOIN song_stats ss ON t.track_id = ss.track_id
            LEFT JOIN like_stats ls ON t.track_id = ls.track_id
            WHERE COALESCE(ss.total_posts, 0) > 0
              AND (
                    COALESCE(ls.likes_30d, 0) * 5
                  + COALESCE(mp.play_30d, 0) * 1
                  + COALESCE(ss.posts_30d, 0) * 2
              ) > 0
            ORDER BY total_score DESC, ls.likes_30d DESC NULLS LAST, mp.play_30d DESC NULLS LAST, t.created_at DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Object[]> findMonthlyChartRaw(
            @Param("startPeriod") Instant startPeriod,
            @Param("endPeriod") Instant endPeriod,
            @Param("limit") int limit
    );
}
