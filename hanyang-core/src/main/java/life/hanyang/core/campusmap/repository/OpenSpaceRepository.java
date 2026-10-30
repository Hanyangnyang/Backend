package life.hanyang.core.campusmap.repository;

import life.hanyang.core.campusmap.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Collection;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OpenSpaceRepository extends JpaRepository<OpenSpace, String> {
    @Query("select e.id from OpenSpace e where e.id in :ids")
    List<String> findExistingIds(@Param("ids") Collection<String> ids);
}
