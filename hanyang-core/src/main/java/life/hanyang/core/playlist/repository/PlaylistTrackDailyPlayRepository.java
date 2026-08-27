package life.hanyang.core.playlist.repository;

import life.hanyang.core.playlist.domain.PlaylistTrackDailyPlay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface PlaylistTrackDailyPlayRepository extends JpaRepository<PlaylistTrackDailyPlay, UUID> {

    Optional<PlaylistTrackDailyPlay> findByTrackIdAndPlayDate(String trackId, LocalDate playDate);

    @Modifying
    @Query("UPDATE PlaylistTrackDailyPlay p SET p.playCount = p.playCount + 1 WHERE p.id = :id")
    void incrementPlayCount(@Param("id") UUID id);

    @Modifying
    @Query(value = """
            INSERT INTO playlist_track_daily_plays (id, track_id, play_date, play_count, created_at, updated_at)
            VALUES (gen_random_uuid(), :trackId, :playDate, 1, now(), now())
            ON CONFLICT (track_id, play_date)
            DO UPDATE SET play_count = playlist_track_daily_plays.play_count + 1, updated_at = now()
            """, nativeQuery = true)
    void upsertDailyPlayCount(@Param("trackId") String trackId, @Param("playDate") LocalDate playDate);
}
