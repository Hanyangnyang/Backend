package life.hanyang.core.feedback.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import life.hanyang.core.feedback.domain.Feedback;
import life.hanyang.core.feedback.domain.FeedbackCategory;
import life.hanyang.core.feedback.domain.FeedbackStatus;
import life.hanyang.core.feedback.domain.FeedbackType;
import lombok.RequiredArgsConstructor;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import static life.hanyang.core.feedback.domain.QFeedback.feedback;

@RequiredArgsConstructor
public class FeedbackRepositoryCustomImpl implements FeedbackRepositoryCustom {
    private final JPAQueryFactory queryFactory;

    @Override
    public Page<Feedback> searchFeedbacks(
            FeedbackCategory category,
            FeedbackType feedbackType,
            FeedbackStatus status,
            Pageable pageable
    ) {
        List<Feedback> content = queryFactory
                .selectFrom(feedback)
                .where(
                        eqCategory(category),
                        eqFeedbackType(feedbackType),
                        eqStatus(status)
                )
                .orderBy(feedback.createdAt.desc(), feedback.id.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();
        Long total = queryFactory.select(feedback.count()).from(feedback)
                .where(eqCategory(category), eqFeedbackType(feedbackType), eqStatus(status))
                .fetchOne();
        return new PageImpl<>(content, pageable, total != null ? total : 0L);
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
