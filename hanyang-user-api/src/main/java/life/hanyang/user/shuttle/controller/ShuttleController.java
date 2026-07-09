package life.hanyang.user.shuttle.controller;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import life.hanyang.core.global.response.ApiResponse;
import life.hanyang.core.shuttle.domain.ShuttleDayType;
import life.hanyang.core.shuttle.domain.ShuttlePeriod;
import life.hanyang.core.shuttle.dto.ShuttleTimetableResponse;
import life.hanyang.core.shuttle.service.ShuttleTimetableService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/api/v1/admin/shuttle")
@RestController
@Tag(name = "셔틀 API", description = "셔틀 정보를 제공합니다.")
@RequiredArgsConstructor
public class ShuttleController {
    private final ShuttleTimetableService shuttleTimetableService;

    @Operation(summary = "셔틀 버스 시간표의 모든 정보를 가져옵니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<ShuttleTimetableResponse>>> getAllTimetables() {
        List<ShuttleTimetableResponse> responses = shuttleTimetableService.getAllTimetable().stream()
                .map(ShuttleTimetableResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @Operation(summary = "셔틀버스 시간표를 기간과 휴무일에 따라 분류하여 가져옵니다.")
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<ShuttleTimetableResponse>>> searchTimetables(
            @RequestParam("period") ShuttlePeriod period,
            @RequestParam("dayType") ShuttleDayType dayType
    ) {
        List<ShuttleTimetableResponse> responses = shuttleTimetableService.getTimetablesByPeriodAndType(period, dayType).stream()
                .map(ShuttleTimetableResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(responses));
    }


}
