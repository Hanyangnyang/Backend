package life.hanyang.admin.subway.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import life.hanyang.admin.subway.dto.SubwayScheduleRequest;
import life.hanyang.core.global.response.ApiResponse;
import life.hanyang.core.subway.service.SubwayService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/api/v1/subway")
@RestController
@RequiredArgsConstructor
@Tag(name = "(관리자용) 지하철 API", description = "지하철 정보를 관리합니다.")
public class SubwayAdminController {
    private final SubwayService subwayService;

    @Operation(summary = "모든 지하철 정보를 삭제하고 외부 API로 새롭게 정보를 받아옵니다.")
    @PostMapping("reset")
    public ResponseEntity<ApiResponse<Void>> resetTimetables(@RequestBody @Valid SubwayScheduleRequest request) {
        subwayService.replaceTimetable(request.station());
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
