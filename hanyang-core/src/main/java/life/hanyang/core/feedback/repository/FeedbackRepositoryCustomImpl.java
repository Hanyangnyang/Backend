package life.hanyang.core.feedback.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import life.hanyang.core.feedback.domain.Feedback;
import life.hanyang.core.feedback.domain.FeedbackCategory;
import life.hanyang.core.feedback.domain.FeedbackStatus;
import life.hanyang.core.feedback.domain.FeedbackType;
import lombok.RequiredArgsConstructor;

import java.util.List;

import static life.hanyang.core.feedback.domain.QFeedback.feedback;

@RequiredArgsConstructor
public class FeedbackRepositoryCustomImpl implements FeedbackRepositoryCustom {
    private final JPAQueryFactory queryFactory;

    @Override
    public List<Feedback> searchFeedbacks(
            FeedbackCategory category,
            FeedbackType feedbackType,
            FeedbackStatus status
    ) {
        return queryFactory
                .selectFrom(feedback)
                .where(
                        eqCategory(category),
                        eqFeedbackType(feedbackType),
                        eqStatus(status)
                )
                .orderBy(feedback.createdAt.desc())
                .fetch();
    }

    private BooleanExpression eqCategory(FeedbackCategory category) {
        return category != null ? feedback.category.eq(category) : null;
    }

    private BooleanExpression eqFeedbackType(FeedbackType feedbackType) {
        return feedbackType != null ? feedback.feedbackType.eq(feedbackType) : null;
    }

    private BooleanExpression eqStatus(FeedbackStatus status) {
        return status != null ? feedback.status.eq(status) : null;
    }
}
