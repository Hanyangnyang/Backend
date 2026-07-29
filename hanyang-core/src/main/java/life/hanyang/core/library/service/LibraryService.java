package life.hanyang.core.library.service;

import life.hanyang.core.library.client.LibraryApiClient;
import life.hanyang.core.library.domain.ReadingRoom;
import life.hanyang.core.library.dto.AvailableSeatResponse;
import life.hanyang.core.library.dto.AvailableSeatResponse.ReadingRoomSeatStatus;
import life.hanyang.core.library.dto.PyxisSeatResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LibraryService {
    private final LibraryApiClient libraryApiClient;

    @Cacheable(
            value = "readingRoomSeats",
            unless = "#result.readingRooms() == null || #result.readingRooms().isEmpty()"
    )

    public AvailableSeatResponse getReadingRoomSeats() {
        PyxisSeatResponse pyxisResponse = libraryApiClient.fetchSeatStatus();

        if (pyxisResponse == null || pyxisResponse.data() == null) {
            return new AvailableSeatResponse(List.of(),null);
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
    }
}
