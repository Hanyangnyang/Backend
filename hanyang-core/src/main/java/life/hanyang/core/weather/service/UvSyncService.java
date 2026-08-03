package life.hanyang.core.weather.service;

import life.hanyang.core.weather.client.UvApiClient;
import life.hanyang.core.weather.domain.HourlyWeather;
import life.hanyang.core.weather.dto.UvResponseDto;
import life.hanyang.core.weather.repository.HourlyWeatherRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.cache.annotation.CacheEvict;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class UvSyncService {

    private final UvApiClient uvApiClient;
    private final HourlyWeatherRepository hourlyWeatherRepository;

    private static final String DEFAULT_LOCATION = "ANSAN";
    private static final String DEFAULT_AREA_NO = "4127100000"; // 안산시 상록구 행정구역코드
    private static final ZoneId KST_ZONE = ZoneId.of("Asia/Seoul");

    /**
     * 자외선 지수(UV Index) 75시간 예측치를 1시간 단위로 꽉 채워 동기화
     */
    @Transactional
    @CacheEvict(cacheNames = "weatherSummary", allEntries = true)
    public void syncUvIndex() {
        String baseTime = getLatestBaseTime();
        log.info("Starting syncUvIndex for areaNo: {}, time: {}", DEFAULT_AREA_NO, baseTime);

        UvResponseDto response = uvApiClient.fetchUvIndex(DEFAULT_AREA_NO, baseTime);
        if (isEmptyResponse(response)) {
            log.warn("UvResponseDto is empty.");
            return;
        }

        UvResponseDto.Item item = response.response().body().items().item().get(0);
        if (item.date() == null || item.date().isBlank()) {
            log.warn("UvResponseDto item date is empty.");
            return;
        }

        LocalDateTime baseDateTime = LocalDateTime.parse(item.date(), DateTimeFormatter.ofPattern("yyyyMMddHH"));

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

                HourlyWeather hourlyWeather = hourlyWeatherRepository.findByLocationAndForecastAt(DEFAULT_LOCATION, forecastAt)
                        .orElseGet(() -> HourlyWeather.builder()
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
        log.info("Successfully synced {} UV index hourly records for baseTime: {}", updatedCount, baseTime);
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
