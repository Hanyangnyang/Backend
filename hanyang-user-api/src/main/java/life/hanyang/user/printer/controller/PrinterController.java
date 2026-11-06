package life.hanyang.user.printer.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import life.hanyang.core.campusmap.domain.Campus;
import life.hanyang.core.global.response.ApiResponse;
import life.hanyang.core.printer.dto.PrinterDto.*;
import life.hanyang.core.printer.service.PrinterService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/campus-map")
@RequiredArgsConstructor
@Tag(name = "프린터 API")
public class PrinterController {
    private final PrinterService service;

    @Operation(summary = "프린터 목록 조회", description = "철거·삭제된 프린터는 제외합니다. campus는 SEOUL 또는 ANSAN, buildingId는 문자열입니다.")
    @GetMapping("/printers")
    public ApiResponse<Page<Response>> list(@RequestParam(required = false) Campus campus,
                                           @RequestParam(required = false) String buildingId,
                                           @RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(service.list(campus, buildingId, page, size));
    }

    @Operation(summary = "프린터 상세 조회", description = "철거된 프린터도 removed=true로 조회되며, 삭제된 프린터는 404입니다.")
    @GetMapping("/printers/{id}")
    public ApiResponse<Response> get(@PathVariable UUID id) { return ApiResponse.success(service.get(id)); }

    @Operation(summary = "신규 프린터 제보", description = "기기 ID 기반 접수이며 로그인 인증이 아닙니다. 건물·세부 위치는 필수, 나머지는 아는 정보만 입력합니다. 승인 전에는 공개되지 않습니다.")
    @PostMapping("/printer-reports")
    public ResponseEntity<ApiResponse<Receipt>> reportNew(@Valid @RequestBody NewReportRequest request) {
        return ResponseEntity.status(201).body(ApiResponse.success(service.reportNew(request)));
    }

    @Operation(summary = "프린터 정보 수정·철거 제보", description = "기기 ID, 상세 조회의 version을 expectedVersion으로 전달합니다. changes에는 변경 항목만 입력하며 broken/removed는 생략과 false를 구분합니다.")
    @PostMapping("/printers/{id}/reports")
    public ResponseEntity<ApiResponse<Receipt>> reportUpdate(@PathVariable UUID id, @Valid @RequestBody UpdateReportRequest request) {
        return ResponseEntity.status(201).body(ApiResponse.success(service.reportUpdate(id, request)));
    }
}
