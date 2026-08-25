package life.hanyang.core.holiday.repository;

import life.hanyang.core.holiday.domain.Holiday;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface HolidayRepository extends JpaRepository<Holiday, Long> {
    Optional<Holiday> findByDate(LocalDate date);
    List<Holiday> findByDateBetweenOrderByDateAsc(LocalDate startDate, LocalDate endDate);
    boolean existsByDate(LocalDate date);
}
