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

    Optional<PlaylistSongLike> findBySongIdAndUserId(UUID songId, UUID userId);

    boolean existsBySongIdAndUserId(UUID songId, UUID userId);

    @Query("SELECT l.song.id FROM PlaylistSongLike l WHERE l.userId = :userId AND l.song.id IN :songIds")
    Set<UUID> findLikedSongIdsByUserIdAndSongIdIn(@Param("userId") UUID userId, @Param("songIds") Collection<UUID> songIds);

    @Query("SELECT l.song FROM PlaylistSongLike l WHERE l.userId = :userId AND l.song.deletedAt IS NULL ORDER BY l.createdAt DESC")
    Page<PlaylistSong> findLikedSongsByUserId(@Param("userId") UUID userId, Pageable pageable);
}
