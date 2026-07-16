package life.hanyang.core.subway.service;

import life.hanyang.core.subway.domain.*;
import life.hanyang.core.subway.dto.SubwayScheduleApiResponse;
import life.hanyang.core.subway.repository.SubwayRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
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

    @Value("${subway.api-key}")
    private String apiKey;

    @Transactional
    public void replaceTimetable(SubwayStation station) {
        List<SubwayTimetable> totalTimetables = new ArrayList<>();

        for (SubwayLine line : SubwayLine.values()) {
            for (SubwayDirection direction : SubwayDirection.values()) {
                for (SubwayDayType dayType : SubwayDayType.values()) {

                    // HOLIDAY(공휴일) 타입은 호출해도 0건이 호출되므로 루프에서 스킵 처리
                    if (dayType == SubwayDayType.HOLIDAY) {
                        continue;
                    }

                    try {
                        List<SubwayTimetable> result = fetchTimetableFromApi(station, line, direction, dayType);
                        totalTimetables.addAll(result);
                    } catch (Exception e) {
                        log.error("지하철 시간표 API 호출 실패 - 노선: {}, 방향: {}, 요일: {}. 에러: {}", 
                                line.getApiValue(), direction.getApiValue(), dayType.getApiValue(), e.getMessage());
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
        
        URI uri = UriComponentsBuilder.fromUriString("https://apis.data.go.kr/B553766/schedule/getTrainSch")
                .queryParam("serviceKey", apiKey)
                .queryParam("pageNo", 1)
                .queryParam("numOfRows", 1000)
                .queryParam("stnNm", station.getApiValue())
                .queryParam("lineNm", line.getApiValue())
                .queryParam("upbdnbSe", direction.getApiValue())
                .queryParam("wkndSe", dayType.getApiValue())
                .queryParam("tmprTmtblYn", "N")
                .queryParam("_type", "json")
                .build(true)
                .toUri();

        SubwayScheduleApiResponse apiResponse = restClient.get()
                .uri(uri)
                .retrieve()
                .body(SubwayScheduleApiResponse.class);

        List<SubwayTimetable> list = new ArrayList<>();
        if (apiResponse == null || apiResponse.getResponse() == null || apiResponse.getResponse().getBody() == null) {
            return list;
        }

        List<SubwayScheduleApiResponse.TrainScheduleItem> items = apiResponse.getResponse().getBody().getItems().getItem();
        if (items == null || items.isEmpty()) {
            return list;
        }

        for (SubwayScheduleApiResponse.TrainScheduleItem item : items) {
            LocalTime departureTime = LocalTime.parse(item.getTrainDptreTm(), DateTimeFormatter.ofPattern("HH:mm:ss"));

            SubwayTimetable timetable = SubwayTimetable.builder()
                    .subwayStation(station)
                    .subwayLine(line)
                    .direction(direction)
                    .subwayDayType(dayType)
                    .time(departureTime)
                    .destination(item.getArvlStnNm())
                    .trainNo(item.getTrainno())
                    .build();

            list.add(timetable);
        }

        return list;
    }
}
