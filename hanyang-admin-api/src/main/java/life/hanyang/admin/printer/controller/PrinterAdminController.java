package life.hanyang.admin.printer.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.PositiveOrZero;
import life.hanyang.core.global.response.ApiResponse;
import life.hanyang.core.campusmap.domain.Campus;
import life.hanyang.core.printer.domain.*;
import life.hanyang.core.printer.dto.PrinterDto.*;
import life.hanyang.core.printer.service.PrinterService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/campus-map")
@RequiredArgsConstructor
@Tag(name = "프린터 관리자 API")
public class PrinterAdminController {
    private final PrinterService service;

    @Operation(summary = "관리자 프린터 목록 조회", description = "removed 생략 시 철거 여부와 관계없이 조회하며, true는 철거, false는 미철거 목록입니다. 삭제된 프린터는 제외합니다.")
    @GetMapping("/printers")
    public ApiResponse<Page<Response>> list(
            @RequestParam(required = false) Campus campus,
            @RequestParam(required = false) String buildingId,
            @RequestParam(required = false) Boolean removed,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(service.listAdmin(campus, buildingId, removed, page, size));
    }

    @Operation(summary = "프린터 등록")
    @PostMapping("/printers")
    public ResponseEntity<ApiResponse<Response>> create(@Valid @RequestBody Input request) {
        return ResponseEntity.status(201).body(ApiResponse.success(service.create(request)));
    }

    @Operation(summary = "프린터 일부 수정", description = "expectedVersion 필수. 생략 필드는 유지, nullable 필드의 null은 값 삭제입니다.")
    @PatchMapping("/printers/{id}")
    public ApiResponse<Response> update(@PathVariable UUID id, @Valid @RequestBody UpdateRequest request) {
        return ApiResponse.success(service.update(id, request));
    }

    @Operation(summary = "프린터 삭제", description = "이력을 보존하며 공개 조회에서 제외합니다.")
    @DeleteMapping("/printers/{id}")
    public ApiResponse<Void> delete(@PathVariable UUID id, @RequestParam @PositiveOrZero long expectedVersion) {
        service.delete(id, expectedVersion);
        return ApiResponse.success();
    }

    @Operation(summary = "프린터 제보 목록 조회")
    @GetMapping("/printer-reports")
    public ApiResponse<Page<ReportSummary>> listReports(
            @RequestParam(required = false) PrinterReportStatus status,
            @RequestParam(required = false) PrinterReportType type,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(service.listReports(status, type, page, size));
    }

    @Operation(summary = "프린터 제보 상세 조회", description = "제보 당시 정보와 현재 정보 및 버전을 함께 반환합니다.")
    @GetMapping("/printer-reports/{id}")
    public ApiResponse<ReportDetail> getReport(@PathVariable UUID id) {
        return ApiResponse.success(service.getReport(id));
    }

    @Operation(summary = "프린터 제보 승인", description = "NEW는 최종 printer, UPDATE는 검토한 expectedVersion과 최종 changes를 제출합니다.")
    @PostMapping("/printer-reports/{id}/approve")
    public ApiResponse<ReportDetail> approve(@PathVariable UUID id, @Valid @RequestBody ApproveRequest request, Principal principal) {
        return ApiResponse.success(service.approve(id, request, principal == null ? null : principal.getName()));
    }

    @Operation(summary = "프린터 제보 반려")
    @PostMapping("/printer-reports/{id}/reject")
    public ApiResponse<ReportDetail> reject(@PathVariable UUID id, @Valid @RequestBody RejectRequest request, Principal principal) {
        return ApiResponse.success(service.reject(id, request, principal == null ? null : principal.getName()));
    }
}
