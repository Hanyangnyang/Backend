package life.hanyang.core.playlist.repository;

import life.hanyang.core.playlist.domain.*;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.Instant;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

class PlaylistCommentRepositoryTest {
    private org.hibernate.SessionFactory factory() {
        return new Configuration().addAnnotatedClass(PlaylistComment.class)
                .setProperty("hibernate.connection.driver_class", "org.h2.Driver")
                .setProperty("hibernate.connection.url", "jdbc:h2:mem:comments-" + UUID.randomUUID() + ";MODE=PostgreSQL")
                .setProperty("hibernate.physical_naming_strategy", "org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy")
                .setProperty("hibernate.hbm2ddl.auto", "create-drop").buildSessionFactory();
    }

    @Test void historyExcludesDeletedOtherAuthorsAndOtherSongsAndIsBoundedTo20() {
        try (var factory = factory(); var em = factory.createEntityManager()) {
            em.getTransaction().begin();
            UUID song = UUID.randomUUID(), author = UUID.randomUUID();
            for (int i = 0; i < 25; i++) {
                var comment = new PlaylistComment(song, author, 1, "댓글" + i);
                ReflectionTestUtils.setField(comment, "createdAt", Instant.parse("2026-10-13T00:00:00Z").plusSeconds(i));
                em.persist(comment);
            }
            em.persist(new PlaylistComment(song, UUID.randomUUID(), 2, "다른 작성자"));
            em.persist(new PlaylistComment(UUID.randomUUID(), author, 1, "다른 글"));
            var removed = new PlaylistComment(song, author, 1, "삭제됨");
            removed.delete();
            em.persist(removed);
            em.flush();
            em.clear();
            var repo = new JpaRepositoryFactory(em).getRepository(PlaylistCommentRepository.class);
            var history = repo.findTop20BySongIdAndDeviceIdAndDeletedAtIsNullOrderByCreatedAtDescIdDesc(song, author);
            assertThat(history).hasSize(20);
            assertThat(history.get(0).getContent()).isEqualTo("댓글24");
            assertThat(history.get(19).getContent()).isEqualTo("댓글5");
            assertThat(history).allSatisfy(c -> {
                assertThat(c.getDeviceId()).isEqualTo(author);
                assertThat(c.getSongId()).isEqualTo(song);
                assertThat(c.getDeletedAt()).isNull();
            });
            em.getTransaction().rollback();
        }
    }

}
