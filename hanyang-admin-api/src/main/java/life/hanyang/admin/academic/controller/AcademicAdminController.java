package life.hanyang.admin.academic.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import life.hanyang.core.academic.dto.AcademicPeriodCreateRequest;
import life.hanyang.core.academic.dto.AcademicPeriodResponse;
import life.hanyang.core.academic.dto.AcademicPeriodUpdateRequest;
import life.hanyang.core.academic.service.AcademicPeriodService;
import life.hanyang.core.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/academic-periods")
@RequiredArgsConstructor
@Tag(name = "관리자 학사 일정 관리 API", description = "학사 일정(학기중/방학중/계절학기) 등록, 수정, 삭제, 목록 조회")
public class AcademicAdminController {

    private final AcademicPeriodService academicPeriodService;

    @Operation(summary = "학사 일정 목록 조회", description = "등록된 학사 일정 목록을 조회합니다. 연도별 필터링을 지원합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<AcademicPeriodResponse>>> getPeriods(
            @Parameter(description = "연도 (미입력 시 전체 조회)", example = "2026")
            @RequestParam(required = false) Integer year
    ) {
        List<AcademicPeriodResponse> responses = academicPeriodService.getPeriods(year);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @Operation(summary = "학사 일정 단건 상세 조회", description = "특정 학사 일정의 상세 정보를 조회합니다.")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AcademicPeriodResponse>> getPeriod(
            @PathVariable Long id
    ) {
        AcademicPeriodResponse response = academicPeriodService.getPeriod(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "학사 일정 신규 등록", description = "새로운 학사 일정을 등록합니다.")
    @PostMapping
    public ResponseEntity<ApiResponse<AcademicPeriodResponse>> createPeriod(
            @Valid @RequestBody AcademicPeriodCreateRequest request
    ) {
        AcademicPeriodResponse response = academicPeriodService.createPeriod(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @Operation(summary = "학사 일정 수정", description = "기존 학사 일정 정보를 수정합니다.")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AcademicPeriodResponse>> updatePeriod(
            @PathVariable Long id,
            @Valid @RequestBody AcademicPeriodUpdateRequest request
    ) {
        AcademicPeriodResponse response = academicPeriodService.updatePeriod(id, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "학사 일정 삭제", description = "특정 학사 일정을 삭제합니다.")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePeriod(
            @PathVariable Long id
    ) {
        academicPeriodService.deletePeriod(id);
        return ResponseEntity.ok(ApiResponse.success());
    }
}
