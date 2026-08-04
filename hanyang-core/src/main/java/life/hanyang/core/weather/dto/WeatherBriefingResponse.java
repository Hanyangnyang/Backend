package life.hanyang.core.weather.dto;

import life.hanyang.core.weather.domain.WeatherBriefing;

import java.time.LocalDateTime;

public record WeatherBriefingResponse(
        String location,
        String content,
        LocalDateTime forecastAt
) {
    public static WeatherBriefingResponse from(WeatherBriefing briefing) {
        return new WeatherBriefingResponse(
                briefing.getLocation(),
                briefing.getBriefingText(),
                briefing.getForecastAt()
        );
    }
}
