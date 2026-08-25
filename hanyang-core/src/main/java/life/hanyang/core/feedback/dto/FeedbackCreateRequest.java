package life.hanyang.core.feedback.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import life.hanyang.core.feedback.domain.FeedbackCategory;
import life.hanyang.core.feedback.domain.FeedbackType;

import java.util.UUID;

public record FeedbackCreateRequest(
        UUID userId,

        @NotNull(message = "카테고리는 필수 항목입니다.")
        FeedbackCategory category,

        @NotNull(message = "피드백 유형은 필수 항목입니다.")
        FeedbackType feedbackType,

        @NotBlank(message = "피드백 내용은 필수 입력 항목입니다.")
        String content,

        String targetId,     // 특정 기능 상세 ID (예: 플레이리스트 곡ID, 버스 노선명 등)
        String platform,     // IOS, ANDROID, WEB
        String appVersion,   // 앱 버전
        String contact       // 연락처 or 이메일 (선택)
) {}
