package life.hanyang.user.academic.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import life.hanyang.core.academic.dto.UnifiedOperationStatusResponse;
import life.hanyang.core.academic.service.AcademicPeriodService;
import life.hanyang.core.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.ZoneId;

@RestController
@RequestMapping("/api/v1/academic")
@RequiredArgsConstructor
@Tag(name = "학사 및 셔틀/시설 통합 운영 상태 API", description = "날짜별 달력/공휴일, 학사 일정, 셔틀 운행 기준 통합 조회 API")
public class AcademicController {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private final AcademicPeriodService academicPeriodService;

    @Operation(
            summary = "통합 운영 상태 조회 (달력/학사/셔틀)",
            description = "특정 날짜의 공휴일 여부, 현재 학기 구분(학기중/방학중/계절학기), 셔틀 운행 기준(평일/주말/미운행)을 통합 조회합니다.\n\n" +
                    "• **date**: 조회 대상 날짜 (미입력 시 한국 시간 기준 오늘 날짜)"
    )
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<UnifiedOperationStatusResponse>> getUnifiedOperationStatus(
            @Parameter(description = "조회 날짜 (YYYY-MM-DD)", example = "2026-08-28")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        LocalDate targetDate = (date != null) ? date : LocalDate.now(KST);
        UnifiedOperationStatusResponse response = academicPeriodService.getUnifiedOperationStatus(targetDate);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
