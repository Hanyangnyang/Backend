package life.hanyang.core.playlist.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import life.hanyang.core.playlist.domain.*;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PlaylistSongPopularityTest {

    @Test
    void sortsByAllReactionsThenNewestAndPreservesPagination() {
        try (SessionFactory factory = new Configuration()
                .addAnnotatedClass(PlaylistTrack.class)
                .addAnnotatedClass(PlaylistArtist.class)
                .addAnnotatedClass(PlaylistTrackArtist.class)
                .addAnnotatedClass(PlaylistSong.class)
                .addAnnotatedClass(PlaylistSongReaction.class)
                .setProperty("hibernate.connection.driver_class", "org.h2.Driver")
                .setProperty("hibernate.connection.url", "jdbc:h2:mem:popularity;MODE=PostgreSQL;DB_CLOSE_DELAY=-1")
                .setProperty("hibernate.hbm2ddl.auto", "create-drop")
                .buildSessionFactory()) {
            EntityManager em = factory.createEntityManager();
            try {
                em.getTransaction().begin();
                PlaylistTrack track = PlaylistTrack.builder()
                        .trackId("target").title("곡").artist("가수").build();
                PlaylistTrack otherTrack = PlaylistTrack.builder()
                        .trackId("other").title("다른 곡").artist("가수").build();
                em.persist(track);
                em.persist(otherTrack);
                PlaylistSong popular = song(em, track, "반응 3개", 0, 0);
                PlaylistSong olderTie = song(em, track, "반응 2개 오래된 글", 0, 1);
                PlaylistSong newerTie = song(em, track, "반응 2개 최신 글", 0, 2);
                PlaylistSong zero = song(em, track, "반응 없고 하트만 많음", 100, 3);
                PlaylistSong deleted = song(em, track, "삭제된 글", 0, 4);
                deleted.softDelete();
                PlaylistSong other = song(em, otherTrack, "다른 곡의 글", 0, 5);
                react(em, popular, ReactionType.LOVE, ReactionType.FIRE, ReactionType.COOL);
                react(em, olderTie, ReactionType.LOVE, ReactionType.FIRE);
                react(em, newerTie, ReactionType.COOL, ReactionType.BEER);
                react(em, deleted, ReactionType.LOVE, ReactionType.FIRE, ReactionType.COOL, ReactionType.BEER);
                react(em, other, ReactionType.LOVE, ReactionType.FIRE, ReactionType.COOL, ReactionType.BEER);
                em.flush();
                em.clear();

                PlaylistSongRepositoryCustomImpl repository =
                        new PlaylistSongRepositoryCustomImpl(new JPAQueryFactory(em));
                Page<PlaylistSong> first = repository.searchSongsByTrackId("target", PageRequest.of(0, 2));
                Page<PlaylistSong> second = repository.searchSongsByTrackId("target", PageRequest.of(1, 2));

                assertThat(first.getContent()).extracting(PlaylistSong::getId)
                        .containsExactly(popular.getId(), newerTie.getId());
                assertThat(second.getContent()).extracting(PlaylistSong::getId)
                        .containsExactly(olderTie.getId(), zero.getId());
                assertThat(first.getTotalElements()).isEqualTo(4);
                assertThat(first.getTotalPages()).isEqualTo(2);
                assertThat(repository.searchSongsByTrackId("missing", PageRequest.of(0, 2)).getContent()).isEmpty();
                em.getTransaction().rollback();
            } finally {
                em.close();
            }
        }
    }

    private PlaylistSong song(EntityManager em, PlaylistTrack track, String comment, int hearts, int hour) {
        PlaylistSong song = PlaylistSong.builder().track(track).comment(comment)
                .deviceId(UUID.randomUUID()).ipAddress("127.0.0.1").genres(Set.of(Genre.KPOP)).build();
        ReflectionTestUtils.setField(song, "heartCount", hearts);
        em.persist(song);
        em.flush();
        // 생성 시각 자동 생성 이후 테스트에 필요한 명시적 순서를 설정한다.
        em.createNativeQuery("UPDATE playlist_songs SET created_at = :createdAt WHERE id = :id")
                .setParameter("createdAt", Instant.parse("2026-10-01T00:00:00Z").plusSeconds(hour * 3600L))
                .setParameter("id", song.getId()).executeUpdate();
        return song;
    }

    private void react(EntityManager em, PlaylistSong song, ReactionType... types) {
        UUID deviceId = UUID.randomUUID();
        for (ReactionType type : types) {
            em.persist(PlaylistSongReaction.builder()
                    .song(song).deviceId(deviceId).reactionType(type).build());
        }
    }
}
