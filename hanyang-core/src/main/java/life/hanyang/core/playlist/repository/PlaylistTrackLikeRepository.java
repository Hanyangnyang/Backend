package life.hanyang.core.playlist.repository;

import life.hanyang.core.playlist.domain.PlaylistTrack;
import life.hanyang.core.playlist.domain.PlaylistTrackLike;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface PlaylistTrackLikeRepository extends JpaRepository<PlaylistTrackLike, UUID> {

    Optional<PlaylistTrackLike> findByTrackTrackIdAndDeviceId(String trackId, UUID deviceId);

    boolean existsByTrackTrackIdAndDeviceId(String trackId, UUID deviceId);

    @Query("SELECT l.track FROM PlaylistTrackLike l WHERE l.deviceId = :deviceId ORDER BY l.createdAt DESC")
    Page<PlaylistTrack> findLikedTracksByDeviceId(@Param("deviceId") UUID deviceId, Pageable pageable);
}
