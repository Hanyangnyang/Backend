package life.hanyang.core.weather.repository;

import life.hanyang.core.weather.domain.WeatherBriefing;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WeatherBriefingRepository extends JpaRepository<WeatherBriefing, Long> {
}