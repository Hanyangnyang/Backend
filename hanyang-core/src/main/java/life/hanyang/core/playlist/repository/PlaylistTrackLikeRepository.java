package life.hanyang.core.playlist.repository;

import life.hanyang.core.playlist.domain.PlaylistTrack;
import life.hanyang.core.playlist.domain.PlaylistTrackLike;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;

public interface PlaylistTrackLikeRepository extends JpaRepository<PlaylistTrackLike, UUID> {

    @Modifying
    @Query(value = """
            INSERT INTO playlist_track_likes (id, track_id, device_id, created_at)
            VALUES (gen_random_uuid(), :trackId, :deviceId, CURRENT_TIMESTAMP)
            ON CONFLICT (track_id, device_id) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("trackId") String trackId, @Param("deviceId") UUID deviceId);

    @Modifying
    @Query(value = """
            DELETE FROM playlist_track_likes
            WHERE track_id = :trackId AND device_id = :deviceId
            """, nativeQuery = true)
    int deleteIfPresent(@Param("trackId") String trackId, @Param("deviceId") UUID deviceId);

    Optional<PlaylistTrackLike> findByTrackTrackIdAndDeviceId(String trackId, UUID deviceId);

    boolean existsByTrackTrackIdAndDeviceId(String trackId, UUID deviceId);

    @Query("SELECT l.track.trackId FROM PlaylistTrackLike l WHERE l.deviceId = :deviceId AND l.track.trackId IN :trackIds")
    Set<String> findLikedTrackIds(@Param("deviceId") UUID deviceId, @Param("trackIds") Collection<String> trackIds);

    @Query("SELECT l.track FROM PlaylistTrackLike l WHERE l.deviceId = :deviceId ORDER BY l.createdAt DESC")
    Page<PlaylistTrack> findLikedTracksByDeviceId(@Param("deviceId") UUID deviceId, Pageable pageable);
}
