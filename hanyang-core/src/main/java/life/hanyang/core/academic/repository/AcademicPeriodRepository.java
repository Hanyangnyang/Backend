package life.hanyang.core.academic.repository;

import life.hanyang.core.academic.domain.AcademicPeriod;
import life.hanyang.core.academic.domain.AcademicPeriodType;
import life.hanyang.core.academic.domain.AcademicSemester;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AcademicPeriodRepository extends JpaRepository<AcademicPeriod, Long> {

    /**
     * 조회일보다 시작일이 이전이거나 같은 학사 일정 중 가장 최신 일정 단건 조회 (공백 없는 무결성 보장)
     */
    Optional<AcademicPeriod> findFirstByStartDateLessThanEqualOrderByStartDateDesc(LocalDate date);

    List<AcademicPeriod> findByYearOrderByStartDateAsc(int year);

    boolean existsByYearAndSemesterAndPeriodType(int year, AcademicSemester semester, AcademicPeriodType periodType);
}
