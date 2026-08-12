package life.hanyang.core.library.service;

import life.hanyang.core.library.client.LibraryApiClient;
import life.hanyang.core.library.domain.ReadingRoom;
import life.hanyang.core.library.dto.AvailableSeatResponse;
import life.hanyang.core.library.dto.AvailableSeatResponse.ReadingRoomSeatStatus;
import life.hanyang.core.library.dto.PyxisSeatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LibraryService {
    private final LibraryApiClient libraryApiClient;

    /**
     * 유저 조회 전용: 무조건 Redis 캐시에서 초고속 리턴 (100% Cache Hit)
     */
    @Cacheable(value = "readingRoomSeats", key = "'all'")
    public AvailableSeatResponse getReadingRoomSeats() {
        AvailableSeatResponse response = refreshReadingRoomSeats();
        if (response == null) {
            return new AvailableSeatResponse(List.of(), LocalDateTime.now());
        }
        return response;
    }

    /**
     * 스케줄러가 주기적(3분)으로 호출하는 캐시 갱신 전용 메서드
     * 외부 API 장애 발생 시 null을 반환하여 기존 유효 캐시를 보호함
     */
    @CachePut(
            value = "readingRoomSeats",
            key = "'all'",
            unless = "#result == null || #result.readingRooms() == null || #result.readingRooms().isEmpty()"
    )
    public AvailableSeatResponse refreshReadingRoomSeats() {
        try {
            PyxisSeatResponse pyxisResponse = libraryApiClient.fetchSeatStatus();

            if (pyxisResponse == null || pyxisResponse.data() == null || pyxisResponse.data().list() == null) {
                log.warn("[LibraryService] 외부 도서관 API 응답 데이터가 null 입니다.");
                return null;
            }

            List<ReadingRoomSeatStatus> seatStatuses = pyxisResponse.data().list().stream()
                    .map(item -> {
                        ReadingRoom room = ReadingRoom.fromExternalId(item.id());
                        var seats = item.seats();
                        return ReadingRoomSeatStatus.of(
                                room,
                                seats.total(),
                                seats.available(),
                                seats.occupied()
                        );
                    })
                    .toList();

            return new AvailableSeatResponse(seatStatuses, LocalDateTime.now());
        } catch (Exception e) {
            log.warn("[LibraryService] 도서관 API 동기화 중 에러 발생: {}", e.getMessage());
            return null;
        }
    }
}
