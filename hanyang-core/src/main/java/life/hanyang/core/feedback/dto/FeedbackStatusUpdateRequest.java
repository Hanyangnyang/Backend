package life.hanyang.core.feedback.dto;

import jakarta.validation.constraints.NotNull;
import life.hanyang.core.feedback.domain.FeedbackStatus;

public record FeedbackStatusUpdateRequest(
        @NotNull(message = "변경할 상태값은 필수입니다.")
        FeedbackStatus status,
        String adminMemo
) {}
