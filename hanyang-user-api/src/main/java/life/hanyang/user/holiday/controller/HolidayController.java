package life.hanyang.user.holiday.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import life.hanyang.core.global.response.ApiResponse;
import life.hanyang.core.holiday.dto.DateInfoResponse;
import life.hanyang.core.holiday.service.HolidayService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/holidays")
@RequiredArgsConstructor
@Tag(name = "날짜/휴일 정보 조회 API", description = "평일, 주말, 공휴일, 미운행 상태 단건 조회 API")
public class HolidayController {

    private final HolidayService holidayService;

    @Operation(summary = "특정 날짜의 평일/주말/공휴일/미운행 상태 조회 (미입력 시 오늘 날짜)")
    @GetMapping("/date-info")
    public ResponseEntity<ApiResponse<DateInfoResponse>> getDateInfo(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        LocalDate targetDate = (date != null) ? date : LocalDate.now();
        return ResponseEntity.ok(ApiResponse.success(holidayService.getDateInfo(targetDate)));
    }
}
