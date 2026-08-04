package life.hanyang.core.weather.service;

import life.hanyang.core.global.llm.gemini.GeminiApiClient;
import life.hanyang.core.weather.domain.HourlyWeather;
import life.hanyang.core.weather.domain.WeatherBriefing;
import life.hanyang.core.weather.repository.HourlyWeatherRepository;
import life.hanyang.core.weather.repository.WeatherBriefingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WeatherBriefingService {
    private final HourlyWeatherRepository hourlyWeatherRepository;
    private final WeatherBriefingRepository weatherBriefingRepository;
    private final GeminiApiClient geminiApiClient;
    private final WeatherPromptBuilder weatherPromptBuilder;

    private List<HourlyWeather> fetchHourlyWeathers(String location, LocalDateTime baseTime, int durationHours) {
        LocalDateTime startHour = baseTime.withMinute(0)
                .withSecond(0)
                .withNano(0);

        LocalDateTime endHour = startHour.plusHours(durationHours);

        return hourlyWeatherRepository.findByLocationAndForecastAtBetweenOrderByForecastAtAsc(
                location,
                startHour,
                endHour
        );
    }

    public void generateAndSaveBriefing(String location) {
        generateAndSaveBriefing(location, LocalDateTime.now(ZoneId.of("Asia/Seoul")));
    }

    public void generateAndSaveBriefing(String location, LocalDateTime baseTime) {
        List<HourlyWeather> hourlyWeathers = fetchHourlyWeathers(location, baseTime, 24);

        if (hourlyWeathers.isEmpty()) {
            log.warn("날씨 데이터가 존재하지 않아 브리핑을 생성할 수 없습니다. location: {}, baseTime: {}", location, baseTime);
            return;
        }

        LocalDateTime baseForecastAt = hourlyWeathers.get(0).getForecastAt();
        String prompt = weatherPromptBuilder.buildPrompt(hourlyWeathers, baseTime);
        String briefingText = geminiApiClient.generateContent(prompt);

        WeatherBriefing briefing = WeatherBriefing.builder()
                .location(location)
                .briefingText(briefingText)
                .forecastAt(baseForecastAt)
                .build();

        weatherBriefingRepository.save(briefing);
    }
}
