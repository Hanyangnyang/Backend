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
     * 최근 24시간 + 최근 6시간 부스트 차트.
     * genre가 null이면 전체, 아니면 해당 장르를 가진 추천글이 있는 트랙만 집계한다.
     * 각 CTE를 트랙 단위로 먼저 축약해 장르/리액션 조인에 따른 카운트 중복을 방지한다.
     */
    @Query(value = """
            WITH hourly_plays AS (
                SELECT 
                    track_id,
                    SUM(play_count) AS play_24h,
                    SUM(CASE WHEN play_hour >= :h6 THEN play_count ELSE 0 END) AS play_6h
                FROM playlist_track_hourly_plays
                WHERE play_hour >= :h24 AND play_hour < :upperBound
                GROUP BY track_id
            ),
            eligible_songs AS (
                SELECT s.id, s.track_id, s.created_at
                FROM playlist_songs s
                WHERE s.deleted_at IS NULL
                  AND (:genre IS NULL OR EXISTS (
                      SELECT 1 FROM playlist_song_genres sg
                      WHERE sg.song_id = s.id AND sg.genre = CAST(:genre AS varchar)
                  ))
            ),
            song_stats AS (
                SELECT 
                    s.track_id,
                    COUNT(s.id) AS total_posts,
                    COUNT(CASE WHEN s.created_at >= :h24 AND s.created_at < :upperBound THEN 1 END) AS posts_24h,
                    COUNT(CASE WHEN s.created_at >= :h6 AND s.created_at < :upperBound THEN 1 END) AS posts_6h
                FROM eligible_songs s
                GROUP BY s.track_id
            ),
            like_stats AS (
                SELECT 
                    l.track_id,
                    COUNT(CASE WHEN l.created_at >= :h24 AND l.created_at < :upperBound THEN 1 END) AS likes_24h,
                    COUNT(CASE WHEN l.created_at >= :h6 AND l.created_at < :upperBound THEN 1 END) AS likes_6h
                FROM playlist_track_likes l
                WHERE l.created_at >= :h24 AND l.created_at < :upperBound
                GROUP BY l.track_id
            ),
            reaction_stats AS (
                SELECT s.track_id, COUNT(DISTINCT r.device_id) AS reactions_24h,
                    COUNT(DISTINCT r.device_id) FILTER (WHERE r.created_at >= :h6) AS reactions_6h
                FROM playlist_song_reactions r
                JOIN eligible_songs s ON s.id = r.song_id
                WHERE r.created_at >= :h24 AND r.created_at < :upperBound
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
                  + COALESCE(rs.reactions_24h, 0) * 2
                  + COALESCE(ls.likes_6h, 0) * 1
                  + COALESCE(hp.play_6h, 0) * 1
                  + COALESCE(ss.posts_6h, 0) * 1
                  + COALESCE(rs.reactions_6h, 0) * 1
                ) AS total_score
            FROM playlist_tracks t
            LEFT JOIN hourly_plays hp ON t.track_id = hp.track_id
            LEFT JOIN song_stats ss ON t.track_id = ss.track_id
            LEFT JOIN like_stats ls ON t.track_id = ls.track_id
            LEFT JOIN reaction_stats rs ON t.track_id = rs.track_id
            WHERE COALESCE(ss.total_posts, 0) > 0
              AND (
                    COALESCE(ls.likes_24h, 0) * 3
                  + COALESCE(hp.play_24h, 0) * 1
                  + COALESCE(ss.posts_24h, 0) * 1
                  + COALESCE(rs.reactions_24h, 0) * 2
                  + COALESCE(ls.likes_6h, 0) * 1
                  + COALESCE(hp.play_6h, 0) * 1
                  + COALESCE(ss.posts_6h, 0) * 1
                  + COALESCE(rs.reactions_6h, 0) * 1
              ) > 0
            ORDER BY total_score DESC, ls.likes_24h DESC NULLS LAST, hp.play_24h DESC NULLS LAST, t.created_at DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Object[]> findRisingChartRaw(
            @Param("h24") Instant h24,
            @Param("h6") Instant h6,
            @Param("upperBound") Instant upperBound,
            @Param("genre") String genre,
            @Param("limit") int limit
    );

    /** 주간/월간 공통 점수: 좋아요*5 + 재생*1 + 추천글*1 + 고유 반응 기기*3 */
    @Query(value = """
            WITH weekly_plays AS (
                SELECT 
                    track_id,
                    SUM(play_count) AS play_7d
                FROM playlist_track_hourly_plays
                WHERE play_hour >= :startPeriod AND play_hour < :endPeriod
                GROUP BY track_id
            ),
            eligible_songs AS (
                SELECT s.id, s.track_id, s.created_at
                FROM playlist_songs s
                WHERE s.deleted_at IS NULL
                  AND (:genre IS NULL OR EXISTS (
                      SELECT 1 FROM playlist_song_genres sg
                      WHERE sg.song_id = s.id AND sg.genre = CAST(:genre AS varchar)
                  ))
            ),
            song_stats AS (
                SELECT 
                    s.track_id,
                    COUNT(s.id) AS total_posts,
                    COUNT(CASE WHEN s.created_at >= :startPeriod AND s.created_at < :endPeriod THEN 1 END) AS posts_7d
                FROM eligible_songs s
                GROUP BY s.track_id
            ),
            like_stats AS (
                SELECT 
                    l.track_id,
                    COUNT(l.id) AS likes_7d
                FROM playlist_track_likes l
                WHERE l.created_at >= :startPeriod AND l.created_at < :endPeriod
                GROUP BY l.track_id
            ),
            reaction_stats AS (
                SELECT s.track_id, COUNT(DISTINCT r.device_id) AS reactions_7d
                FROM playlist_song_reactions r
                JOIN eligible_songs s ON s.id = r.song_id
                WHERE r.created_at >= :startPeriod AND r.created_at < :endPeriod
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
                  + COALESCE(rs.reactions_7d, 0) * 3
                ) AS total_score
            FROM playlist_tracks t
            LEFT JOIN weekly_plays wp ON t.track_id = wp.track_id
            LEFT JOIN song_stats ss ON t.track_id = ss.track_id
            LEFT JOIN like_stats ls ON t.track_id = ls.track_id
            LEFT JOIN reaction_stats rs ON t.track_id = rs.track_id
            WHERE COALESCE(ss.total_posts, 0) > 0
              AND (
                    COALESCE(ls.likes_7d, 0) * 5
                  + COALESCE(wp.play_7d, 0) * 1
                  + COALESCE(ss.posts_7d, 0) * 1
                  + COALESCE(rs.reactions_7d, 0) * 3
              ) > 0
            ORDER BY total_score DESC, ls.likes_7d DESC NULLS LAST, wp.play_7d DESC NULLS LAST, t.created_at DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Object[]> findWeeklyChartRaw(
            @Param("startPeriod") Instant startPeriod,
            @Param("endPeriod") Instant endPeriod,
            @Param("genre") String genre,
            @Param("limit") int limit
    );

    /** 월간 인기 차트 집계 (완결된 지난달 1일~말일) */
    @Query(value = """
            WITH monthly_plays AS (
                SELECT 
                    track_id,
                    SUM(play_count) AS play_30d
                FROM playlist_track_hourly_plays
                WHERE play_hour >= :startPeriod AND play_hour < :endPeriod
                GROUP BY track_id
            ),
            eligible_songs AS (
                SELECT s.id, s.track_id, s.created_at
                FROM playlist_songs s
                WHERE s.deleted_at IS NULL
                  AND (:genre IS NULL OR EXISTS (
                      SELECT 1 FROM playlist_song_genres sg
                      WHERE sg.song_id = s.id AND sg.genre = CAST(:genre AS varchar)
                  ))
            ),
            song_stats AS (
                SELECT 
                    s.track_id,
                    COUNT(s.id) AS total_posts,
                    COUNT(CASE WHEN s.created_at >= :startPeriod AND s.created_at < :endPeriod THEN 1 END) AS posts_30d
                FROM eligible_songs s
                GROUP BY s.track_id
            ),
            like_stats AS (
                SELECT 
                    l.track_id,
                    COUNT(l.id) AS likes_30d
                FROM playlist_track_likes l
                WHERE l.created_at >= :startPeriod AND l.created_at < :endPeriod
                GROUP BY l.track_id
            ),
            reaction_stats AS (
                SELECT s.track_id, COUNT(DISTINCT r.device_id) AS reactions_30d
                FROM playlist_song_reactions r
                JOIN eligible_songs s ON s.id = r.song_id
                WHERE r.created_at >= :startPeriod AND r.created_at < :endPeriod
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
                  + COALESCE(ss.posts_30d, 0) * 1
                  + COALESCE(rs.reactions_30d, 0) * 3
                ) AS total_score
            FROM playlist_tracks t
            LEFT JOIN monthly_plays mp ON t.track_id = mp.track_id
            LEFT JOIN song_stats ss ON t.track_id = ss.track_id
            LEFT JOIN like_stats ls ON t.track_id = ls.track_id
            LEFT JOIN reaction_stats rs ON t.track_id = rs.track_id
            WHERE COALESCE(ss.total_posts, 0) > 0
              AND (
                    COALESCE(ls.likes_30d, 0) * 5
                  + COALESCE(mp.play_30d, 0) * 1
                  + COALESCE(ss.posts_30d, 0) * 1
                  + COALESCE(rs.reactions_30d, 0) * 3
              ) > 0
            ORDER BY total_score DESC, ls.likes_30d DESC NULLS LAST, mp.play_30d DESC NULLS LAST, t.created_at DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Object[]> findMonthlyChartRaw(
            @Param("startPeriod") Instant startPeriod,
            @Param("endPeriod") Instant endPeriod,
            @Param("genre") String genre,
            @Param("limit") int limit
    );
}
