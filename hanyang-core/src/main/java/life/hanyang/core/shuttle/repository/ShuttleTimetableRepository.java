package life.hanyang.core.shuttle.repository;

import life.hanyang.core.shuttle.domain.ShuttleDayType;
import life.hanyang.core.shuttle.domain.ShuttlePeriod;
import life.hanyang.core.shuttle.domain.ShuttleTimetable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShuttleTimetableRepository extends JpaRepository<ShuttleTimetable,Long> {
    List<ShuttleTimetable> findByShuttlePeriodAndShuttleDayType(ShuttlePeriod period, ShuttleDayType dayType);
}