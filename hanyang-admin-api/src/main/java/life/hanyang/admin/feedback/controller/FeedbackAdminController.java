package life.hanyang.admin.feedback.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import life.hanyang.core.feedback.domain.FeedbackCategory;
import life.hanyang.core.feedback.domain.FeedbackStatus;
import life.hanyang.core.feedback.domain.FeedbackType;
import life.hanyang.core.feedback.dto.FeedbackResponse;
import life.hanyang.core.feedback.dto.FeedbackStatusUpdateRequest;
import life.hanyang.core.feedback.service.FeedbackService;
import life.hanyang.core.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/feedbacks")
@RequiredArgsConstructor
@Tag(name = "(관리자용) 통합 피드백 관리 API", description = "전체 서비스에서 접수된 사용자 피드백을 조회, 처리 및 관리합니다.")
public class FeedbackAdminController {

    private final FeedbackService feedbackService;

    @Operation(summary = "피드백 목록 조회 (카테고리, 유형, 처리상태별 필터)")
    @GetMapping
    public ResponseEntity<ApiResponse<List<FeedbackResponse>>> getFeedbacks(
            @RequestParam(required = false) FeedbackCategory category,
            @RequestParam(required = false) FeedbackType feedbackType,
            @RequestParam(required = false) FeedbackStatus status
    ) {
        List<FeedbackResponse> responses = feedbackService.getFeedbacks(category, feedbackType, status);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @Operation(summary = "피드백 단건 상세 조회")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<FeedbackResponse>> getFeedback(@PathVariable UUID id) {
        FeedbackResponse response = feedbackService.getFeedback(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "피드백 처리 상태 및 관리자 메모 수정")
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<FeedbackResponse>> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody FeedbackStatusUpdateRequest request
    ) {
        FeedbackResponse response = feedbackService.updateStatus(id, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "피드백 삭제")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteFeedback(@PathVariable UUID id) {
        feedbackService.deleteFeedback(id);
        return ResponseEntity.ok(ApiResponse.success());
    }
}
