package life.hanyang.core.playlist.repository;

import life.hanyang.core.playlist.domain.PlaylistComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.Instant;
import java.util.Optional;
import java.util.List;
import java.util.UUID;

public interface PlaylistCommentRepository extends JpaRepository<PlaylistComment, UUID> {
    Page<PlaylistComment> findBySongIdAndDeletedAtIsNull(UUID songId, Pageable pageable);
    Optional<PlaylistComment> findFirstBySongIdAndDeviceIdOrderByCreatedAtAscIdAsc(UUID songId, UUID deviceId);
    Optional<PlaylistComment> findByIdAndSongIdAndDeletedAtIsNull(UUID id, UUID songId);
    List<PlaylistComment> findTop20BySongIdAndDeviceIdAndDeletedAtIsNullOrderByCreatedAtDescIdDesc(UUID songId, UUID deviceId);
    Optional<PlaylistComment> findByIdAndSongId(UUID id, UUID songId);
    @Query("select coalesce(max(c.anonymousNumber), 0) from PlaylistComment c where c.songId = :songId")
    int maxAnonymousNumber(UUID songId);
    long countByDeviceIdAndCreatedAtAfter(UUID deviceId, Instant since);
}
