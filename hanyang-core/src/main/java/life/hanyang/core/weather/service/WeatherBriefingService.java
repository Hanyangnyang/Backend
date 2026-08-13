package life.hanyang.core.weather.service;

import life.hanyang.core.global.exception.BusinessException;
import life.hanyang.core.global.exception.EntityNotFoundException;
import life.hanyang.core.global.exception.ErrorCode;
import life.hanyang.core.global.llm.gemini.GeminiApiClient;
import life.hanyang.core.weather.domain.HourlyWeather;
import life.hanyang.core.weather.domain.WeatherBriefing;
import life.hanyang.core.weather.dto.WeatherBriefingResponse;
import life.hanyang.core.weather.repository.HourlyWeatherRepository;
import life.hanyang.core.weather.repository.WeatherBriefingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
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

        return hourlyWeatherRepository.findAllByLocationAndForecastAtBetweenOrderByForecastAtAsc(
                location,
                startHour,
                endHour
        );
    }

    @Caching(evict = {
            @CacheEvict(value = "weatherBriefing", key = "#location + ':latest'"),
            @CacheEvict(value = "weatherBriefing", key = "#location + ':' + T(java.time.LocalDateTime).now(T(java.time.ZoneId).of('Asia/Seoul')).withMinute(0).withSecond(0).withNano(0)")
    })
    @Transactional
    public void generateAndSaveBriefing(String location) {
        generateAndSaveBriefing(location, LocalDateTime.now(ZoneId.of("Asia/Seoul")));
    }

    @Caching(evict = {
            @CacheEvict(value = "weatherBriefing", key = "#location + ':latest'"),
            @CacheEvict(value = "weatherBriefing", key = "#location + ':' + #baseTime.withMinute(0).withSecond(0).withNano(0).toString()")
    })
    @Transactional
    public void generateAndSaveBriefing(String location, LocalDateTime baseTime) {
        List<HourlyWeather> hourlyWeathers = fetchHourlyWeathers(location, baseTime, 24);

        if (hourlyWeathers.isEmpty()) {
            throw new BusinessException(
                    String.format("날씨 수집 데이터가 존재하지 않아 브리핑을 생성할 수 없습니다. (location: %s, baseTime: %s)", location, baseTime),
                    ErrorCode.ENTITY_NOT_FOUND
            );
        }

        LocalDateTime baseForecastAt = hourlyWeathers.get(0).getForecastAt();
        String prompt = weatherPromptBuilder.buildPrompt(hourlyWeathers, baseTime);

        String briefingText;
        try {
            briefingText = geminiApiClient.generateContent(prompt);
        } catch (Exception e) {
            throw new BusinessException(
                    "Gemini LLM 브리핑 생성에 실패했습니다: " + e.getMessage(),
                    ErrorCode.INTERNAL_SERVER_ERROR
            );
        }

        WeatherBriefing briefing = WeatherBriefing.builder()
                .location(location)
                .briefingText(briefingText)
                .forecastAt(baseForecastAt)
                .build();

        weatherBriefingRepository.save(briefing);
    }

    @Cacheable(
            value = "weatherBriefing",
            key = "#dateTime == null ? #location + ':latest' : #location + ':' + #dateTime"
    )
    public WeatherBriefingResponse getBriefing(String location, LocalDateTime dateTime) {
        if (dateTime != null) {
            LocalDateTime targetTime = dateTime.withMinute(0).withSecond(0).withNano(0);
            return weatherBriefingRepository.findByLocationAndForecastAt(location, targetTime)
                    .map(WeatherBriefingResponse::from)
                    .orElseThrow(() -> {
                        log.warn("[WeatherBriefing] 요청 시각 브리핑 없음. location: {}, targetTime: {}", location, targetTime);
                        return new EntityNotFoundException("해당 시각의 날씨 브리핑을 찾을 수 없습니다.");
                    });
        }
        return weatherBriefingRepository.findTopByLocationOrderByForecastAtDesc(location)
                .map(WeatherBriefingResponse::from)
                .orElseThrow(() -> {
                    log.warn("[WeatherBriefing] 최신 브리핑 데이터 없음. location: {}", location);
                    return new EntityNotFoundException("날씨 브리핑 데이터를 찾을 수 없습니다.");
                });
    }
}