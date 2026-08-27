package life.hanyang.core.playlist.repository;

import life.hanyang.core.playlist.domain.PlaylistSong;
import life.hanyang.core.playlist.domain.PlaylistSongLike;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface PlaylistSongLikeRepository extends JpaRepository<PlaylistSongLike, UUID> {

    Optional<PlaylistSongLike> findBySongIdAndDeviceId(UUID songId, UUID deviceId);

    boolean existsBySongIdAndDeviceId(UUID songId, UUID deviceId);

    @Query("SELECT l.song.id FROM PlaylistSongLike l WHERE l.deviceId = :deviceId AND l.song.id IN :songIds")
    Set<UUID> findLikedSongIdsByDeviceIdAndSongIdIn(@Param("deviceId") UUID deviceId, @Param("songIds") Collection<UUID> songIds);

    @Query("SELECT l.song FROM PlaylistSongLike l WHERE l.deviceId = :deviceId AND l.song.deletedAt IS NULL ORDER BY l.createdAt DESC")
    Page<PlaylistSong> findLikedSongsByDeviceId(@Param("deviceId") UUID deviceId, Pageable pageable);
}
