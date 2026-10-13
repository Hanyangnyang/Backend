package life.hanyang.admin.playlist.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import life.hanyang.core.global.exception.*;
import life.hanyang.core.global.response.ApiResponse;
import life.hanyang.core.playlist.domain.ReportStatus;
import life.hanyang.core.playlist.dto.*;
import life.hanyang.core.playlist.service.PlaylistCommentReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/playlist")
@Tag(name = "(관리자용) 플레이리스트 댓글 신고")
public class PlaylistCommentAdminController {
    private final PlaylistCommentReportService reports;

    @GetMapping("/comment-reports")
    @Operation(summary = "댓글 신고 목록 조회", description = "상태 필터, 최신순. 신고 당시 댓글 내용은 삭제 후에도 관리자에게 제공됩니다.")
    public ApiResponse<Page<PlaylistCommentReportResponse>> list(@RequestParam(required = false) ReportStatus status,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        if (page < 0 || size < 1 || size > 100) throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        return ApiResponse.success(reports.list(status, PageRequest.of(page, size)));
    }

    @PatchMapping("/comment-reports/{reportId}")
    @Operation(summary = "댓글 신고 처리", description = "상태와 관리자 메모(최대 1000자)를 변경합니다. REVIEWED 상태 변경만으로 댓글이 삭제되지는 않습니다.")
    public ApiResponse<PlaylistCommentReportResponse> process(@PathVariable UUID reportId,
            @Valid @RequestBody PlaylistReportProcessRequest request) {
        return ApiResponse.success(reports.process(reportId, request));
    }

    @DeleteMapping("/songs/{songId}/comments/{commentId}")
    @Operation(summary = "댓글 관리자 삭제", description = "미처리 신고를 REVIEWED로 일괄 처리하고 미발송 알림을 취소합니다. 삭제 사유 최대 1000자.")
    public ApiResponse<Void> delete(@PathVariable UUID songId, @PathVariable UUID commentId,
            @RequestBody(required = false) PlaylistSongDeleteRequest request) {
        reports.deleteByAdmin(songId, commentId, request == null ? null : request.reason());
        return ApiResponse.success();
    }
}
