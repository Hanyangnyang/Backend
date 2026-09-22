package life.hanyang.admin.subway.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import life.hanyang.admin.subway.dto.SubwayScheduleRequest;
import life.hanyang.core.global.response.ApiResponse;
import life.hanyang.core.subway.dto.SubwayTimetableDeleteRequest;
import life.hanyang.core.subway.dto.SubwayTimetableImportRequest;
import life.hanyang.core.subway.service.SubwayService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.DeleteMapping;

@RequestMapping("/api/v1/admin/subway")
@RestController
@RequiredArgsConstructor
@Tag(name = "(관리자용) 지하철 API", description = "지하철 정보를 관리합니다.")
public class SubwayAdminController {
    private final SubwayService subwayService;

    @Operation(summary = "해당 지하철역의 시간표를 삭제한 뒤 외부 API로 다시 동기화합니다.")
    @PostMapping("sync")
    public ResponseEntity<ApiResponse<Void>> resetTimetables(@RequestBody @Valid SubwayScheduleRequest request) {
        subwayService.replaceTimetable(request.station());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(summary = "JSON으로 지하철 시간표를 추가합니다.")
    @PostMapping("/timetables")
    public ResponseEntity<ApiResponse<Void>> addTimetables(
            @RequestBody @Valid SubwayTimetableImportRequest request) {
        subwayService.addTimetables(request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(summary = "해당 역·노선의 지하철 시간표를 전체 교체합니다.")
    @PutMapping("/timetables")
    public ResponseEntity<ApiResponse<Void>> replaceTimetables(
            @RequestBody @Valid SubwayTimetableImportRequest request) {
        subwayService.replaceTimetables(request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(summary = "지정한 조건과 일치하는 지하철 시간표를 삭제합니다.")
    @DeleteMapping("/timetables")
    public ResponseEntity<ApiResponse<Long>> deleteTimetables(
            @RequestBody SubwayTimetableDeleteRequest request) {
        return ResponseEntity.ok(ApiResponse.success(subwayService.deleteTimetables(request)));
    }
}
