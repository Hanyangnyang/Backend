package life.hanyang.core.weather.dto;

import life.hanyang.core.weather.domain.HourlyWeather;
import java.time.LocalDateTime;

public record WeatherCurrentResponse(
        LocalDateTime forecastAt,
        Double temperature,       // 기온 (°C)
        Integer humidity,          // 습도 (%)
        String weatherCondition,   // SUNNY, CLOUDY, RAIN 등 (날씨 정보가 없으면 null)
        Double precipitation,      // 강수량 (mm)
        Integer pm10Value,         // 미세먼지 농도 (µg/m³)
        Integer pm10Grade,         // 미세먼지 등급 (1:좋음, 2:보통, 3:나쁨, 4:매우나쁨)
        Integer pm25Value,         // 초미세먼지 농도 (µg/m³)
        Integer pm25Grade,         // 초미세먼지 등급
        Integer uvIndex            // 자외선 지수
) {
    public static WeatherCurrentResponse from(HourlyWeather weather) {
        return new WeatherCurrentResponse(
                weather.getForecastAt(),
                weather.getTemperature(),
                weather.getHumidity(),
                weather.getWeatherCondition(),
                weather.getPrecipitation(),
                weather.getPm10Value(),
                weather.getPm10Grade(),
                weather.getPm25Value(),
                weather.getPm25Grade(),
                weather.getUvIndex()
        );
    }
}
