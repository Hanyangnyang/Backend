package life.hanyang.admin.shuttle.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import life.hanyang.core.global.response.ApiResponse;
import life.hanyang.core.shuttle.dto.ShuttleTimetableRequest;
import life.hanyang.core.shuttle.dto.ShuttleTimetableResponse;
import life.hanyang.core.shuttle.service.ShuttleTimetableService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RequestMapping("/api/v1/shuttle")
@RestController
@Tag(name = "(관리자용) 셔틀 API", description = "셔틀 정보를 관리합니다.")
@RequiredArgsConstructor
public class ShuttleAdminController {
    private final ShuttleTimetableService shuttleTimetableService;
    private final ObjectMapper objectMapper;

    @Operation(summary = "모든 셔틀 정보를 삭제하고 입력한 파일에 있는 정보를 추가합니다.")
    @PostMapping("reset-reload")
    public ResponseEntity<ApiResponse<Void>> uploadTimetables(
            @RequestParam("file") MultipartFile file) {
        try {
            List<ShuttleTimetableRequest> requests = objectMapper.readValue(
                    file.getInputStream(),
                    new TypeReference<List<ShuttleTimetableRequest>>() {
                    }
            );
            shuttleTimetableService.createAllTimetable(requests);
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success());
        } catch (IOException e) {
            throw new IllegalArgumentException("JSON 파일 파싱 중 에러가 발생했습니다: " + e.getMessage());
        }
    }

    @Operation(summary = "입력한 셔틀 스케줄들을 삭제합니다.")
    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> deleteTimetable(
            @RequestParam("ids") List<Long> ids) {
        shuttleTimetableService.deleteTimetable(ids);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Operation(summary = "모든 셔틀 스케줄을 삭제합니다.")
    @DeleteMapping("all")
    public ResponseEntity<ApiResponse<Void>> deleteAllTimetables() {
        shuttleTimetableService.deleteAllTimetable();
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Operation(summary = "개별 셔틀 정보를 추가합니다.")
    @PostMapping
    public ResponseEntity<ApiResponse<ShuttleTimetableResponse>> createTimetable(
            @RequestBody ShuttleTimetableRequest request) {
        ShuttleTimetableResponse response = shuttleTimetableService.createTimetable(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @Operation(summary = "특정 셔틀 정보를 수정합니다.")
    @PutMapping("{id}")
    public ResponseEntity<ApiResponse<ShuttleTimetableResponse>> updateTimetable(
            @PathVariable Long id,
            @RequestBody ShuttleTimetableRequest request) {
        ShuttleTimetableResponse response = shuttleTimetableService.updateTimetable(id, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
