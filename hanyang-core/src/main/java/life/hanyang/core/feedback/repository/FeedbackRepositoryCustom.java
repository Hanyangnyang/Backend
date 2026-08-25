package life.hanyang.core.feedback.repository;

import life.hanyang.core.feedback.domain.Feedback;
import life.hanyang.core.feedback.domain.FeedbackCategory;
import life.hanyang.core.feedback.domain.FeedbackStatus;
import life.hanyang.core.feedback.domain.FeedbackType;

import java.util.List;

public interface FeedbackRepositoryCustom {
    List<Feedback> searchFeedbacks(
            FeedbackCategory category,
            FeedbackType feedbackType,
            FeedbackStatus status
    );
}
