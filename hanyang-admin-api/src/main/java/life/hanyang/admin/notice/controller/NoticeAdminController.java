package life.hanyang.admin.notice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import life.hanyang.core.global.response.ApiResponse;
import life.hanyang.core.notice.dto.NoticeDto.*;
import life.hanyang.core.notice.service.NoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/notices")
@RequiredArgsConstructor
@Tag(name = "(관리자용) 공지 관리 API")
public class NoticeAdminController {
    private final NoticeService notices;

    @GetMapping("/target-options")
    @Operation(summary = "설정 가능한 공지 페이지와 조건 값을 조회합니다.")
    public ApiResponse<List<PageOption>> options() { return ApiResponse.success(notices.options()); }

    @PostMapping
    @Operation(summary = "공지와 노출 조건을 등록합니다.")
    public ResponseEntity<ApiResponse<AdminResponse>> create(@RequestBody Input input) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(notices.create(input)));
    }

    @GetMapping
    @Operation(summary = "공지 목록을 게시일 최신순으로 조회합니다.")
    public ApiResponse<Page<AdminResponse>> list(@RequestParam(required = false) String pageKey,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(notices.listAdmin(pageKey, enabled, page, size));
    }

    @GetMapping("/{noticeId}")
    @Operation(summary = "편집용 공지 상세 정보를 조회합니다.")
    public ApiResponse<AdminResponse> get(@PathVariable Long noticeId) {
        return ApiResponse.success(notices.get(noticeId));
    }

    @PutMapping("/{noticeId}")
    @Operation(summary = "공지 내용과 노출 조건을 전체 교체합니다.")
    public ApiResponse<AdminResponse> update(@PathVariable Long noticeId, @RequestBody Input input) {
        return ApiResponse.success(notices.update(noticeId, input));
    }

    @DeleteMapping("/{noticeId}")
    @Operation(summary = "공지와 연결된 노출 조건을 함께 삭제합니다.")
    public ApiResponse<Void> delete(@PathVariable Long noticeId) {
        notices.delete(noticeId);
        return ApiResponse.success();
    }
}
