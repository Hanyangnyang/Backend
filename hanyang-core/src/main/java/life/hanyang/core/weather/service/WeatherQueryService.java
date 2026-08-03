package life.hanyang.core.weather.service;

import life.hanyang.core.weather.domain.HourlyWeather;
import life.hanyang.core.weather.dto.WeatherCompositeResponse;
import life.hanyang.core.weather.dto.WeatherCurrentResponse;
import life.hanyang.core.weather.dto.WeatherHourlyResponse;
import life.hanyang.core.weather.repository.HourlyWeatherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WeatherQueryService {

    private final HourlyWeatherRepository hourlyWeatherRepository;
    private final FineDustSyncService fineDustSyncService;

    private static final String DEFAULT_LOCATION = "ANSAN";
    private static final ZoneId KST_ZONE = ZoneId.of("Asia/Seoul");

    /**
     * 메인 화면용 통합 날씨 정보 조회 (현재 카드 + 과거 24시간 ~ 미래 24시간 시간별 슬라이더)
     */
    @Cacheable(cacheNames = "weatherSummary", key = "'summary'")
    public WeatherCompositeResponse getWeatherSummary() {
        LocalDateTime now = LocalDateTime.now(KST_ZONE).withMinute(0).withSecond(0).withNano(0);

        // 과거 24시간 전부터 ~ 미래 24시간 후까지 (총 48시간 범위)
        LocalDateTime start = now.minusHours(24);
        LocalDateTime end = now.plusHours(24);

        // 미세먼지 수치 누락 시 보완을 위한 최신 관측 미세먼지 레코드 1회만 조회 (엔티티 수정 방지)
        HourlyWeather fallbackFineDust = fineDustSyncService.getLatestFineDustRecord(DEFAULT_LOCATION).orElse(null);

        // 1. 현재 정시 날씨 조회 및 순수 DTO 합성
        HourlyWeather currentWeather = hourlyWeatherRepository.findByLocationAndForecastAt(DEFAULT_LOCATION, now)
                .orElseGet(() -> HourlyWeather.builder()
                        .location(DEFAULT_LOCATION)
                        .forecastAt(now)
                        .temperature(0.0)
                        .build());

        WeatherCurrentResponse currentResponse = WeatherCurrentResponse.from(currentWeather, fallbackFineDust);

        // 2. 과거 24시간 ~ 미래 24시간 목록 조회 및 순수 DTO 합성
        List<HourlyWeather> hourlyList = hourlyWeatherRepository
                .findAllByLocationAndForecastAtBetweenOrderByForecastAtAsc(DEFAULT_LOCATION, start, end);

        List<WeatherHourlyResponse> hourlyResponses = hourlyList.stream()
                .map(weather -> WeatherHourlyResponse.from(weather, fallbackFineDust))
                .toList();

        return new WeatherCompositeResponse(currentResponse, hourlyResponses);
    }
}
