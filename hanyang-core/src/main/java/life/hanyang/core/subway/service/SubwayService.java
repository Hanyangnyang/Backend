package life.hanyang.core.subway.service;

import life.hanyang.core.subway.domain.*;
import life.hanyang.core.subway.dto.SubwayScheduleApiResponse;
import life.hanyang.core.subway.dto.SubwaySearchRequest;
import life.hanyang.core.subway.dto.SubwayTimetableResponse;
import life.hanyang.core.subway.repository.SubwayRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubwayService {
    private final SubwayRepository subwayRepository;
    private final RestClient restClient;

    @Value("${api.public-data-key}")
    private String apiKey;

    @Transactional
    public void replaceTimetable(SubwayStation station) {
        List<SubwayTimetable> totalTimetables = new ArrayList<>();

        for (SubwayLine line : SubwayLine.values()) {
            for (SubwayDirection direction : SubwayDirection.values()) {
                for (SubwayDayType dayType : SubwayDayType.values()) {
                    
                    if (dayType == SubwayDayType.HOLIDAY) {
                        continue;
                    }

                    try {
                        List<SubwayTimetable> result = fetchTimetableFromApi(station, line, direction, dayType);
                        totalTimetables.addAll(result);
                    } catch (Exception e) {
                        log.error("지하철 시간표 API 호출 실패 - 노선: {}, 방향: {}, 요일: {}", 
                                line.getApiValue(), direction.getApiValue(), dayType.getApiValue(), e);
                    }
                }
            }
        }

        if (!totalTimetables.isEmpty()) {
            subwayRepository.deleteBySubwayStationInBatch(station);
            subwayRepository.saveAll(totalTimetables);
            log.info("지하철 시간표 동기화 완료 - 총 {}건 적재", totalTimetables.size());
        } else {
            throw new IllegalStateException("동기화할 지하철 시간표 데이터가 전혀 존재하지 않습니다.");
        }
    }

    private List<SubwayTimetable> fetchTimetableFromApi(SubwayStation station, SubwayLine line, 
                                                        SubwayDirection direction, SubwayDayType dayType) {
        
        String encodedStnNm = URLEncoder.encode(station.getApiValue(), StandardCharsets.UTF_8);
        String encodedLineNm = URLEncoder.encode(line.getApiValue(), StandardCharsets.UTF_8);
        String encodedDirection = URLEncoder.encode(direction.getApiValue(), StandardCharsets.UTF_8);
        String encodedDayType = URLEncoder.encode(dayType.getApiValue(), StandardCharsets.UTF_8);

        String url = String.format(
                "https://apis.data.go.kr/B553766/schedule/getTrainSch" +
                        "?serviceKey=%s" +
                        "&pageNo=1" +
                        "&numOfRows=1000" +
                        "&stnNm=%s" +
                        "&lineNm=%s" +
                        "&upbdnbSe=%s" +
                        "&wkndSe=%s" +
                        "&tmprTmtblYn=N" +
                        "&dataType=JSON",
                apiKey, encodedStnNm, encodedLineNm, encodedDirection, encodedDayType
        );

        URI uri = URI.create(url);
        log.info("지하철 시간표 API 호출 URI: {}", uri);

        SubwayScheduleApiResponse apiResponse = restClient.get()
                .uri(uri)
                .retrieve()
                .body(SubwayScheduleApiResponse.class);

        List<SubwayTimetable> list = new ArrayList<>();
        if (apiResponse == null || apiResponse.getResponse() == null || apiResponse.getResponse().getBody() == null) {
            log.warn("API 응답 구조가 비어있습니다. (Response or Body is null)");
            return list;
        }

        List<SubwayScheduleApiResponse.TrainScheduleItem> items = apiResponse.getResponse().getBody().getItems().getItem();
        log.info("지하철 시간표 API 응답 건수: {}건 (노선: {}, 방향: {}, 요일: {})", 
                items != null ? items.size() : 0, line.getApiValue(), direction.getApiValue(), dayType.getApiValue());

        if (items == null || items.isEmpty()) {
            return list;
        }

        for (SubwayScheduleApiResponse.TrainScheduleItem item : items) {
            // 도착 시간을 우선으로 하되, 없으면 출발 시간을 사용 (시발 열차 대응)
            String rawTime = item.getTrainArvlTm();
            if (rawTime == null || rawTime.isBlank()) {
                rawTime = item.getTrainDptreTm();
            }

            LocalTime time = parseLocalTime(rawTime);
            if (time == null) {
                continue;
            }

            SubwayTimetable timetable = SubwayTimetable.builder()
                    .subwayStation(station)
                    .subwayLine(line)
                    .direction(direction)
                    .subwayDayType(dayType)
                    .time(time)
                    .destination(item.getArvlStnNm())
                    .trainNo(item.getTrainno())
                    .build();

            list.add(timetable);
        }

        return list;
    }

    private LocalTime parseLocalTime(String rawTime) {
        if (rawTime == null || rawTime.isBlank()) {
            return null;
        }
        if (rawTime.startsWith("24:")) {
            rawTime = "00:" + rawTime.substring(3);
        }
        return LocalTime.parse(rawTime, DateTimeFormatter.ofPattern("HH:mm:ss"));
    }

    public List<SubwayTimetableResponse> getTimetable(SubwaySearchRequest request) {
        List<SubwayTimetable> timetables = subwayRepository.findBySubwayStationAndSubwayLineAndDirectionAndSubwayDayTypeOrderByTimeAsc(
                request.subwayStation(),
                request.subwayLine(),
                request.direction(),
                request.dayType()
        );
        return timetables.stream()
                .map(SubwayTimetableResponse::from)
                .toList();
    }
}
