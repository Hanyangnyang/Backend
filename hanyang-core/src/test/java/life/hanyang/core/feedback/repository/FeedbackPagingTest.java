package life.hanyang.core.feedback.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import life.hanyang.core.feedback.domain.*;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class FeedbackPagingTest {
    @Test
    void filtersAndPaginatesInDatabaseWithStableOrderAndCorrectTotal() {
        try (var factory = new Configuration().addAnnotatedClass(Feedback.class)
                .setProperty("hibernate.connection.driver_class", "org.h2.Driver")
                .setProperty("hibernate.connection.url", "jdbc:h2:mem:feedbackPaging;MODE=PostgreSQL")
                .setProperty("hibernate.hbm2ddl.auto", "create-drop").buildSessionFactory();
             var em = factory.createEntityManager()) {
            em.getTransaction().begin();
            for (int i = 0; i < 7; i++) {
                var feedback = Feedback.builder().category(i < 5 ? FeedbackCategory.PLAYLIST : FeedbackCategory.GENERAL)
                        .feedbackType(FeedbackType.FEATURE_REQUEST).content("feedback " + i).build();
                em.persist(feedback);
            }
            em.flush();
            em.createQuery("update Feedback f set f.createdAt = :time")
                    .setParameter("time", Instant.parse("2026-10-01T00:00:00Z")).executeUpdate();
            em.clear();
            var repo = new FeedbackRepositoryCustomImpl(new JPAQueryFactory(em));
            var first = repo.searchFeedbacks(FeedbackCategory.PLAYLIST, FeedbackType.FEATURE_REQUEST,
                    FeedbackStatus.PENDING, PageRequest.of(0, 2));
            var second = repo.searchFeedbacks(FeedbackCategory.PLAYLIST, FeedbackType.FEATURE_REQUEST,
                    FeedbackStatus.PENDING, PageRequest.of(1, 2));
            assertThat(first.getContent()).hasSize(2);
            assertThat(second.getContent()).hasSize(2);
            assertThat(first.getTotalElements()).isEqualTo(5);
            assertThat(second.getTotalElements()).isEqualTo(5);
            assertThat(second.getContent()).extracting(Feedback::getId)
                    .doesNotContainAnyElementsOf(first.getContent().stream().map(Feedback::getId).toList());
            var beyond = repo.searchFeedbacks(FeedbackCategory.PLAYLIST, null, null, PageRequest.of(3, 2));
            assertThat(beyond.getContent()).isEmpty();
            assertThat(beyond.getTotalElements()).isEqualTo(5);
            assertThat(repo.searchFeedbacks(null, null, null, PageRequest.of(0, 2)).getTotalElements()).isEqualTo(7);
            em.getTransaction().rollback();
        }
    }
}
