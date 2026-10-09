package life.hanyang.core.playlist.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import life.hanyang.core.playlist.domain.*;
import life.hanyang.core.playlist.dto.PlaylistTrackSearchResponse;
import life.hanyang.core.playlist.dto.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import life.hanyang.core.playlist.dto.SpotifySearchExpansion;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PlaylistArtistReadTest {
    @Test
    void linkedNamesOverrideLegacyAndUseParticipationOrder() {
        try (SessionFactory factory = factory()) {
            EntityManager em = factory.createEntityManager();
            try {
                em.getTransaction().begin();
                PlaylistTrack linked = track(em, "linked", "old name");
                link(em, linked, artist(em, "B", "가수 B"), 1);
                link(em, linked, artist(em, "A", "가수 A"), 0);
                track(em, "unlinked", "기존 가수");
                em.flush();
                em.clear();

                assertThat(em.find(PlaylistTrack.class, "linked").getArtist()).isEqualTo("가수 A, 가수 B");
                assertThat(em.find(PlaylistTrack.class, "unlinked").getArtist()).isEqualTo("기존 가수");
                PlaylistTrack loaded = em.find(PlaylistTrack.class, "linked");
                PlaylistSong post = PlaylistSong.builder().track(loaded).genres(Set.of(Genre.KPOP)).build();
                PlaylistSongReport report = PlaylistSongReport.builder().song(post).reason("reason").build();
                ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
                for (Object response : List.of(PlaylistSongResponse.of(post),
                        PlaylistTrackDetailResponse.of(loaded, 0, false, new PageImpl<>(List.of(), PageRequest.of(0, 20), 0)),
                        PlaylistTrackLikeResponse.of(loaded), PlaylistSongReportResponse.from(report),
                        PlaylistChartItemResponse.from(1, loaded))) {
                    var json = mapper.valueToTree(response);
                    assertThat(json.path("artists").size()).isEqualTo(2);
                    assertThat(json.path("artists").get(0).path("name").asText()).isEqualTo("가수 A");
                    assertThat(json.path("artists").get(1).path("name").asText()).isEqualTo("가수 B");
                    assertThat(json.path("artists").get(0).path("id").asText()).isNotBlank();
                    assertThat(json.path("artists").get(0).path("spotifyArtistId").asText()).isEqualTo("A");
                    assertThat(json.path("artists").get(0).path("imageUrl").asText()).isEqualTo("https://image/A");
                }
                assertThat(PlaylistTrackLikeResponse.of(em.find(PlaylistTrack.class, "unlinked")).artists()).isEmpty();

                em.clear();
                factory.getStatistics().clear();
                var links = new JpaRepositoryFactory(em).getRepository(PlaylistTrackArtistRepository.class)
                        .findWithArtistsByTrackIds(List.of("linked", "unlinked"));
                assertThat(links).extracting(link -> link.getTrack().getTrackId()).containsExactly("linked", "linked");
                assertThat(links).extracting(link -> link.getArtist().getName()).containsExactly("가수 A", "가수 B");
                assertThat(factory.getStatistics().getPrepareStatementCount()).isEqualTo(1);
                em.getTransaction().rollback();
            } finally { em.close(); }
        }
    }

    @Test
    void allCoartistsAreSearchableWithoutDuplicatingPostsOrTrackAggregates() {
        try (SessionFactory factory = factory()) {
            EntityManager em = factory.createEntityManager();
            try {
                em.getTransaction().begin();
                PlaylistTrack linked = track(em, "linked", "obsolete name");
                link(em, linked, artist(em, "A", "가수 A"), 0);
                link(em, linked, artist(em, "B", "가수 B"), 1);
                song(em, linked, "first");
                song(em, linked, "second");
                PlaylistTrack legacy = track(em, "legacy", "가수 C");
                song(em, legacy, "legacy post");
                em.flush();
                em.clear();

                var songs = new PlaylistSongRepositoryCustomImpl(new JPAQueryFactory(em), em);
                var first = songs.searchSongsWithWeight("가수", SpotifySearchExpansion.empty(), PageRequest.of(0, 2));
                var second = songs.searchSongsWithWeight("가수", SpotifySearchExpansion.empty(), PageRequest.of(1, 2));
                assertThat(first.getTotalElements()).isEqualTo(3);
                assertThat(first.getContent()).hasSize(2);
                assertThat(second.getContent()).hasSize(1);
                assertThat(first.getContent()).extracting(PlaylistSong::getId)
                        .doesNotContain(second.getContent().get(0).getId());
                assertThat(songs.searchSongsWithWeight("가수 B", SpotifySearchExpansion.empty(), PageRequest.of(0, 20))
                        .getTotalElements()).isEqualTo(2);
                assertThat(songs.searchSongsWithWeight("obsolete", SpotifySearchExpansion.empty(), PageRequest.of(0, 20))
                        .getTotalElements()).isZero();
                assertThat(songs.searchSongsWithWeight("가수 C", SpotifySearchExpansion.empty(), PageRequest.of(0, 20))
                        .getTotalElements()).isEqualTo(1);

                var tracks = new PlaylistTrackRepositoryCustomImpl(new JPAQueryFactory(em));
                var results = tracks.searchTracks("가수 B", PageRequest.of(0, 20));
                assertThat(results.getTotalElements()).isEqualTo(1);
                assertThat(results.getContent()).hasSize(1);
                PlaylistTrackSearchResponse found = results.getContent().get(0);
                assertThat(found.artist()).isEqualTo("가수 A, 가수 B");
                assertThat(found.totalSongsCount()).isEqualTo(2);
                assertThat(found.totalHeartCount()).isZero();
                assertThat(found.artists()).extracting(artist -> artist.name()).containsExactly("가수 A", "가수 B");
                assertThat(tracks.searchTracks("obsolete", PageRequest.of(0, 20)).getTotalElements()).isZero();
                em.getTransaction().rollback();
            } finally { em.close(); }
        }
    }

    @Test
    void spotifyExpansionUsesOnlyTrackIdsAndDirectMatchesComeFirst() {
        try (SessionFactory factory = factory()) {
            EntityManager em = factory.createEntityManager();
            try {
                em.getTransaction().begin();
                PlaylistTrack first = track(em, "first", "legacy");
                PlaylistTrack second = track(em, "second", "legacy");
                PlaylistArtist a = artist(em, "A", "가수 A");
                link(em, first, a, 0);
                link(em, first, artist(em, "B", "가수 B"), 1);
                link(em, second, a, 0);
                song(em, first, "post");
                song(em, second, "post");
                em.flush();
                em.clear();

                var repository = new PlaylistSongRepositoryCustomImpl(new JPAQueryFactory(em), em);
                var result = repository.searchSongsWithWeight("unmatched query",
                        new SpotifySearchExpansion(List.of("first", "second"), List.of(), List.of()), PageRequest.of(0, 20));
                assertThat(result.getContent()).extracting(PlaylistSong::getTrackId).containsExactlyInAnyOrder("first", "second");
                var ignoredMetadata = repository.searchSongsWithWeight("unmatched query",
                        new SpotifySearchExpansion(List.of(), List.of("first"), List.of("가수 A")), PageRequest.of(0, 20));
                assertThat(ignoredMetadata.getTotalElements()).isZero();
                var directFirst = repository.searchSongsWithWeight("가수 B",
                        new SpotifySearchExpansion(List.of("second"), List.of(), List.of()), PageRequest.of(0, 20));
                assertThat(directFirst.getContent()).extracting(PlaylistSong::getTrackId).containsExactly("first", "second");
                em.getTransaction().rollback();
            } finally { em.close(); }
        }
    }

    @Test
    @org.junit.jupiter.api.Timeout(60)
    void searchWithFullSpotifyExpansionKeepsPagingAndBoundedParserMemory() {
        try (SessionFactory factory = factory()) {
            EntityManager em = factory.createEntityManager();
            try {
                em.getTransaction().begin();
                PlaylistTrack first = track(em, "first", "obsolete");
                PlaylistTrack second = track(em, "second", "legacy");
                link(em, first, artist(em, "A", "candidate-0"), 0);
                link(em, first, artist(em, "B", "candidate-1"), 1);
                link(em, second, artist(em, "C", "candidate-2"), 0);
                song(em, first, "post");
                song(em, second, "post");
                em.flush();
                em.clear();
                List<String> candidates = java.util.stream.IntStream.range(0, 16)
                        .mapToObj(i -> "candidate-" + i).toList();
                SpotifySearchExpansion expansion = new SpotifySearchExpansion(
                        java.util.stream.IntStream.range(0, 8).mapToObj(i -> i == 0 ? "first" : i == 1 ? "second" : "external-" + i).toList(),
                        java.util.stream.IntStream.range(0, 8).mapToObj(i -> "title-" + i).toList(), candidates);
                var repository = new PlaylistSongRepositoryCustomImpl(new JPAQueryFactory(em), em);
                for (int i = 0; i < 4; i++) {
                    // Different list sizes force distinct query shapes rather than a cached parse.
                    var varyingExpansion = new SpotifySearchExpansion(expansion.trackIds().subList(0, 8 - i),
                            expansion.titles().subList(0, 8 - i), candidates.subList(0, 16 - i));
                    var firstPage = repository.searchSongsWithWeight("candidate-0", varyingExpansion, PageRequest.of(0, 1));
                    var secondPage = repository.searchSongsWithWeight("candidate-0", varyingExpansion, PageRequest.of(1, 1));
                    assertThat(firstPage.getTotalElements()).isEqualTo(2);
                    assertThat(firstPage.getContent()).extracting(PlaylistSong::getTrackId).containsExactly("first");
                    assertThat(secondPage.getContent()).extracting(PlaylistSong::getTrackId).containsExactly("second");
                }
                em.getTransaction().rollback();
            } finally { em.close(); }
        }
    }

    @Test
    void feedArtistLoadingUsesBatchesInsteadOfOneQueryPerTrackAndArtist() {
        try (SessionFactory factory = factory()) {
            EntityManager em = factory.createEntityManager();
            try {
                em.getTransaction().begin();
                for (int index = 0; index < 12; index++) {
                    PlaylistTrack track = track(em, "track-" + index, "legacy");
                    link(em, track, artist(em, String.format("%022d", index), "가수 " + index), 0);
                    song(em, track, "post");
                }
                em.flush();
                em.clear();
                factory.getStatistics().clear();
                var repository = new PlaylistSongRepositoryCustomImpl(new JPAQueryFactory(em), em);
                var result = repository.searchSongs(null, PageRequest.of(0, 20));
                assertThat(result.getContent()).hasSize(12);
                assertThat(result.getContent().stream().map(PlaylistSong::getArtist)).allMatch(name -> name.startsWith("가수 "));
                // Page + count + batched links + batched artists, independent of these 12 tracks.
                assertThat(factory.getStatistics().getPrepareStatementCount()).isLessThanOrEqualTo(4);
                em.getTransaction().rollback();
            } finally { em.close(); }
        }
    }

    @Test
    void unionSearchEscapesWildcardsAndKeepsTotalForOutOfRangePages() {
        try (SessionFactory factory = factory()) {
            EntityManager em = factory.createEntityManager();
            try {
                em.getTransaction().begin();
                PlaylistTrack track = track(em, "literal", "legacy");
                PlaylistSong literal = song(em, track, "100%_! literal");
                song(em, track, "ordinary comment");
                PlaylistSong deleted = song(em, track, "100%_! deleted");
                deleted.softDelete();
                em.flush();
                em.clear();
                var repository = new PlaylistSongRepositoryCustomImpl(new JPAQueryFactory(em), em);
                for (String keyword : List.of("%", "_", "!")) {
                    var result = repository.searchSongsWithWeight(keyword, SpotifySearchExpansion.empty(), PageRequest.of(0, 20));
                    assertThat(result.getContent()).extracting(PlaylistSong::getId).containsExactly(literal.getId());
                    var emptyPage = repository.searchSongsWithWeight(keyword, SpotifySearchExpansion.empty(), PageRequest.of(2, 1));
                    assertThat(emptyPage.getContent()).isEmpty();
                    assertThat(emptyPage.getTotalElements()).isEqualTo(1);
                }
                em.getTransaction().rollback();
            } finally { em.close(); }
        }
    }

    @Test
    void searchResponseArtistNamesAndArraysUseBatchesForMultipleCoartists() {
        try (SessionFactory factory = factory()) {
            EntityManager em = factory.createEntityManager();
            try {
                em.getTransaction().begin();
                for (int index = 0; index < 12; index++) {
                    PlaylistTrack track = track(em, "search-" + index, "obsolete");
                    link(em, track, artist(em, "search-A-" + index, "가수 A " + index), 0);
                    link(em, track, artist(em, "search-B-" + index, "가수 B " + index), 1);
                    song(em, track, "post");
                    song(em, track, "another post");
                }
                em.flush();
                em.clear();
                factory.getStatistics().clear();
                var repository = new PlaylistSongRepositoryCustomImpl(new JPAQueryFactory(em), em);
                var result = repository.searchSongsWithWeight("가수", SpotifySearchExpansion.empty(), PageRequest.of(0, 20));
                assertThat(result.getTotalElements()).isEqualTo(24);
                assertThat(result.getContent()).hasSize(20);
                for (PlaylistSong song : result) {
                    PlaylistSongResponse response = PlaylistSongResponse.of(song);
                    assertThat(response.artist()).startsWith("가수 A ").contains(", 가수 B ");
                    assertThat(response.artists()).hasSize(2);
                }
                // List + distinct count + one batch for links + one batch for artists.
                assertThat(factory.getStatistics().getPrepareStatementCount()).isLessThanOrEqualTo(4);
                em.getTransaction().rollback();
            } finally { em.close(); }
        }
    }

    @Test
    void allRawChartsUseOrderedNamesAndDoNotMultiplyScores() throws Exception {
        try (SessionFactory factory = factory()) {
            EntityManager em = factory.createEntityManager();
            try {
                em.getTransaction().begin();
                PlaylistTrack track = track(em, "linked", "legacy");
                link(em, track, artist(em, "B", "가수 B"), 1);
                link(em, track, artist(em, "A", "가수 A"), 0);
                song(em, track, "post");
                PlaylistTrack unlinked = track(em, "unlinked", "기존 가수");
                song(em, unlinked, "post");
                Instant now = Instant.now();
                em.persist(PlaylistTrackHourlyPlay.builder().trackId("linked").playHour(now).playCount(10).build());
                em.persist(PlaylistTrackHourlyPlay.builder().trackId("unlinked").playHour(now).playCount(10).build());
                em.flush();
                for (String method : List.of("findRisingChartRaw", "findWeeklyChartRaw", "findMonthlyChartRaw")) {
                    String sql = java.util.Arrays.stream(PlaylistTrackHourlyPlayRepository.class.getMethods())
                            .filter(candidate -> candidate.getName().equals(method)).findFirst().orElseThrow()
                            .getAnnotation(Query.class).value();
                    var query = em.createNativeQuery(sql).setParameter("genre", null).setParameter("limit", 20);
                    if (method.equals("findRisingChartRaw")) {
                        query.setParameter("h24", now.minusSeconds(86400)).setParameter("h6", now.minusSeconds(21600))
                                .setParameter("upperBound", now.plusSeconds(60));
                    } else {
                        query.setParameter("startPeriod", now.minusSeconds(86400)).setParameter("endPeriod", now.plusSeconds(60));
                    }
                    @SuppressWarnings("unchecked") List<Object[]> rows = query.getResultList();
                    assertThat(rows).hasSize(2);
                    Object[] linked = rows.stream().filter(row -> row[0].equals("linked")).findFirst().orElseThrow();
                    Object[] legacy = rows.stream().filter(row -> row[0].equals("unlinked")).findFirst().orElseThrow();
                    assertThat(linked[2]).isEqualTo("가수 A, 가수 B");
                    assertThat(legacy[2]).isEqualTo("기존 가수");
                    assertThat(((Number) linked[4]).longValue()).isEqualTo(((Number) legacy[4]).longValue());
                }
                em.getTransaction().rollback();
            } finally { em.close(); }
        }
    }

    private SessionFactory factory() {
        return new Configuration().addAnnotatedClass(PlaylistTrack.class).addAnnotatedClass(PlaylistArtist.class)
                .addAnnotatedClass(PlaylistTrackArtist.class).addAnnotatedClass(PlaylistSong.class)
                .addAnnotatedClass(PlaylistSongReaction.class).addAnnotatedClass(PlaylistTrackLike.class)
                .addAnnotatedClass(PlaylistTrackHourlyPlay.class)
                .setProperty("hibernate.connection.driver_class", "org.h2.Driver")
                .setProperty("hibernate.connection.url", "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=PostgreSQL")
                .setProperty("hibernate.hbm2ddl.auto", "create-drop")
                .setProperty("hibernate.generate_statistics", "true").buildSessionFactory();
    }

    private PlaylistTrack track(EntityManager em, String id, String legacyName) {
        PlaylistTrack track = PlaylistTrack.builder().trackId(id).title(id).artist(legacyName).build();
        em.persist(track);
        return track;
    }

    private PlaylistArtist artist(EntityManager em, String id, String name) {
        UUID internalId = UUID.randomUUID();
        em.createNativeQuery("""
                INSERT INTO playlist_artists(id, spotify_artist_id, name, image_url, created_at, updated_at)
                VALUES (:id, :spotifyId, :name, :image, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """).setParameter("id", internalId).setParameter("spotifyId", id).setParameter("name", name)
                .setParameter("image", "https://image/" + id).executeUpdate();
        return em.find(PlaylistArtist.class, internalId);
    }

    private void link(EntityManager em, PlaylistTrack track, PlaylistArtist artist, int position) {
        em.persist(new PlaylistTrackArtist(track, artist, position));
    }

    private PlaylistSong song(EntityManager em, PlaylistTrack track, String comment) {
        PlaylistSong song = PlaylistSong.builder().track(track).comment(comment).genres(Set.of(Genre.KPOP))
                .deviceId(UUID.randomUUID()).ipAddress("127.0.0.1").build();
        em.persist(song);
        return song;
    }
}
