package life.hanyang.core.weather.dto;

import life.hanyang.core.weather.domain.HourlyWeather;
import java.time.LocalDateTime;

public record WeatherHourlyResponse(
        LocalDateTime forecastAt,
        Double temperature,       // 기온 (°C)
        Integer humidity,          // 습도 (%)
        String weatherCondition,   // SUNNY, CLOUDY, RAIN 등
        Integer precipProbability, // 강수확률 (%)
        Double precipitation,      // 강수량 (mm)
        Integer pm10Value,         // 미세먼지 농도 (µg/m³)
        Integer pm10Grade,         // 미세먼지 등급 (1:좋음, 2:보통, 3:나쁨, 4:매우나쁨)
        Integer pm25Value,         // 초미세먼지 농도 (µg/m³)
        Integer pm25Grade,         // 초미세먼지 등급
        Integer uvIndex            // 자외선 지수
) {
    public static WeatherHourlyResponse from(HourlyWeather weather, HourlyWeather fallbackFineDust) {
        Integer pm10Val = weather.getPm10Value() != null ? weather.getPm10Value() : (fallbackFineDust != null ? fallbackFineDust.getPm10Value() : null);
        Integer pm25Val = weather.getPm25Value() != null ? weather.getPm25Value() : (fallbackFineDust != null ? fallbackFineDust.getPm25Value() : null);
        Integer pm10Grd = weather.getPm10Grade() != null ? weather.getPm10Grade() : (fallbackFineDust != null ? fallbackFineDust.getPm10Grade() : null);
        Integer pm25Grd = weather.getPm25Grade() != null ? weather.getPm25Grade() : (fallbackFineDust != null ? fallbackFineDust.getPm25Grade() : null);

        return new WeatherHourlyResponse(
                weather.getForecastAt(),
                weather.getTemperature(),
                weather.getHumidity(),
                weather.getWeatherCondition(),
                weather.getPrecipProbability(),
                weather.getPrecipitation(),
                pm10Val,
                pm10Grd,
                pm25Val,
                pm25Grd,
                weather.getUvIndex()
        );
    }
}
