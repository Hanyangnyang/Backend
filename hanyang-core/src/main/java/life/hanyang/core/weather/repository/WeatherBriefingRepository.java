package life.hanyang.core.weather.repository;

import life.hanyang.core.weather.domain.WeatherBriefing;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface WeatherBriefingRepository extends JpaRepository<WeatherBriefing, Long> {

    Optional<WeatherBriefing> findTopByLocationOrderByForecastAtDesc(String location);
    Optional<WeatherBriefing> findByLocationAndForecastAt(String location, LocalDateTime targetTime);
}