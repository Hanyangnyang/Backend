package life.hanyang.core.feedback.dto;

import life.hanyang.core.feedback.domain.Feedback;
import life.hanyang.core.feedback.domain.FeedbackCategory;
import life.hanyang.core.feedback.domain.FeedbackStatus;
import life.hanyang.core.feedback.domain.FeedbackType;

import java.time.Instant;
import java.util.UUID;

public record FeedbackResponse(
        UUID id,
        UUID userId,
        FeedbackCategory category,
        FeedbackType feedbackType,
        String content,
        String targetId,
        String platform,
        String appVersion,
        String contact,
        FeedbackStatus status,
        String adminMemo,
        Instant createdAt,
        Instant updatedAt
) {
    public static FeedbackResponse from(Feedback feedback) {
        return new FeedbackResponse(
                feedback.getId(),
                feedback.getUserId(),
                feedback.getCategory(),
                feedback.getFeedbackType(),
                feedback.getContent(),
                feedback.getTargetId(),
                feedback.getPlatform(),
                feedback.getAppVersion(),
                feedback.getContact(),
                feedback.getStatus(),
                feedback.getAdminMemo(),
                feedback.getCreatedAt(),
                feedback.getUpdatedAt()
        );
    }
}
