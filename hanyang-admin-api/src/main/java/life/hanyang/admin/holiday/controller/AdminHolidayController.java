package life.hanyang.admin.holiday.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import life.hanyang.core.global.response.ApiResponse;
import life.hanyang.core.holiday.dto.HolidayCreateRequest;
import life.hanyang.core.holiday.dto.HolidayResponse;
import life.hanyang.core.holiday.dto.HolidayUpdateRequest;
import life.hanyang.core.holiday.service.HolidayService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/holidays")
@RequiredArgsConstructor
@Tag(name = "(관리자용) 공휴일/일정 관리 API")
public class AdminHolidayController {

    private final HolidayService holidayService;

    @Operation(summary = "공휴일 수동 등록")
    @PostMapping
    public ResponseEntity<ApiResponse<HolidayResponse>> createHoliday(
            @Valid @RequestBody HolidayCreateRequest request
    ) {
        HolidayResponse response = holidayService.createHoliday(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @Operation(summary = "공휴일 정보 및 상태 수정 (예: HOLIDAY -> NO_OPERATION 변경)")
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<HolidayResponse>> updateHoliday(
            @PathVariable Long id,
            @RequestBody HolidayUpdateRequest request
    ) {
        HolidayResponse response = holidayService.updateHoliday(id, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "공휴일 삭제")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteHoliday(@PathVariable Long id) {
        holidayService.deleteHoliday(id);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Operation(summary = "공휴일 목록 조회")
    @GetMapping
    public ResponseEntity<ApiResponse<List<HolidayResponse>>> getHolidays(
            @RequestParam int year,
            @RequestParam(required = false) Integer month
    ) {
        return ResponseEntity.ok(ApiResponse.success(holidayService.getHolidays(year, month)));
    }

    @Operation(summary = "공공데이터포털 공휴일 수동 동기화 트리거")
    @PostMapping("/sync")
    public ResponseEntity<ApiResponse<Integer>> syncHolidays(
            @RequestParam(required = false) Integer year
    ) {
        int targetYear = (year != null) ? year : LocalDate.now().getYear();
        int syncedCount = holidayService.syncHolidays(targetYear);
        return ResponseEntity.ok(ApiResponse.success(syncedCount));
    }
}
