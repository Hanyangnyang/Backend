package life.hanyang.core.playlist.repository;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.hibernate.query.NativeQuery;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class PlaylistRecommendationRepository {
    private final EntityManager entityManager;

    public record Signal(String trackId, double playScore, double preferenceScore) {
        public double score() { return playScore + preferenceScore; }
    }
    public record TrackArtist(String trackId, UUID artistId) { }
    public record TrackGenre(String trackId, String genre) { }
    public record Candidate(UUID artistId, String trackId) { }

    /** Bound each history before combining it; repeat plays count only once per Korean day. */
    public List<Signal> findSignals(UUID deviceId, Instant since, Instant boostedSince,
                                    LocalDate sinceDate, LocalDate boostedDate) {
        NativeQuery<?> query = entityManager.createNativeQuery("""
                WITH plays AS (
                    SELECT track_id, play_date FROM playlist_track_daily_devices
                    WHERE device_id = :deviceId AND play_date >= :sinceDate
                    ORDER BY play_date DESC, track_id LIMIT 100
                ), likes AS (
                    SELECT track_id, created_at FROM playlist_track_likes
                    WHERE device_id = :deviceId AND created_at >= :since
                    ORDER BY created_at DESC, track_id LIMIT 100
                ), posts AS (
                    SELECT track_id, created_at FROM playlist_songs
                    WHERE device_id = :deviceId AND created_at >= :since AND deleted_at IS NULL
                    ORDER BY created_at DESC, id DESC LIMIT 100
                ), signals AS (
                    SELECT track_id, CASE WHEN play_date >= :boostedDate THEN 2 ELSE 1 END AS play_score, 0 AS preference_score FROM plays
                    UNION ALL
                    SELECT track_id, 0, CASE WHEN created_at >= :boostedSince THEN 6 ELSE 3 END FROM likes
                    UNION ALL
                    SELECT track_id, 0, CASE WHEN created_at >= :boostedSince THEN 6 ELSE 3 END FROM posts
                )
                SELECT track_id, CAST(sum(play_score) AS double precision) AS play_score,
                       CAST(sum(preference_score) AS double precision) AS preference_score
                FROM signals GROUP BY track_id
                """).unwrap(NativeQuery.class).addScalar("track_id", String.class).addScalar("play_score", Double.class).addScalar("preference_score", Double.class);
        query.setParameter("deviceId", deviceId).setParameter("since", since).setParameter("boostedSince", boostedSince)
                .setParameter("sinceDate", sinceDate).setParameter("boostedDate", boostedDate);
        return rows(query).stream().map(row -> new Signal((String) row[0], ((Number) row[1]).doubleValue(), ((Number) row[2]).doubleValue())).toList();
    }

    public List<TrackArtist> findArtists(List<String> trackIds) {
        if (trackIds.isEmpty()) return List.of();
        NativeQuery<?> query = entityManager.createNativeQuery("""
                SELECT track_id, artist_id FROM playlist_track_artists WHERE track_id IN (:trackIds)
                """).unwrap(NativeQuery.class).addScalar("track_id", String.class).addScalar("artist_id", UUID.class);
        query.setParameter("trackIds", trackIds);
        return rows(query).stream().map(row -> new TrackArtist((String) row[0], (UUID) row[1])).toList();
    }

    public List<TrackGenre> findGenres(List<String> trackIds) {
        if (trackIds.isEmpty()) return List.of();
        NativeQuery<?> query = entityManager.createNativeQuery("""
                SELECT DISTINCT s.track_id, g.genre FROM playlist_songs s
                JOIN playlist_song_genres g ON g.song_id = s.id
                WHERE s.deleted_at IS NULL AND s.track_id IN (:trackIds)
                """).unwrap(NativeQuery.class).addScalar("track_id", String.class).addScalar("genre", String.class);
        query.setParameter("trackIds", trackIds);
        return rows(query).stream().map(row -> new TrackGenre((String) row[0], (String) row[1])).toList();
    }

    /** At most five alternative tracks per artist, so shared collaboration tracks can be skipped. */
    public List<Candidate> findInterestCandidates(List<UUID> artistIds) {
        if (artistIds.isEmpty()) return List.of();
        NativeQuery<?> query = entityManager.createNativeQuery("""
                WITH ranked AS (
                    SELECT ta.artist_id, ta.track_id,
                           row_number() OVER (PARTITION BY ta.artist_id ORDER BY t.created_at DESC, ta.track_id) AS position
                    FROM playlist_track_artists ta JOIN playlist_tracks t ON t.track_id = ta.track_id
                    WHERE ta.artist_id IN (:artistIds)
                )
                SELECT artist_id, track_id FROM ranked WHERE position <= 5 ORDER BY artist_id, position
                """).unwrap(NativeQuery.class).addScalar("artist_id", UUID.class).addScalar("track_id", String.class);
        query.setParameter("artistIds", artistIds);
        return candidates(query);
    }

    /** Sample distinct artists, not posts: prolific posters never give an artist extra lottery entries. */
    public List<Candidate> findDiscoveryCandidates(UUID deviceId, List<String> genres, Set<UUID> seenArtists) {
        if (genres.isEmpty()) return List.of();
        String excludeSeen = seenArtists.isEmpty() ? "" : " AND ta.artist_id NOT IN (:seenArtists) ";
        NativeQuery<?> query = entityManager.createNativeQuery("""
                WITH eligible AS (
                    SELECT DISTINCT ta.artist_id, ta.track_id
                    FROM playlist_song_genres g JOIN playlist_songs s ON s.id = g.song_id
                    JOIN playlist_track_artists ta ON ta.track_id = s.track_id
                    WHERE g.genre IN (:genres) AND s.deleted_at IS NULL AND s.device_id <> :deviceId
                """ + excludeSeen + """
                ), sampled_artists AS (
                    SELECT artist_id FROM eligible GROUP BY artist_id ORDER BY random() LIMIT 50
                ), ranked AS (
                    SELECT e.artist_id, e.track_id,
                           row_number() OVER (PARTITION BY e.artist_id ORDER BY t.created_at DESC, e.track_id) AS position
                    FROM eligible e JOIN sampled_artists a ON a.artist_id = e.artist_id
                    JOIN playlist_tracks t ON t.track_id = e.track_id
                )
                SELECT artist_id, track_id FROM ranked WHERE position <= 5 ORDER BY artist_id, position
                """).unwrap(NativeQuery.class).addScalar("artist_id", UUID.class).addScalar("track_id", String.class);
        query.setParameter("genres", genres).setParameter("deviceId", deviceId);
        if (!seenArtists.isEmpty()) query.setParameter("seenArtists", seenArtists);
        return candidates(query);
    }

    private List<Candidate> candidates(NativeQuery<?> query) {
        return rows(query).stream().map(row -> new Candidate((UUID) row[0], (String) row[1])).toList();
    }

    @SuppressWarnings("unchecked")
    private List<Object[]> rows(NativeQuery<?> query) {
        return (List<Object[]>) (List<?>) query.getResultList();
    }
}
