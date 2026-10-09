package life.hanyang.core.playlist.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import life.hanyang.core.playlist.domain.PlaylistArtist;
import life.hanyang.core.playlist.domain.PlaylistTrack;
import life.hanyang.core.playlist.domain.PlaylistTrackArtist;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlaylistArtistMappingTest {
    @Test
    void missingTrackQuerySupportsCursorAndExcludesLinkedTracks() {
        try (SessionFactory factory = factory("artist-cursor")) {
            EntityManager em = factory.createEntityManager();
            try {
                em.getTransaction().begin();
                PlaylistTrack a = track(em, "a");
                track(em, "b");
                track(em, "c");
                em.persist(new PlaylistTrackArtist(a, artist(em), 0));
                em.flush();
                PlaylistTrackRepository repository = new JpaRepositoryFactory(em).getRepository(
                        PlaylistTrackRepository.class, new PlaylistTrackRepositoryCustomImpl(new JPAQueryFactory(em)));

                assertThat(repository.findTrackIdsWithoutArtists("", PageRequest.of(0, 1))).containsExactly("b");
                assertThat(repository.findTrackIdsWithoutArtists("b", PageRequest.of(0, 20))).containsExactly("c");
                assertThat(repository.findTrackIdsWithoutArtists("c", PageRequest.of(0, 20))).isEmpty();
                assertThat(repository.findByIdForArtistSync("a")).isPresent();
                em.getTransaction().rollback();
            } finally {
                em.close();
            }
        }
    }

    @Test
    void duplicateTrackArtistIsRejectedByDatabaseConstraint() {
        try (SessionFactory factory = factory("artist-duplicate")) {
            EntityManager em = factory.createEntityManager();
            try {
                em.getTransaction().begin();
                PlaylistTrack track = track(em, "a");
                PlaylistArtist artist = artist(em);
                em.persist(new PlaylistTrackArtist(track, artist, 0));
                em.flush();
                em.persist(new PlaylistTrackArtist(track, artist, 1));
                assertThatThrownBy(em::flush).isInstanceOf(RuntimeException.class);
                em.getTransaction().rollback();
            } finally {
                em.close();
            }
        }
    }

    private SessionFactory factory(String name) {
        return new Configuration().addAnnotatedClass(PlaylistTrack.class)
                .addAnnotatedClass(PlaylistArtist.class).addAnnotatedClass(PlaylistTrackArtist.class)
                .setProperty("hibernate.connection.driver_class", "org.h2.Driver")
                .setProperty("hibernate.connection.url", "jdbc:h2:mem:" + name + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1")
                .setProperty("hibernate.hbm2ddl.auto", "create-drop").buildSessionFactory();
    }

    private PlaylistTrack track(EntityManager em, String id) {
        PlaylistTrack track = PlaylistTrack.builder().trackId(id).title("title").artist("legacy").build();
        em.persist(track);
        return track;
    }

    private PlaylistArtist artist(EntityManager em) {
        UUID id = UUID.randomUUID();
        em.createNativeQuery("""
                INSERT INTO playlist_artists(id, spotify_artist_id, name, created_at, updated_at)
                VALUES (:id, '000000000000000000000A', 'Artist', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """).setParameter("id", id).executeUpdate();
        return em.find(PlaylistArtist.class, id);
    }
}
