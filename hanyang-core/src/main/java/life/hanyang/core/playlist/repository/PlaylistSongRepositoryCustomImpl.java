package life.hanyang.core.playlist.repository;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import life.hanyang.core.playlist.domain.Genre;
import life.hanyang.core.playlist.domain.PlaylistSong;
import life.hanyang.core.playlist.dto.SpotifySearchExpansion;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static life.hanyang.core.playlist.domain.QPlaylistSong.playlistSong;
import static life.hanyang.core.playlist.domain.QPlaylistSongReaction.playlistSongReaction;
import static life.hanyang.core.playlist.domain.QPlaylistTrack.playlistTrack;

@RequiredArgsConstructor
public class PlaylistSongRepositoryCustomImpl implements PlaylistSongRepositoryCustom {
    private final JPAQueryFactory queryFactory;
    private final EntityManager entityManager;

    @Override
    public Page<PlaylistSong> searchSongs(Genre genre, Pageable pageable) {
        List<PlaylistSong> content = queryFactory
                .selectFrom(playlistSong)
                .join(playlistSong.track).fetchJoin()
                .where(
                        playlistSong.deletedAt.isNull(),
                        containsGenre(genre)
                )
                .orderBy(playlistSong.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(playlistSong.count())
                .from(playlistSong)
                .where(
                        playlistSong.deletedAt.isNull(),
                        containsGenre(genre)
                )
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    @Override
    public Page<PlaylistSong> searchSongsForAdmin(Genre genre, Boolean isDeleted, Pageable pageable) {
        List<PlaylistSong> content = queryFactory
                .selectFrom(playlistSong)
                .join(playlistSong.track).fetchJoin()
                .where(
                        containsGenre(genre),
                        eqDeleted(isDeleted)
                )
                .orderBy(playlistSong.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(playlistSong.count())
                .from(playlistSong)
                .where(
                        containsGenre(genre),
                        eqDeleted(isDeleted)
                )
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    @Override
    public Page<PlaylistSong> searchSongsByTrackId(String trackId, Pageable pageable) {
        boolean latest = pageable.getSort().getOrderFor("createdAt") != null;
        OrderSpecifier<?>[] ordering = latest
                ? new OrderSpecifier<?>[]{playlistSong.createdAt.desc(), playlistSong.id.desc()}
                : new OrderSpecifier<?>[]{playlistSongReaction.id.count().desc(), playlistSong.createdAt.desc(), playlistSong.id.desc()};
        var contentQuery = queryFactory.selectFrom(playlistSong)
                .join(playlistSong.track, playlistTrack).fetchJoin()
                .where(playlistSong.track.trackId.eq(trackId), playlistSong.deletedAt.isNull());
        if (!latest) {
            contentQuery.leftJoin(playlistSongReaction).on(playlistSongReaction.song.eq(playlistSong))
                    .groupBy(playlistSong, playlistTrack);
        }
        List<PlaylistSong> content = contentQuery.orderBy(ordering)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(playlistSong.count())
                .from(playlistSong)
                .where(
                        playlistSong.track.trackId.eq(trackId),
                        playlistSong.deletedAt.isNull()
                )
                .fetchOne();

        Pageable effectivePageable = pageable.isPaged()
                ? PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), latest
                    ? Sort.by(Sort.Direction.DESC, "createdAt", "id")
                    : Sort.by(Sort.Direction.DESC, "reactionCount", "createdAt", "id"))
                : pageable;
        return new PageImpl<>(content, effectivePageable, total != null ? total : 0L);
    }

    @Override
    public Page<PlaylistSong> searchSongsWithWeight(String keyword, SpotifySearchExpansion expansion, Pageable pageable) {
        if (keyword == null || keyword.isBlank()) {
            return searchSongs(null, pageable);
        }
        List<String> spotifyTrackIds = expansion == null ? List.of() : expansion.trackIds();
        String candidates = """
                WITH matching_tracks AS (
                    SELECT track_id FROM playlist_tracks WHERE lower(title) LIKE :pattern ESCAPE '!'
                    UNION
                    SELECT ta.track_id FROM playlist_artists a
                    JOIN playlist_track_artists ta ON ta.artist_id = a.id
                    WHERE lower(a.name) LIKE :pattern ESCAPE '!'
                    UNION
                    SELECT t.track_id FROM playlist_tracks t
                    WHERE lower(t.artist) LIKE :pattern ESCAPE '!'
                      AND NOT EXISTS (SELECT 1 FROM playlist_track_artists ta WHERE ta.track_id = t.track_id)
                ), candidates AS (
                    SELECT s.id, 0 AS priority FROM playlist_songs s
                    JOIN matching_tracks t ON t.track_id = s.track_id
                    WHERE s.deleted_at IS NULL
                    UNION ALL
                    SELECT id, 0 AS priority FROM playlist_songs
                    WHERE deleted_at IS NULL AND lower(comment) LIKE :pattern ESCAPE '!'
                """;
        if (!spotifyTrackIds.isEmpty()) {
            candidates += """
                    UNION ALL
                    SELECT id, 1 AS priority FROM playlist_songs
                    WHERE deleted_at IS NULL AND track_id IN (:spotifyTrackIds)
                    """;
        }
        candidates += """
                ), best_matches AS (
                    SELECT id, min(priority) AS priority FROM candidates GROUP BY id
                )
                """;
        String pattern = "%" + keyword.toLowerCase(Locale.ROOT)
                .replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
        // Native SQL avoids the HQL parser explosion and lets each UNION branch
        // search its own indexed column. Only one page of IDs leaves the database.
        Query pageQuery = entityManager.createNativeQuery(candidates + """
                SELECT s.id, count(*) OVER () AS total
                FROM best_matches b JOIN playlist_songs s ON s.id = b.id
                ORDER BY b.priority, s.created_at DESC, s.id DESC
                """).unwrap(org.hibernate.query.NativeQuery.class)
                .addScalar("id", UUID.class).addScalar("total", Long.class);
        bindSearchParameters(pageQuery, pattern, spotifyTrackIds);
        pageQuery.setFirstResult(Math.toIntExact(pageable.getOffset()));
        pageQuery.setMaxResults(pageable.getPageSize());
        @SuppressWarnings("unchecked")
        List<Object[]> rows = pageQuery.getResultList();
        if (rows.isEmpty()) {
            // An out-of-range page still needs the real total, not zero.
            long total = 0;
            if (pageable.getOffset() > 0) {
                Query countQuery = entityManager.createNativeQuery(candidates + "SELECT count(*) FROM best_matches");
                bindSearchParameters(countQuery, pattern, spotifyTrackIds);
                total = ((Number) countQuery.getSingleResult()).longValue();
            }
            return new PageImpl<>(List.of(), pageable, total);
        }
        List<UUID> ids = rows.stream().map(row -> (UUID) row[0]).toList();
        Map<UUID, PlaylistSong> songs = queryFactory.selectFrom(playlistSong)
                .join(playlistSong.track).fetchJoin()
                .where(playlistSong.id.in(ids), playlistSong.deletedAt.isNull()).fetch().stream()
                .collect(Collectors.toMap(PlaylistSong::getId, Function.identity()));
        List<PlaylistSong> content = ids.stream().map(songs::get).filter(java.util.Objects::nonNull).toList();
        return new PageImpl<>(content, pageable, ((Number) rows.get(0)[1]).longValue());
    }

    private void bindSearchParameters(Query query, String pattern, List<String> spotifyTrackIds) {
        query.setParameter("pattern", pattern);
        if (!spotifyTrackIds.isEmpty()) query.setParameter("spotifyTrackIds", spotifyTrackIds);
    }

    @Override
    public Page<PlaylistSong> searchMySongs(java.util.UUID deviceId, Pageable pageable) {
        boolean isAsc = pageable.getSort().stream()
                .anyMatch(order -> order.getProperty().equalsIgnoreCase("createdAt") && order.isAscending());

        List<PlaylistSong> content = queryFactory
                .selectFrom(playlistSong)
                .join(playlistSong.track).fetchJoin()
                .where(
                        playlistSong.deviceId.eq(deviceId),
                        playlistSong.deletedAt.isNull()
                )
                .orderBy(isAsc ? playlistSong.createdAt.asc() : playlistSong.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(playlistSong.count())
                .from(playlistSong)
                .where(
                        playlistSong.deviceId.eq(deviceId),
                        playlistSong.deletedAt.isNull()
                )
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    private BooleanExpression containsGenre(Genre genre) {
        return genre != null ? playlistSong.genres.contains(genre) : null;
    }

    private BooleanExpression eqDeleted(Boolean isDeleted) {
        if (isDeleted == null) {
            return null;
        }
        return isDeleted ? playlistSong.deletedAt.isNotNull() : playlistSong.deletedAt.isNull();
    }
}
