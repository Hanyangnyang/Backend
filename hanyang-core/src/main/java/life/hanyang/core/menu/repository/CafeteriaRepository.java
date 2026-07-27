package life.hanyang.core.menu.repository;

import life.hanyang.core.menu.entity.Cafeteria;
import life.hanyang.core.menu.entity.CafeteriaCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CafeteriaRepository extends JpaRepository<Cafeteria, Long> {
    Optional<Cafeteria> findByCode(CafeteriaCode code);
}
