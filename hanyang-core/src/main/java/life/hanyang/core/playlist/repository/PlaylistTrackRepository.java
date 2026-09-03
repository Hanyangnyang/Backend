package life.hanyang.core.playlist.repository;

import life.hanyang.core.playlist.domain.PlaylistTrack;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PlaylistTrackRepository extends JpaRepository<PlaylistTrack, String>, PlaylistTrackRepositoryCustom {

    @Modifying
    @Query("UPDATE PlaylistTrack t SET t.likeCount = t.likeCount + 1 WHERE t.trackId = :trackId")
    void incrementLikeCount(@Param("trackId") String trackId);

    @Modifying
    @Query("UPDATE PlaylistTrack t SET t.likeCount = CASE WHEN t.likeCount > 0 THEN t.likeCount - 1 ELSE 0 END WHERE t.trackId = :trackId")
    void decrementLikeCount(@Param("trackId") String trackId);

    @Query("SELECT t.likeCount FROM PlaylistTrack t WHERE t.trackId = :trackId")
    Optional<Integer> getLikeCount(@Param("trackId") String trackId);
}
