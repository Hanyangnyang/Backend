package life.hanyang.core.weather.service;

import life.hanyang.core.global.exception.BusinessException;
import life.hanyang.core.global.exception.ErrorCode;
import life.hanyang.core.global.util.TransactionCacheEvictor;
import life.hanyang.core.weather.client.FineDustApiClient;
import life.hanyang.core.weather.domain.HourlyWeather;
import life.hanyang.core.weather.dto.AirKoreaRealtimeApiResponse;
import life.hanyang.core.weather.repository.HourlyWeatherRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class FineDustSyncService {

    private final FineDustApiClient fineDustApiClient;
    private final HourlyWeatherRepository hourlyWeatherRepository;
    private final TransactionCacheEvictor transactionCacheEvictor;

    private static final String DEFAULT_LOCATION = "ANSAN";
    private static final String DEFAULT_STATION = "본오동";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");


    /**
     * 에어코리아 실시간 미세먼지 데이터 동기화
     */
    @Transactional
    public void syncRealtimeFineDust() {
        log.info("[FineDustSyncService] 미세먼지 실황 동기화 시작 (측정소: {})", DEFAULT_STATION);

        AirKoreaRealtimeApiResponse response;
        try {
            response = fineDustApiClient.fetchRealtimeFineDust(DEFAULT_STATION);
        } catch (Exception e) {
            throw new BusinessException("에어코리아 미세먼지 API 호출 실패: " + e.getMessage(), ErrorCode.INTERNAL_SERVER_ERROR, e);
        }

        if (isEmptyResponse(response)) {
            throw new BusinessException(
                    String.format("미세먼지 API 응답 데이터가 비어 있습니다. (station: %s)", DEFAULT_STATION),
                    ErrorCode.ENTITY_NOT_FOUND
            );
        }

        List<AirKoreaRealtimeApiResponse.Response.Body.AirKoreaRealtimeItem> items =
                response.response().body().items();

        if (items == null || items.isEmpty()) {
            throw new BusinessException(
                    String.format("미세먼지 측정항목 목록이 비어 있습니다. (station: %s)", DEFAULT_STATION),
                    ErrorCode.ENTITY_NOT_FOUND
            );
        }

        // 가장 최신 관측 데이터 (첫 번째 아이템)
        AirKoreaRealtimeApiResponse.Response.Body.AirKoreaRealtimeItem latestItem = items.get(0);

        if (latestItem.dataTime() == null || latestItem.dataTime().isBlank()) {
            throw new BusinessException(
                    String.format("최신 미세먼지 관측시각(dataTime) 정보가 없습니다. (station: %s)", DEFAULT_STATION),
                    ErrorCode.ENTITY_NOT_FOUND
            );
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

        transactionCacheEvictor.evictCacheAfterCommit("weatherSummary");
        log.info("[FineDustSyncService] 미세먼지 실황 동기화 완료 (관측시각: {}, PM10: {}, PM2.5: {})", forecastAt, pm10Value, pm25Value);
    }

    /**
     * 가장 최근 관측된 미세먼지 레코드 조회 (DTO 합성용)
     */
    @Transactional(readOnly = true)
    public Optional<HourlyWeather> getLatestFineDustRecord(String location) {
        return hourlyWeatherRepository.findFirstByLocationAndPm10ValueIsNotNullOrderByForecastAtDesc(location);
    }

    private boolean isEmptyResponse(AirKoreaRealtimeApiResponse response) {
        return response == null ||
                response.response() == null ||
                response.response().body() == null ||
                response.response().body().items() == null ||
                response.response().body().items().isEmpty();
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
