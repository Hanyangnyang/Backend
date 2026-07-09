package life.hanyang.core.gym.repository;

import life.hanyang.core.gym.domain.GymScheduleCell;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GymScheduleCellRepository extends JpaRepository<GymScheduleCell, Long> {
    List<GymScheduleCell> findByGymPeriodId(Long periodId);
    Long countByIdIn(List<Long> scheduleCellIds);
}
