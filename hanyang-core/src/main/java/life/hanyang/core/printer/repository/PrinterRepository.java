package life.hanyang.core.printer.repository;

import life.hanyang.core.campusmap.domain.Campus;
import life.hanyang.core.printer.domain.Printer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.UUID;

public interface PrinterRepository extends JpaRepository<Printer, UUID> {
    @EntityGraph(attributePaths = "building")
    @Query(value = """
            select p from Printer p where p.deleted = false and (cast(:removed as boolean) is null or p.removed = :removed)
              and (cast(:campus as string) is null or p.building.campus = :campus)
              and (cast(:buildingId as string) is null or p.building.id = :buildingId)
            order by p.building.name asc, coalesce(p.name, p.detailLocation) asc,
                     p.detailLocation asc, p.id asc
            """, countQuery = """
            select count(p) from Printer p where p.deleted = false and (cast(:removed as boolean) is null or p.removed = :removed)
              and (cast(:campus as string) is null or p.building.campus = :campus)
              and (cast(:buildingId as string) is null or p.building.id = :buildingId)
            """)
    Page<Printer> findForListing(@Param("campus") Campus campus, @Param("buildingId") String buildingId,
                                 @Param("removed") Boolean removed, Pageable pageable);

    @EntityGraph(attributePaths = "building")
    @Query("select p from Printer p where p.id = :id and p.deleted = false")
    Optional<Printer> findActive(@Param("id") UUID id);
}
