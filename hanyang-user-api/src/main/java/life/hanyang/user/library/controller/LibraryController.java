package life.hanyang.user.library.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import life.hanyang.core.global.response.ApiResponse;
import life.hanyang.core.library.dto.AvailableSeatResponse;
import life.hanyang.core.library.service.LibraryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/library")
@Tag(name = "도서관 API", description = "도서관 관련 정보를 조회합니다.")
@RequiredArgsConstructor
public class LibraryController {
    private final LibraryService libraryService;

    @Operation(summary = "도서관 좌석 현황을 불러옵니다.")
    @GetMapping("/seats")
    public ResponseEntity<ApiResponse<AvailableSeatResponse>> getReadingRoomSeats() {
        return ResponseEntity.ok(ApiResponse.success(libraryService.getReadingRoomSeats()));
    }
}
