package life.hanyang.core.gym.repository;

import life.hanyang.core.gym.domain.GymPeriodType;
import life.hanyang.core.gym.domain.GymSemesterNo;
import org.springframework.data.jpa.repository.JpaRepository;
import life.hanyang.core.gym.domain.GymPeriod;
import org.springframework.stereotype.Repository;

@Repository
public interface GymPeriodRepository extends JpaRepository<GymPeriod, Long> {
    boolean existsByYearAndSemesterAndPeriodType(Integer year, GymSemesterNo semester, GymPeriodType periodType);
}