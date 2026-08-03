package life.hanyang.core.weather.dto;

import java.util.List;

public record WeatherCompositeResponse(
        WeatherCurrentResponse current,
        List<WeatherHourlyResponse> hourly
) {
}
