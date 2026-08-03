package life.hanyang.core.weather.repository;

import life.hanyang.core.weather.domain.HourlyWeather;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.Optional;

public interface HourlyWeatherRepository extends JpaRepository<HourlyWeather,Long> {
    Optional<HourlyWeather> findByLocationAndForecastAt(String location, LocalDateTime forecastAt);

    Optional<HourlyWeather> findFirstByLocationAndPm10ValueIsNotNullOrderByForecastAtDesc(String location);
}

