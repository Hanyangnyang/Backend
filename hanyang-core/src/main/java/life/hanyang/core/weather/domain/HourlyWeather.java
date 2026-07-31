package life.hanyang.core.weather.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "hourly_weather",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_location_forecast_at",
                        columnNames = {"location", "forecast_at"}
                )
        },
        indexes = {
                @Index(name = "idx_forecast_at", columnList = "forecast_at")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HourlyWeather {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String location;

    @Column(name = "forecast_at", nullable = false)
    private LocalDateTime forecastAt;

    @Column(nullable = false)
    private Double temperature;

    private Integer humidity;

    @Column(length = 30)
    private String weatherCondition;

    private Integer skyState;
    private Integer rainState;
    private Integer precipProbability;
    private Double precipitation;
    private Boolean hasThunder;
    private Integer pm10Value;
    private Integer pm25Value;
    private Integer uvIndex;

    @Builder
    public HourlyWeather(String location, LocalDateTime forecastAt, Double temperature, Integer humidity,
                         String weatherCondition, Integer skyState, Integer rainState,
                         Integer precipProbability, Double precipitation, Boolean hasThunder,
                         Integer pm10Value, Integer pm25Value, Integer uvIndex) {
        this.location = location;
        this.forecastAt = forecastAt;
        this.temperature = temperature;
        this.humidity = humidity;
        this.skyState = skyState;
        this.rainState = rainState;
        this.precipProbability = precipProbability;
        this.precipitation = precipitation;
        this.hasThunder = hasThunder != null ? hasThunder : false;
        this.pm10Value = pm10Value;
        this.pm25Value = pm25Value;
        this.uvIndex = uvIndex;

        if (weatherCondition != null) {
            this.weatherCondition = weatherCondition;
        } else {
            recalculateWeatherCondition();
        }
    }

    // 1) 단기예보 PATCH (Non-null 값만 부분 업데이트)
    public void patchVillageFcst(Double temperature, Integer humidity, Integer skyState,
                                 Integer rainState, Integer precipProbability, Double precipitation) {
        if (temperature != null) this.temperature = temperature;
        if (humidity != null) this.humidity = humidity;
        if (skyState != null) this.skyState = skyState;
        if (rainState != null) this.rainState = rainState;
        if (precipProbability != null) this.precipProbability = precipProbability;
        if (precipitation != null) this.precipitation = precipitation;

        recalculateWeatherCondition();
    }

    // 2) 초단기예보 PATCH (Non-null 값만 부분 업데이트)
    public void patchUltraSrtFcst(Double temperature, Integer humidity, Integer skyState,
                                  Integer rainState, Double precipitation, Boolean hasThunder) {
        if (temperature != null) this.temperature = temperature;
        if (humidity != null) this.humidity = humidity;
        if (skyState != null) this.skyState = skyState;
        if (rainState != null) this.rainState = rainState;
        if (precipitation != null) this.precipitation = precipitation;
        if (hasThunder != null) this.hasThunder = hasThunder;

        recalculateWeatherCondition();
    }

    // 3) 초단기실황 PATCH (Non-null 값만 부분 업데이트)
    public void patchUltraSrtNcst(Double temperature, Integer humidity, Integer rainState, Double precipitation) {
        if (temperature != null) this.temperature = temperature;
        if (humidity != null) this.humidity = humidity;
        if (rainState != null) this.rainState = rainState;
        if (precipitation != null) this.precipitation = precipitation;

        recalculateWeatherCondition();
    }

    // 현재 보유 중인 skyState와 rainState 기반으로 weatherCondition 상태 자동 재계산
    private void recalculateWeatherCondition() {
        WeatherCondition condition = WeatherCondition.from(this.skyState, this.rainState);
        this.weatherCondition = condition.name();
    }
}
