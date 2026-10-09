package life.hanyang.core.feedback.repository;

import life.hanyang.core.feedback.domain.Feedback;
import life.hanyang.core.feedback.domain.FeedbackCategory;
import life.hanyang.core.feedback.domain.FeedbackStatus;
import life.hanyang.core.feedback.domain.FeedbackType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface FeedbackRepositoryCustom {
    Page<Feedback> searchFeedbacks(
            FeedbackCategory category,
            FeedbackType feedbackType,
            FeedbackStatus status,
            Pageable pageable
    );
}
