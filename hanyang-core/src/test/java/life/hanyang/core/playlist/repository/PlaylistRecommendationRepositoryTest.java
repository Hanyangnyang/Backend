package life.hanyang.core.playlist.repository;

import jakarta.persistence.EntityManager;
import life.hanyang.core.playlist.domain.*;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PlaylistRecommendationRepositoryTest {
    @Test
    void boundsHistoriesAndSamplesDistinctUnseenArtistsFromOtherDevicesGenrePosts() {
        try (var factory = new Configuration().addAnnotatedClass(PlaylistTrack.class).addAnnotatedClass(PlaylistArtist.class)
                .addAnnotatedClass(PlaylistTrackArtist.class).addAnnotatedClass(PlaylistSong.class)
                .addAnnotatedClass(PlaylistTrackLike.class)
                .setProperty("hibernate.connection.driver_class", "org.h2.Driver")
                .setProperty("hibernate.connection.url", "jdbc:h2:mem:recommendations;MODE=PostgreSQL")
                .setProperty("hibernate.hbm2ddl.auto", "create-drop").buildSessionFactory();
             var em = factory.createEntityManager()) {
            em.getTransaction().begin();
            // H2 may reuse parameterized CTE results; PostgreSQL does not use this optimization.
            em.createNativeQuery("SET OPTIMIZE_REUSE_RESULTS FALSE").executeUpdate();
            em.createNativeQuery("CREATE TABLE playlist_track_daily_devices (track_id varchar(255), device_id uuid, play_date date, PRIMARY KEY(track_id,device_id,play_date))")
                    .executeUpdate();
            var device = UUID.randomUUID(); var other = UUID.randomUUID();
            var since = Instant.parse("2026-09-10T15:00:00Z"); var boost = Instant.parse("2026-10-02T15:00:00Z");
            var known = track(em, "known"); var fresh = track(em, "fresh"); var ownOnly = track(em, "own-only");
            var wrongGenre = track(em, "wrong-genre"); var removed = track(em, "removed");
            UUID knownArtist = artist(em, "known"), freshArtist = artist(em, "fresh"),
                    ownArtist = artist(em, "own"), wrongArtist = artist(em, "wrong"), removedArtist = artist(em, "removed");
            for (var pair : List.of(new Object[]{known, knownArtist}, new Object[]{fresh, freshArtist},
                    new Object[]{ownOnly, ownArtist}, new Object[]{wrongGenre, wrongArtist}, new Object[]{removed, removedArtist})) {
                em.persist(new PlaylistTrackArtist((PlaylistTrack) pair[0], em.find(PlaylistArtist.class, pair[1]), 0));
            }
            song(em, known, other, Genre.BAND);
            for (int i = 0; i < 8; i++) song(em, fresh, other, Genre.BAND);
            song(em, ownOnly, device, Genre.BAND);
            song(em, wrongGenre, other, Genre.ROCK);
            var deleted = song(em, removed, other, Genre.BAND); deleted.softDelete();
            em.persist(PlaylistTrackLike.builder().deviceId(device).track(known).build());
            em.flush();
            em.createNativeQuery("UPDATE playlist_track_likes SET created_at = :time").setParameter("time", boost).executeUpdate();
            em.createNativeQuery("UPDATE playlist_songs SET created_at = :time").setParameter("time", boost).executeUpdate();
            em.createNativeQuery("INSERT INTO playlist_track_daily_devices VALUES ('known', :device, :date)")
                    .setParameter("device", device).setParameter("date", LocalDate.of(2026, 10, 9)).executeUpdate();
            em.createNativeQuery("INSERT INTO playlist_track_daily_devices VALUES ('known', :device, :date)")
                    .setParameter("device", other).setParameter("date", LocalDate.of(2026, 10, 9)).executeUpdate();
            em.clear();
            var repo = new PlaylistRecommendationRepository(em);
            var signals = repo.findSignals(device, since, boost, LocalDate.of(2026, 9, 11), LocalDate.of(2026, 10, 3));
            assertThat(signals).contains(new PlaylistRecommendationRepository.Signal("known", 2, 6),
                    new PlaylistRecommendationRepository.Signal("own-only", 0, 6));
            assertThat(signals).hasSize(2);
            assertThat(repo.findArtists(List.of("known"))).containsExactly(new PlaylistRecommendationRepository.TrackArtist("known", knownArtist));
            assertThat(repo.findGenres(List.of("fresh"))).containsExactly(new PlaylistRecommendationRepository.TrackGenre("fresh", "BAND"));
            assertThat(repo.findDiscoveryCandidates(device, List.of("BAND"), Set.of(knownArtist)))
                    .containsExactly(new PlaylistRecommendationRepository.Candidate(freshArtist, "fresh"));
            assertThat(repo.findDiscoveryCandidates(device, List.of("BAND"), Set.of()))
                    .hasSize(2).extracting(PlaylistRecommendationRepository.Candidate::artistId)
                    .containsExactlyInAnyOrder(knownArtist, freshArtist);
            assertThat(repo.findInterestCandidates(List.of(knownArtist)))
                    .containsExactly(new PlaylistRecommendationRepository.Candidate(knownArtist, "known"));
            assertThat(repo.findDiscoveryCandidates(device, List.of(), Set.of())).isEmpty();
            assertThat(repo.findSignals(UUID.randomUUID(), since, boost, LocalDate.of(2026, 9, 11), LocalDate.of(2026, 10, 3))).isEmpty();

            for (int i = 0; i < 110; i++) {
                var track = track(em, "bounded-" + i);
                var post = song(em, track, device, Genre.BAND);
                em.persist(PlaylistTrackLike.builder().deviceId(device).track(track).build());
                em.flush();
                Instant time = boost.plusSeconds(i + 1);
                em.createNativeQuery("UPDATE playlist_songs SET created_at = :time WHERE id = :id")
                        .setParameter("time", time).setParameter("id", post.getId()).executeUpdate();
                em.createNativeQuery("UPDATE playlist_track_likes SET created_at = :time WHERE track_id = :id")
                        .setParameter("time", time).setParameter("id", track.getTrackId()).executeUpdate();
            }
            var bounded = repo.findSignals(device, since, boost, LocalDate.of(2026, 9, 11), LocalDate.of(2026, 10, 3));
            assertThat(bounded).hasSize(101); // 100 latest liked/written tracks plus the separately bounded play history.
            assertThat(bounded).extracting(PlaylistRecommendationRepository.Signal::trackId)
                    .doesNotContain("bounded-0", "bounded-9", "own-only").contains("bounded-10", "bounded-109");
            assertThat(bounded).contains(new PlaylistRecommendationRepository.Signal("bounded-109", 0, 12));
            em.getTransaction().rollback();
        }
    }

    private PlaylistTrack track(EntityManager em, String id) {
        var track = PlaylistTrack.builder().trackId(id).title(id).artist("legacy").build();
        em.persist(track); return track;
    }

    private UUID artist(EntityManager em, String name) {
        UUID id = UUID.randomUUID();
        em.createNativeQuery("INSERT INTO playlist_artists(id,spotify_artist_id,name,created_at,updated_at) VALUES(:id,:spotify,:name,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)")
                .setParameter("id", id).setParameter("spotify", name).setParameter("name", name).executeUpdate();
        return id;
    }

    private PlaylistSong song(EntityManager em, PlaylistTrack track, UUID device, Genre genre) {
        var song = PlaylistSong.builder().track(track).deviceId(device).ipAddress("127.0.0.1")
                .comment("comment").genres(Set.of(genre)).build();
        em.persist(song); return song;
    }
}
