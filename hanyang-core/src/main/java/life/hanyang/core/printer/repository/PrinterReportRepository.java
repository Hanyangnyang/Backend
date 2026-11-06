package life.hanyang.core.printer.repository;

import jakarta.persistence.LockModeType;
import life.hanyang.core.printer.domain.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.UUID;

public interface PrinterReportRepository extends JpaRepository<PrinterReport, UUID> {
    @EntityGraph(attributePaths = {"printer", "printer.building"})
    @Query(value = """
            select r from PrinterReport r
            where (cast(:status as string) is null or r.status = :status) and (cast(:type as string) is null or r.type = :type)
            """, countQuery = """
            select count(r) from PrinterReport r
            where (cast(:status as string) is null or r.status = :status) and (cast(:type as string) is null or r.type = :type)
            """)
    Page<PrinterReport> findReports(@Param("status") PrinterReportStatus status,
                                   @Param("type") PrinterReportType type, Pageable pageable);

    @EntityGraph(attributePaths = {"printer", "printer.building"})
    @Query("select r from PrinterReport r where r.id = :id")
    Optional<PrinterReport> findDetail(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from PrinterReport r where r.id = :id")
    Optional<PrinterReport> findForProcessing(@Param("id") UUID id);
}
