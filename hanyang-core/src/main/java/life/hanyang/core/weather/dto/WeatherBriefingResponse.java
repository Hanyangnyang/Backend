package life.hanyang.core.weather.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import life.hanyang.core.weather.domain.WeatherBriefing;

import java.time.LocalDateTime;

public record WeatherBriefingResponse(
        @JsonProperty("location") String location,
        @JsonProperty("content") String content,
        @JsonProperty("forecastAt") LocalDateTime forecastAt
) {
    public static WeatherBriefingResponse from(WeatherBriefing briefing) {
        return new WeatherBriefingResponse(
                briefing.getLocation(),
                briefing.getBriefingText(),
                briefing.getForecastAt()
        );
    }
}
