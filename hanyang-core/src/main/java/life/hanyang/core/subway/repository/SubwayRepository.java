package life.hanyang.core.subway.repository;

import life.hanyang.core.subway.domain.SubwayDayType;
import life.hanyang.core.subway.domain.SubwayDirection;
import life.hanyang.core.subway.domain.SubwayLine;
import life.hanyang.core.subway.domain.SubwayStation;
import life.hanyang.core.subway.domain.SubwayTimetable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubwayRepository extends JpaRepository<SubwayTimetable,Long> {
    @Modifying(clearAutomatically = true)
    @Query("delete from SubwayTimetable s where s.subwayStation = :subwayStation")
    int deleteBySubwayStationInBatch(@Param("subwayStation") SubwayStation subwayStation);

    List<SubwayTimetable> findBySubwayStationAndSubwayLineAndDirectionAndSubwayDayTypeOrderByTimeAsc(
            SubwayStation subwayStation,
            SubwayLine subwayLine,
            SubwayDirection direction,
            SubwayDayType subwayDayType
    );
}
