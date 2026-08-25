package life.hanyang.user.feedback.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import life.hanyang.core.feedback.dto.FeedbackCreateRequest;
import life.hanyang.core.feedback.dto.FeedbackResponse;
import life.hanyang.core.feedback.service.FeedbackService;
import life.hanyang.core.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/feedbacks")
@RequiredArgsConstructor
@Tag(name = "피드백 접수 API", description = "각종 서비스 기능별 사용자 피드백을 통합 접수합니다.")
public class FeedbackController {

    private final FeedbackService feedbackService;

    @Operation(summary = "피드백 제출", description = "셔틀, 시내버스, 지하철, 캠퍼스맵, 플레이리스트 등 기능별 피드백을 접수합니다.")
    @PostMapping
    public ResponseEntity<ApiResponse<FeedbackResponse>> submitFeedback(
            @Valid @RequestBody FeedbackCreateRequest request
    ) {
        FeedbackResponse response = feedbackService.createFeedback(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }
}
