package life.hanyang.core.playlist.repository;

import life.hanyang.core.playlist.domain.PlaylistSongReport;
import life.hanyang.core.playlist.domain.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PlaylistSongReportRepository extends JpaRepository<PlaylistSongReport, UUID> {

    @EntityGraph(attributePaths = {"song"})
    Page<PlaylistSongReport> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @EntityGraph(attributePaths = {"song"})
    Page<PlaylistSongReport> findAllByStatusOrderByCreatedAtDesc(ReportStatus status, Pageable pageable);
}
