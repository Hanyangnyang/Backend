package life.hanyang.core.weather.service;

import life.hanyang.core.weather.client.WeatherApiClient;
import life.hanyang.core.weather.domain.HourlyWeather;
import life.hanyang.core.weather.dto.UltraSrtFcstResponseDto;
import life.hanyang.core.weather.dto.UltraSrtNcstResponseDto;
import life.hanyang.core.weather.dto.VillageFcstResponseDto;
import life.hanyang.core.weather.repository.HourlyWeatherRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.cache.annotation.CacheEvict;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WeatherSyncService {

    private final WeatherApiClient weatherApiClient;
    private final HourlyWeatherRepository hourlyWeatherRepository;

    private static final String DEFAULT_LOCATION = "ANSAN";
    private static final int DEFAULT_NX = 58;
    private static final int DEFAULT_NY = 121;
    private static final ZoneId KST_ZONE = ZoneId.of("Asia/Seoul");

    // 1. 단기예보 3일치 정보 업데이트 (POP, SKY, PTY, TMP, REH, PCP)
    @Transactional
    @CacheEvict(cacheNames = "weatherSummary", allEntries = true)
    public void syncVillageFcst() {
        String[] baseDateTime = getLatestVillageBaseDateTime();
        log.info("Starting syncVillageFcst for baseDate: {}, baseTime: {}", baseDateTime[0], baseDateTime[1]);

        VillageFcstResponseDto response = weatherApiClient.fetchVillageFcst(baseDateTime[0], baseDateTime[1], DEFAULT_NX, DEFAULT_NY);
        if (isEmptyResponse(response)) {
            log.warn("VillageFcst API response is empty.");
            return;
        }

        Map<LocalDateTime, List<VillageFcstResponseDto.Item>> timeGroupMap = groupItemsByForecastTime(response);
        if (timeGroupMap.isEmpty()) return;

        List<LocalDateTime> sortedTimes = timeGroupMap.keySet().stream().sorted().toList();
        LocalDateTime minTime = sortedTimes.get(0);
        LocalDateTime maxTime = sortedTimes.get(sortedTimes.size() - 1);

        Map<LocalDateTime, HourlyWeather> existingMap = hourlyWeatherRepository
                .findAllByLocationAndForecastAtBetweenOrderByForecastAtAsc(DEFAULT_LOCATION, minTime, maxTime)
                .stream()
                .collect(Collectors.toMap(HourlyWeather::getForecastAt, w -> w, (existing, replacement) -> existing));

        List<HourlyWeather> weatherListToSave = new ArrayList<>();

        for (Map.Entry<LocalDateTime, List<VillageFcstResponseDto.Item>> entry : timeGroupMap.entrySet()) {
            LocalDateTime forecastAt = entry.getKey();
            List<VillageFcstResponseDto.Item> itemsAtTime = entry.getValue();

            HourlyWeather existing = existingMap.get(forecastAt);
            HourlyWeather hourlyWeather = createOrPatchVillageFcstWithExisting(forecastAt, itemsAtTime, existing);
            weatherListToSave.add(hourlyWeather);
        }

        hourlyWeatherRepository.saveAll(weatherListToSave);
        log.info("Successfully synced {} hourly weather records from VillageFcst.", weatherListToSave.size());
    }

    // 2. 초단기예보 6시간치 정보 업데이트 (T1H, RN1, SKY, PTY, LGT, REH)
    @Transactional
    @CacheEvict(cacheNames = "weatherSummary", allEntries = true)
    public void syncUltraSrtFcst() {
        String[] baseDateTime = getLatestUltraSrtFcstBaseDateTime();
        log.info("Starting syncUltraSrtFcst for baseDate: {}, baseTime: {}", baseDateTime[0], baseDateTime[1]);

        UltraSrtFcstResponseDto response = weatherApiClient.fetchUltraSrtFcst(baseDateTime[0], baseDateTime[1], DEFAULT_NX, DEFAULT_NY);
        if (isEmptyUltraSrtFcstResponse(response)) {
            log.warn("UltraSrtFcst API response is empty.");
            return;
        }

        Map<LocalDateTime, List<UltraSrtFcstResponseDto.Item>> timeGroupMap = groupUltraSrtFcstItems(response);
        if (timeGroupMap.isEmpty()) return;

        List<LocalDateTime> sortedTimes = timeGroupMap.keySet().stream().sorted().toList();
        LocalDateTime minTime = sortedTimes.get(0);
        LocalDateTime maxTime = sortedTimes.get(sortedTimes.size() - 1);

        Map<LocalDateTime, HourlyWeather> existingMap = hourlyWeatherRepository
                .findAllByLocationAndForecastAtBetweenOrderByForecastAtAsc(DEFAULT_LOCATION, minTime, maxTime)
                .stream()
                .collect(Collectors.toMap(HourlyWeather::getForecastAt, w -> w, (existing, replacement) -> existing));

        List<HourlyWeather> weatherListToSave = new ArrayList<>();

        for (Map.Entry<LocalDateTime, List<UltraSrtFcstResponseDto.Item>> entry : timeGroupMap.entrySet()) {
            LocalDateTime forecastAt = entry.getKey();
            List<UltraSrtFcstResponseDto.Item> itemsAtTime = entry.getValue();

            HourlyWeather existing = existingMap.get(forecastAt);
            HourlyWeather hourlyWeather = createOrPatchUltraSrtFcstWithExisting(forecastAt, itemsAtTime, existing);
            weatherListToSave.add(hourlyWeather);
        }

        hourlyWeatherRepository.saveAll(weatherListToSave);
        log.info("Successfully synced {} ultra short-term forecast records.", weatherListToSave.size());
    }

    // 3. 초단기실황 관측 정보 업데이트 (T1H, RN1, PTY, REH)
    @Transactional
    @CacheEvict(cacheNames = "weatherSummary", allEntries = true)
    public void syncUltraSrtNcst() {
        String[] baseDateTime = getLatestNcstBaseDateTime();
        log.info("Starting syncUltraSrtNcst for baseDate: {}, baseTime: {}", baseDateTime[0], baseDateTime[1]);

        UltraSrtNcstResponseDto response = weatherApiClient.fetchUltraSrtNcst(baseDateTime[0], baseDateTime[1], DEFAULT_NX, DEFAULT_NY);
        if (isEmptyNcstResponse(response)) {
            log.warn("UltraSrtNcst API response is empty.");
            return;
        }

        LocalDateTime forecastAt = LocalDateTime.parse(
                baseDateTime[0] + baseDateTime[1],
                DateTimeFormatter.ofPattern("yyyyMMddHHmm")
        );

        Double t1h = null;
        Integer reh = null;
        Integer pty = null;
        Double rn1 = null;

        for (UltraSrtNcstResponseDto.Item item : response.response().body().items().item()) {
            String val = item.obsrValue();
            switch (item.category()) {
                case "T1H" -> t1h = parseDouble(val);
                case "REH" -> reh = parseInt(val);
                case "PTY" -> pty = parseInt(val);
                case "RN1" -> rn1 = parsePrecipitation(val);
            }
        }

        final Double finalT1h = t1h;
        final Integer finalReh = reh;
        final Integer finalPty = pty;
        final Double finalRn1 = rn1;

        HourlyWeather hourlyWeather = hourlyWeatherRepository.findByLocationAndForecastAt(DEFAULT_LOCATION, forecastAt)
                .map(existing -> {
                    // PATCH 방식: non-null 값만 업데이트 및 기존 precipProbability/skyState 완전 보존
                    existing.patchUltraSrtNcst(finalT1h, finalReh, finalPty, finalRn1);
                    return existing;
                })
                .orElseGet(() -> HourlyWeather.builder()
                        .location(DEFAULT_LOCATION)
                        .forecastAt(forecastAt)
                        .temperature(finalT1h != null ? finalT1h : 0.0)
                        .humidity(finalReh)
                        .rainState(finalPty)
                        .precipitation(finalRn1)
                        .build());

        hourlyWeatherRepository.save(hourlyWeather);
        log.info("Successfully synced ultra short-term actual weather for {}", forecastAt);
    }

    private boolean isEmptyResponse(VillageFcstResponseDto response) {
        return response == null || response.response() == null 
                || response.response().body() == null 
                || response.response().body().items() == null
                || response.response().body().items().item() == null;
    }

    private boolean isEmptyNcstResponse(UltraSrtNcstResponseDto response) {
        return response == null || response.response() == null 
                || response.response().body() == null 
                || response.response().body().items() == null
                || response.response().body().items().item() == null;
    }

    private boolean isEmptyUltraSrtFcstResponse(UltraSrtFcstResponseDto response) {
        return response == null || response.response() == null 
                || response.response().body() == null 
                || response.response().body().items() == null
                || response.response().body().items().item() == null;
    }

    private Map<LocalDateTime, List<VillageFcstResponseDto.Item>> groupItemsByForecastTime(VillageFcstResponseDto response) {
        List<VillageFcstResponseDto.Item> items = response.response().body().items().item();
        Map<LocalDateTime, List<VillageFcstResponseDto.Item>> map = new HashMap<>();

        for (VillageFcstResponseDto.Item item : items) {
            LocalDateTime forecastAt = LocalDateTime.parse(
                    item.fcstDate() + item.fcstTime(),
                    DateTimeFormatter.ofPattern("yyyyMMddHHmm")
            );
            map.computeIfAbsent(forecastAt, k -> new ArrayList<>()).add(item);
        }
        return map;
    }

    private Map<LocalDateTime, List<UltraSrtFcstResponseDto.Item>> groupUltraSrtFcstItems(UltraSrtFcstResponseDto response) {
        List<UltraSrtFcstResponseDto.Item> items = response.response().body().items().item();
        Map<LocalDateTime, List<UltraSrtFcstResponseDto.Item>> map = new HashMap<>();

        for (UltraSrtFcstResponseDto.Item item : items) {
            LocalDateTime forecastAt = LocalDateTime.parse(
                    item.fcstDate() + item.fcstTime(),
                    DateTimeFormatter.ofPattern("yyyyMMddHHmm")
            );
            map.computeIfAbsent(forecastAt, k -> new ArrayList<>()).add(item);
        }
        return map;
    }

    private HourlyWeather createOrPatchVillageFcstWithExisting(LocalDateTime forecastAt, List<VillageFcstResponseDto.Item> itemsAtTime, HourlyWeather existing) {
        Double temp = null;
        Integer humidity = null;
        Integer sky = null;
        Integer pty = null;
        Integer pop = null;
        Double pcp = null;

        for (VillageFcstResponseDto.Item item : itemsAtTime) {
            String val = item.fcstValue();
            switch (item.category()) {
                case "TMP" -> temp = parseDouble(val);
                case "REH" -> humidity = parseInt(val);
                case "SKY" -> sky = parseInt(val);
                case "PTY" -> pty = parseInt(val);
                case "POP" -> pop = parseInt(val);
                case "PCP" -> pcp = parsePrecipitation(val);
            }
        }

        if (existing != null) {
            existing.patchVillageFcst(temp, humidity, sky, pty, pop, pcp);
            return existing;
        }

        return HourlyWeather.builder()
                .location(DEFAULT_LOCATION)
                .forecastAt(forecastAt)
                .temperature(temp != null ? temp : 0.0)
                .humidity(humidity)
                .skyState(sky)
                .rainState(pty)
                .precipProbability(pop)
                .precipitation(pcp)
                .build();
    }

    private HourlyWeather createOrPatchUltraSrtFcstWithExisting(LocalDateTime forecastAt, List<UltraSrtFcstResponseDto.Item> itemsAtTime, HourlyWeather existing) {
        Double t1h = null;
        Integer reh = null;
        Integer sky = null;
        Integer pty = null;
        Double rn1 = null;
        Boolean lgt = false;

        for (UltraSrtFcstResponseDto.Item item : itemsAtTime) {
            String val = item.fcstValue();
            switch (item.category()) {
                case "T1H" -> t1h = parseDouble(val);
                case "REH" -> reh = parseInt(val);
                case "SKY" -> sky = parseInt(val);
                case "PTY" -> pty = parseInt(val);
                case "RN1" -> rn1 = parsePrecipitation(val);
                case "LGT" -> lgt = parseDouble(val) != null && parseDouble(val) > 0;
            }
        }

        if (existing != null) {
            existing.patchUltraSrtFcst(t1h, reh, sky, pty, rn1, lgt);
            return existing;
        }

        return HourlyWeather.builder()
                .location(DEFAULT_LOCATION)
                .forecastAt(forecastAt)
                .temperature(t1h != null ? t1h : 0.0)
                .humidity(reh)
                .skyState(sky)
                .rainState(pty)
                .precipitation(rn1)
                .hasThunder(lgt)
                .build();
    }

    private String[] getLatestVillageBaseDateTime() {
        LocalDateTime now = LocalDateTime.now(KST_ZONE);
        int[] baseHours = {2, 5, 8, 11, 14, 17, 20, 23};
        
        LocalDateTime target = now;
        if (now.toLocalTime().isBefore(LocalTime.of(2, 10))) {
            target = now.minusDays(1);
            return new String[]{target.format(DateTimeFormatter.ofPattern("yyyyMMdd")), "2300"};
        }

        int targetHour = 23;
        for (int i = baseHours.length - 1; i >= 0; i--) {
            LocalTime baseTime = LocalTime.of(baseHours[i], 10);
            if (!now.toLocalTime().isBefore(baseTime)) {
                targetHour = baseHours[i];
                break;
            }
        }

        String baseDateStr = target.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String baseTimeStr = String.format("%02d00", targetHour);
        return new String[]{baseDateStr, baseTimeStr};
    }

    private String[] getLatestUltraSrtFcstBaseDateTime() {
        LocalDateTime now = LocalDateTime.now(KST_ZONE);
        if (now.getMinute() < 45) {
            now = now.minusHours(1);
        }
        String baseDateStr = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String baseTimeStr = String.format("%02d30", now.getHour());
        return new String[]{baseDateStr, baseTimeStr};
    }

    private String[] getLatestNcstBaseDateTime() {
        LocalDateTime now = LocalDateTime.now(KST_ZONE);
        if (now.getMinute() < 40) {
            now = now.minusHours(1);
        }
        String baseDateStr = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String baseTimeStr = String.format("%02d00", now.getHour());
        return new String[]{baseDateStr, baseTimeStr};
    }

    private Double parseDouble(String val) {
        try { return Double.parseDouble(val); } catch (Exception e) { return null; }
    }

    private Integer parseInt(String val) {
        try { return Integer.parseInt(val); } catch (Exception e) { return null; }
    }

    private Double parsePrecipitation(String val) {
        if (val == null || val.equals("강수없음")) return 0.0;
        val = val.replace("mm", "").trim();
        try { return Double.parseDouble(val); } catch (Exception e) { return 0.0; }
    }
}
