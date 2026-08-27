package life.hanyang.core.playlist.repository;

import life.hanyang.core.playlist.domain.PlaylistSongReport;
import life.hanyang.core.playlist.domain.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PlaylistSongReportRepository extends JpaRepository<PlaylistSongReport, UUID> {

    @EntityGraph(attributePaths = {"song", "song.track"})
    Page<PlaylistSongReport> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @EntityGraph(attributePaths = {"song", "song.track"})
    Page<PlaylistSongReport> findAllByStatusOrderByCreatedAtDesc(ReportStatus status, Pageable pageable);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE PlaylistSongReport r " +
            "SET r.status = :newStatus, r.adminMemo = :memo, r.reviewedAt = :reviewedAt " +
            "WHERE r.song.id = :songId AND r.status = :pendingStatus")
    void resolvePendingReportsBySongId(
            @org.springframework.data.repository.query.Param("songId") UUID songId,
            @org.springframework.data.repository.query.Param("newStatus") ReportStatus newStatus,
            @org.springframework.data.repository.query.Param("memo") String memo,
            @org.springframework.data.repository.query.Param("reviewedAt") java.time.Instant reviewedAt,
            @org.springframework.data.repository.query.Param("pendingStatus") ReportStatus pendingStatus
    );
}
