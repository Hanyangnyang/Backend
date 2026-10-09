package life.hanyang.core.playlist.repository;

import life.hanyang.core.playlist.domain.PlaylistTrack;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;

public interface PlaylistTrackRepository extends JpaRepository<PlaylistTrack, String>, PlaylistTrackRepositoryCustom {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM PlaylistTrack t WHERE t.trackId = :trackId")
    Optional<PlaylistTrack> findByIdForArtistSync(@Param("trackId") String trackId);

    @Query("""
            SELECT t.trackId FROM PlaylistTrack t
            WHERE t.trackId > :afterTrackId
              AND NOT EXISTS (SELECT a.id FROM PlaylistTrackArtist a WHERE a.track = t)
            ORDER BY t.trackId
            """)
    List<String> findTrackIdsWithoutArtists(@Param("afterTrackId") String afterTrackId, Pageable pageable);

    @Modifying
    @Query("UPDATE PlaylistTrack t SET t.likeCount = t.likeCount + 1 WHERE t.trackId = :trackId")
    void incrementLikeCount(@Param("trackId") String trackId);

    @Modifying
    @Query("UPDATE PlaylistTrack t SET t.likeCount = CASE WHEN t.likeCount > 0 THEN t.likeCount - 1 ELSE 0 END WHERE t.trackId = :trackId")
    void decrementLikeCount(@Param("trackId") String trackId);

    @Query("SELECT t.likeCount FROM PlaylistTrack t WHERE t.trackId = :trackId")
    Optional<Integer> getLikeCount(@Param("trackId") String trackId);
}
