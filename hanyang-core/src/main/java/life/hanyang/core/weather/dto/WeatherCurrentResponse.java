package life.hanyang.core.weather.dto;

import life.hanyang.core.weather.domain.HourlyWeather;
import java.time.LocalDateTime;

public record WeatherCurrentResponse(
        LocalDateTime forecastAt,
        Double temperature,       // 기온 (°C)
        Double minTemperature,    // 오늘 최저 기온 (°C)
        Double maxTemperature,    // 오늘 최고 기온 (°C)
        Integer humidity,          // 습도 (%)
        String weatherCondition,   // SUNNY, CLOUDY, RAIN 등 (날씨 정보가 없으면 null)
        Double precipitation,      // 강수량 (mm)
        Integer pm10Value,         // 미세먼지 농도 (µg/m³)
        Integer pm10Grade,         // 미세먼지 등급 (1:좋음, 2:보통, 3:나쁨, 4:매우나쁨)
        Integer pm25Value,         // 초미세먼지 농도 (µg/m³)
        Integer pm25Grade,         // 초미세먼지 등급
        Integer uvIndex            // 자외선 지수 (null: 점검중 또는 데이터 없음)
) {
    public static WeatherCurrentResponse from(
            HourlyWeather weather,
            HourlyWeather fallbackFineDust,
            Double minTemperature,
            Double maxTemperature
    ) {
        Integer pm10Val = weather.getPm10Value() != null ? weather.getPm10Value() : (fallbackFineDust != null ? fallbackFineDust.getPm10Value() : null);
        Integer pm25Val = weather.getPm25Value() != null ? weather.getPm25Value() : (fallbackFineDust != null ? fallbackFineDust.getPm25Value() : null);
        Integer pm10Grd = weather.getPm10Grade() != null ? weather.getPm10Grade() : (fallbackFineDust != null ? fallbackFineDust.getPm10Grade() : null);
        Integer pm25Grd = weather.getPm25Grade() != null ? weather.getPm25Grade() : (fallbackFineDust != null ? fallbackFineDust.getPm25Grade() : null);

        return new WeatherCurrentResponse(
                weather.getForecastAt(),
                weather.getTemperature(),
                minTemperature,
                maxTemperature,
                weather.getHumidity(),
                weather.getWeatherCondition(),
                weather.getPrecipitation(),
                pm10Val,
                pm10Grd,
                pm25Val,
                pm25Grd,
                weather.getUvIndex()
        );
    }
}
