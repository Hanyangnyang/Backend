package life.hanyang.core.weather.service;

import life.hanyang.core.global.exception.BusinessException;
import life.hanyang.core.global.exception.ErrorCode;
import life.hanyang.core.global.util.TransactionCacheEvictor;
import life.hanyang.core.weather.client.UvApiClient;
import life.hanyang.core.weather.domain.HourlyWeather;
import life.hanyang.core.weather.dto.UvResponseDto;
import life.hanyang.core.weather.repository.HourlyWeatherRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UvSyncService {

    private final UvApiClient uvApiClient;
    private final HourlyWeatherRepository hourlyWeatherRepository;
    private final TransactionCacheEvictor transactionCacheEvictor;

    private static final String DEFAULT_LOCATION = "ANSAN";
    private static final String DEFAULT_AREA_NO = "4127100000"; // 안산시 상록구 행정구역코드
    private static final ZoneId KST_ZONE = ZoneId.of("Asia/Seoul");

    /**
     * 자외선 지수(UV Index) 75시간 예측치를 1시간 단위로 꽉 채워 동기화
     */
    @Transactional
    public void syncUvIndex() {
        String baseTime = getLatestBaseTime();
        log.info("[UvSyncService] 자외선 지수 동기화 시작 (행정구역: {}, 기준시각: {})", DEFAULT_AREA_NO, baseTime);

        UvResponseDto response;
        try {
            response = uvApiClient.fetchUvIndex(DEFAULT_AREA_NO, baseTime);
        } catch (Exception e) {
            throw new BusinessException("기상청 자외선 API 호출 실패: " + e.getMessage(), ErrorCode.INTERNAL_SERVER_ERROR);
        }

        if (isEmptyResponse(response)) {
            throw new BusinessException(
                    String.format("자외선 API 응답 데이터가 비어 있습니다. (areaNo: %s, baseTime: %s)", DEFAULT_AREA_NO, baseTime),
                    ErrorCode.ENTITY_NOT_FOUND
            );
        }

        UvResponseDto.Item item = response.response().body().items().item().get(0);
        if (item.date() == null || item.date().isBlank()) {
            throw new BusinessException(
                    String.format("자외선 지수 발표시각(date) 정보가 없습니다. (areaNo: %s, baseTime: %s)", DEFAULT_AREA_NO, baseTime),
                    ErrorCode.ENTITY_NOT_FOUND
            );
        }

        LocalDateTime baseDateTime = LocalDateTime.parse(item.date(), DateTimeFormatter.ofPattern("yyyyMMddHH"));
        LocalDateTime maxDateTime = baseDateTime.plusHours(77); // 75h + 2h offset

        // 1. DB 단 1회 조회를 통해 해당 범위의 기존 레코드를 Map으로 조작
        Map<LocalDateTime, HourlyWeather> existingMap = hourlyWeatherRepository
                .findAllByLocationAndForecastAtBetweenOrderByForecastAtAsc(DEFAULT_LOCATION, baseDateTime, maxDateTime)
                .stream()
                .collect(Collectors.toMap(HourlyWeather::getForecastAt, w -> w, (existing, replacement) -> existing));

        Map<Integer, String> hourValueMap = createHourValueMap(item);

        int updatedCount = 0;
        List<HourlyWeather> weatherListToSave = new ArrayList<>();

        for (Map.Entry<Integer, String> entry : hourValueMap.entrySet()) {
            Integer hoursToAdd = entry.getKey();
            Integer uvValue = parseInt(entry.getValue());

            if (uvValue == null) continue;

            // 3시간 간격 응답을 1시간 단위 3개 레코드에 꽉 채워서 부여 (예: 0h -> +0h, +1h, +2h)
            for (int offset = 0; offset < 3; offset++) {
                LocalDateTime forecastAt = baseDateTime.plusHours(hoursToAdd + offset);

                HourlyWeather hourlyWeather = existingMap.computeIfAbsent(forecastAt, k -> HourlyWeather.builder()
                        .location(DEFAULT_LOCATION)
                        .forecastAt(forecastAt)
                        .temperature(0.0)
                        .build());

                hourlyWeather.patchUvIndex(uvValue);
                weatherListToSave.add(hourlyWeather);
                updatedCount++;
            }
        }

        hourlyWeatherRepository.saveAll(weatherListToSave);
        transactionCacheEvictor.evictCacheAfterCommit("weatherSummary");
        log.info("[UvSyncService] 자외선 지수 {}건 동기화 완료 (기준시각: {})", updatedCount, baseTime);
    }

    private String getLatestBaseTime() {
        LocalDateTime now = LocalDateTime.now(KST_ZONE);
        String dateStr = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int hour = now.getHour();
        return hour < 18 ? dateStr + "06" : dateStr + "18";
    }

    private boolean isEmptyResponse(UvResponseDto response) {
        return response == null || response.response() == null
                || response.response().body() == null
                || response.response().body().items() == null
                || response.response().body().items().item() == null
                || response.response().body().items().item().isEmpty();
    }

    private Map<Integer, String> createHourValueMap(UvResponseDto.Item item) {
        Map<Integer, String> map = new LinkedHashMap<>();
        map.put(0, item.h0());
        map.put(3, item.h3());
        map.put(6, item.h6());
        map.put(9, item.h9());
        map.put(12, item.h12());
        map.put(15, item.h15());
        map.put(18, item.h18());
        map.put(21, item.h21());
        map.put(24, item.h24());
        map.put(27, item.h27());
        map.put(30, item.h30());
        map.put(33, item.h33());
        map.put(36, item.h36());
        map.put(39, item.h39());
        map.put(42, item.h42());
        map.put(45, item.h45());
        map.put(48, item.h48());
        map.put(51, item.h51());
        map.put(54, item.h54());
        map.put(57, item.h57());
        map.put(60, item.h60());
        map.put(63, item.h63());
        map.put(66, item.h66());
        map.put(69, item.h69());
        map.put(72, item.h72());
        map.put(75, item.h75());
        return map;
    }

    private Integer parseInt(String val) {
        if (val == null || val.isBlank()) return null;
        try {
            return Integer.parseInt(val.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
