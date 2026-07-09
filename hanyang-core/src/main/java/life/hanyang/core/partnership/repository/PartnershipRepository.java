package life.hanyang.core.partnership.repository;

import life.hanyang.core.partnership.domain.Partnership;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PartnershipRepository extends JpaRepository<Partnership, Long> {
}
