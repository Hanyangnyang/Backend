package life.hanyang.core.playlist.repository;

import life.hanyang.core.playlist.domain.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.UUID;
import java.util.Optional;

public interface PlaylistCommentReportRepository extends JpaRepository<PlaylistCommentReport, UUID> {
    @Query("select r.comment.songId from PlaylistCommentReport r where r.id = :id")
    Optional<UUID> findSongIdByReportId(@Param("id") UUID id);
    boolean existsByCommentIdAndReporterDeviceId(UUID commentId, UUID reporterDeviceId);
    @EntityGraph(attributePaths = "comment")
    Page<PlaylistCommentReport> findAllByOrderByCreatedAtDescIdDesc(Pageable pageable);
    @EntityGraph(attributePaths = "comment")
    Page<PlaylistCommentReport> findAllByStatusOrderByCreatedAtDescIdDesc(ReportStatus status, Pageable pageable);
    @Modifying
    @Query("update PlaylistCommentReport r set r.status = :status, r.adminMemo = :memo, "
            + "r.reviewedAt = :now, r.updatedAt = :now where r.comment.id = :commentId and r.status = :pending")
    void resolvePendingByCommentId(@Param("commentId") UUID commentId, @Param("status") ReportStatus status,
            @Param("memo") String memo, @Param("now") Instant now, @Param("pending") ReportStatus pending);
}
