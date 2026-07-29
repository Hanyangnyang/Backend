package life.hanyang.user.library.controller;

import io.swagger.v3.oas.annotations.Operation;
import life.hanyang.core.library.dto.AvailableSeatResponse;
import life.hanyang.core.library.service.LibraryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/library")
@RequiredArgsConstructor
public class LibraryController {
    private final LibraryService libraryService;

    @Operation(summary = "도서관 좌석 현황을 불러옵니다.")
    @GetMapping("/seats")
    public AvailableSeatResponse getReadingRoomSeats() {
        return libraryService.getReadingRoomSeats();
    }
}
