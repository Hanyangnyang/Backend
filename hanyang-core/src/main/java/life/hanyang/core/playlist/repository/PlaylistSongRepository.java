package life.hanyang.core.playlist.repository;

import life.hanyang.core.playlist.domain.PlaylistSong;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface PlaylistSongRepository extends JpaRepository<PlaylistSong, UUID>, PlaylistSongRepositoryCustom {

    Optional<PlaylistSong> findByIdAndDeletedAtIsNull(UUID id);

    boolean existsByIdAndDeletedAtIsNull(UUID id);

    @Modifying
    @Query("UPDATE PlaylistSong s SET s.heartCount = s.heartCount + 1 WHERE s.id = :id")
    void incrementHeartCount(@Param("id") UUID id);

    @Modifying
    @Query("UPDATE PlaylistSong s SET s.heartCount = CASE WHEN s.heartCount > 0 THEN s.heartCount - 1 ELSE 0 END WHERE s.id = :id")
    void decrementHeartCount(@Param("id") UUID id);

    @Query("SELECT s.heartCount FROM PlaylistSong s WHERE s.id = :id")
    Optional<Integer> getHeartCount(@Param("id") UUID id);
}
