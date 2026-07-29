package life.hanyang.core.library.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import life.hanyang.core.library.domain.ReadingRoom;

import java.time.LocalDateTime;
import java.util.List;

public record AvailableSeatResponse (
        List<ReadingRoomSeatStatus> readingRooms,

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime updatedAt
) {
    public record ReadingRoomSeatStatus(
            ReadingRoom room,
            String roomName,
            int totalSeat,
            int availableSeats,
            int occupiedSeats
    ) {
        public static ReadingRoomSeatStatus of(ReadingRoom room, int totalSeats, int availableSeats, int occupiedSeats) {
            return new ReadingRoomSeatStatus(
                    room,
                    room.getName(),
                    totalSeats,
                    availableSeats,
                    occupiedSeats
            );
        }
    }
}
