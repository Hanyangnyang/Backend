package life.hanyang.admin.gym.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import life.hanyang.core.global.response.ApiResponse;
import life.hanyang.core.gym.dto.GymPeriodRequest;
import life.hanyang.core.gym.dto.GymScheduleCellDto;
import life.hanyang.core.gym.dto.GymScheduleCreateRequest;
import life.hanyang.core.gym.dto.GymScheduleUpdateDto; // 👈 추가
import life.hanyang.core.gym.service.GymPeriodService;
import life.hanyang.core.gym.service.GymScheduleCellService;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.Response;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/api/v1/admin/gym")
@RestController
@Tag(name = "(관리자용) 체대 헬스장 시간표 관리 API", description = "체대 헬스장 시간표를 관리하는데 사용합니다.")
@RequiredArgsConstructor
public class GymAdminController {
    private final GymScheduleCellService gymScheduleCellService;
    private final GymPeriodService gymPeriodService;

    @Operation(summary = "특정 학기(기간)에 스케줄을 추가합니다. (여러개 가능)")
    @PostMapping("/schedules-cells")
    public ResponseEntity<ApiResponse<Void>> createScheduleCells(@RequestBody List<GymScheduleCreateRequest> requests) {
        gymScheduleCellService.saveSchedules(requests);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success());
    }

    @Operation(summary = "특정 스케줄을 삭제합니다. (여러개 가능)")
    @DeleteMapping("/schedules-cells")
    public ResponseEntity<ApiResponse<Void>> deleteScheduleCells(
            @RequestParam("scheduleCellIds") List<Long> scheduleCellIds){
        gymScheduleCellService.deleteSchedules(scheduleCellIds);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Operation(summary = "특정 스케줄의 내용을 변경합니다.")
    @PatchMapping("/schedules-cells/{cellId}")
    public ResponseEntity<ApiResponse<Void>> updateScheduleCell(
            @PathVariable Long cellId,
            @RequestBody GymScheduleCellDto dto
    ) {
        gymScheduleCellService.updateSchedule(cellId, dto);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Operation(summary = "여러 스케줄의 내용을 한꺼번에 변경합니다. (일괄 수정)")
    @PatchMapping("/schedules-cells")
    public ResponseEntity<ApiResponse<Void>> updateScheduleCells(
            @RequestBody List<GymScheduleUpdateDto> requests
    ) {
        gymScheduleCellService.updateSchedules(requests);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Operation(summary = "새로운 학기를 정보를 추가합니다.")
    @PostMapping("/period")
    public ResponseEntity<ApiResponse<Void>> createPeriod(
            @Valid @RequestBody GymPeriodRequest request){
        gymPeriodService.savePeriod(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success());
    }

    @Operation(summary = "특정 학기 정보와 해당 학기 시간표 정보를 삭제합니다.")
    @DeleteMapping("/period/{periodId}")
    public ResponseEntity<ApiResponse<Void>> deletePeriod(
            @PathVariable Long periodId){
        gymPeriodService.deletePeriod(periodId);
        return ResponseEntity.ok(ApiResponse.success());
    }
}
