package life.hanyang.core.feedback.repository;

import life.hanyang.core.feedback.domain.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, UUID>, FeedbackRepositoryCustom {
}
