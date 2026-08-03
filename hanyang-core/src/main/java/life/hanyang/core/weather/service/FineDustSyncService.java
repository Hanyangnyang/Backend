package life.hanyang.core.weather.service;

import life.hanyang.core.weather.client.FineDustApiClient;
import life.hanyang.core.weather.domain.HourlyWeather;
import life.hanyang.core.weather.dto.AirKoreaRealtimeApiResponse;
import life.hanyang.core.weather.repository.HourlyWeatherRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.cache.annotation.CacheEvict;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FineDustSyncService {

    private final FineDustApiClient fineDustApiClient;
    private final HourlyWeatherRepository hourlyWeatherRepository;

    private static final String DEFAULT_LOCATION = "ANSAN";
    private static final String DEFAULT_STATION = "본오동";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");


    /**
     * 에어코리아 실시간 미세먼지 데이터 동기화
     */
    @Transactional
    @CacheEvict(cacheNames = "weatherSummary", allEntries = true)
    public void syncRealtimeFineDust() {
        log.info("Starting syncRealtimeFineDust for station: {}", DEFAULT_STATION);

        AirKoreaRealtimeApiResponse response = fineDustApiClient.fetchRealtimeFineDust(DEFAULT_STATION);
        if (isEmptyResponse(response)) {
            log.warn("FineDust API response is empty.");
            return;
        }

        List<AirKoreaRealtimeApiResponse.Response.Body.AirKoreaRealtimeItem> items =
                response.response().body().items();

        if (items == null || items.isEmpty()) {
            log.warn("FineDust items list is empty.");
            return;
        }

        // 가장 최신 관측 데이터 (첫 번째 아이템)
        AirKoreaRealtimeApiResponse.Response.Body.AirKoreaRealtimeItem latestItem = items.get(0);

        if (latestItem.dataTime() == null) {
            log.warn("Latest FineDust item dataTime is null.");
            return;
        }

        LocalDateTime forecastAt = LocalDateTime.parse(latestItem.dataTime(), DATE_TIME_FORMATTER);
        Integer pm10Value = parseInt(latestItem.pm10Value());
        Integer pm25Value = parseInt(latestItem.pm25Value());
        Integer pm10Grade = parseInt(latestItem.pm10Grade1h());
        Integer pm25Grade = parseInt(latestItem.pm25Grade1h());

        HourlyWeather hourlyWeather = hourlyWeatherRepository.findByLocationAndForecastAt(DEFAULT_LOCATION, forecastAt)
                .orElseGet(() -> HourlyWeather.builder()
                        .location(DEFAULT_LOCATION)
                        .forecastAt(forecastAt)
                        .temperature(0.0) // 미세먼지 데이터 선 진입 시 기본 온도값
                        .build());

        hourlyWeather.patchFineDust(pm10Value, pm25Value, pm10Grade, pm25Grade);
        hourlyWeatherRepository.save(hourlyWeather);

        log.info("Successfully synced FineDust for forecastAt: {}, pm10: {}, pm25: {}", forecastAt, pm10Value, pm25Value);
    }

    /**
     * 대상 HourlyWeather의 미세먼지 정보가 null인 경우, 가장 최근의 관측 데이터로 보완(Fallback)
     */
    @Transactional(readOnly = true)
    public void fillFallbackFineDust(HourlyWeather targetWeather) {
        if (targetWeather == null || targetWeather.getPm10Value() != null) {
            return;
        }

        hourlyWeatherRepository.findFirstByLocationAndPm10ValueIsNotNullOrderByForecastAtDesc(targetWeather.getLocation())
                .ifPresent(latest -> targetWeather.patchFineDust(
                        latest.getPm10Value(),
                        latest.getPm25Value(),
                        latest.getPm10Grade(),
                        latest.getPm25Grade()
                ));
    }

    private boolean isEmptyResponse(AirKoreaRealtimeApiResponse response) {
        return response == null ||
                response.response() == null ||
                response.response().body() == null;
    }

    private Integer parseInt(String value) {
        if (value == null || value.equals("-") || value.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
