package life.hanyang.core.playlist.repository;

import life.hanyang.core.playlist.domain.ChartType;
import life.hanyang.core.playlist.domain.Genre;
import life.hanyang.core.playlist.domain.PlaylistChart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlaylistChartRepository extends JpaRepository<PlaylistChart, UUID> {

    /**
     * 특정 차트 유형의 가장 최신 스냅샷 시각 조회
     */
    @Query("SELECT MAX(c.snapshotTime) FROM PlaylistChart c WHERE c.chartType = :chartType AND (:genre IS NULL AND c.genre IS NULL OR c.genre = :genre)")
    Optional<Instant> findLatestSnapshotTime(@Param("chartType") ChartType chartType, @Param("genre") Genre genre);

    /**
     * 특정 차트 유형 및 스냅샷 시각의 1~100위 랭킹 목록 조회 (Fetch Join으로 Track N+1 방지)
     */
    @Query("SELECT c FROM PlaylistChart c JOIN FETCH c.track WHERE c.chartType = :chartType AND c.snapshotTime = :snapshotTime AND (:genre IS NULL AND c.genre IS NULL OR c.genre = :genre) ORDER BY c.rank ASC")
    List<PlaylistChart> findByChartTypeAndSnapshotTimeOrderByRankAsc(
            @Param("chartType") ChartType chartType,
            @Param("snapshotTime") Instant snapshotTime,
            @Param("genre") Genre genre
    );

    /**
     * 특정 차트 유형의 가장 최신 랭킹 목록 조회
     */
    @Query("""
            SELECT c FROM PlaylistChart c JOIN FETCH c.track 
            WHERE c.chartType = :chartType
              AND (:genre IS NULL AND c.genre IS NULL OR c.genre = :genre)
              AND c.snapshotTime = (SELECT MAX(c2.snapshotTime) FROM PlaylistChart c2 WHERE c2.chartType = :chartType AND (:genre IS NULL AND c2.genre IS NULL OR c2.genre = :genre))
            ORDER BY c.rank ASC
            """)
    List<PlaylistChart> findLatestChartByChartTypeAndGenre(@Param("chartType") ChartType chartType, @Param("genre") Genre genre);

    @Modifying(flushAutomatically = true)
    @Query(value = "LOCK TABLE playlist_charts IN SHARE ROW EXCLUSIVE MODE", nativeQuery = true)
    void lockChartSnapshots();

    @Modifying(flushAutomatically = true)
    @Query("DELETE FROM PlaylistChart c WHERE c.chartType = :chartType AND c.snapshotTime = :snapshotTime")
    void deleteByChartTypeAndSnapshotTime(@Param("chartType") ChartType chartType, @Param("snapshotTime") Instant snapshotTime);
}
