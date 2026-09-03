package life.hanyang.core.playlist.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.core.types.dsl.StringExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import life.hanyang.core.playlist.domain.Genre;
import life.hanyang.core.playlist.domain.PlaylistSong;
import life.hanyang.core.playlist.dto.SpotifySearchExpansion;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static life.hanyang.core.playlist.domain.QPlaylistSong.playlistSong;

@RequiredArgsConstructor
public class PlaylistSongRepositoryCustomImpl implements PlaylistSongRepositoryCustom {
    private final JPAQueryFactory queryFactory;

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
        List<PlaylistSong> content = queryFactory
                .selectFrom(playlistSong)
                .join(playlistSong.track).fetchJoin()
                .where(
                        playlistSong.track.trackId.eq(trackId),
                        playlistSong.deletedAt.isNull()
                )
                .orderBy(playlistSong.heartCount.desc(), playlistSong.createdAt.desc())
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

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    @Override
    public Page<PlaylistSong> searchSongsWithWeight(String keyword, SpotifySearchExpansion expansion, Pageable pageable) {
        if (keyword == null || keyword.isBlank()) {
            return searchSongs(null, pageable);
        }

        SpotifySearchExpansion safeExpansion = expansion != null ? expansion : SpotifySearchExpansion.empty();
        BooleanExpression directMatchCondition = playlistSong.track.title.containsIgnoreCase(keyword)
                .or(playlistSong.track.artist.containsIgnoreCase(keyword))
                .or(playlistSong.comment.containsIgnoreCase(keyword));
        BooleanExpression matchCondition = directMatchCondition.or(spotifyMatch(safeExpansion));

        NumberExpression<Integer> matchPriority = new CaseBuilder()
                .when(playlistSong.track.title.equalsIgnoreCase(keyword)).then(1)
                .when(playlistSong.track.artist.equalsIgnoreCase(keyword)).then(2)
                .when(playlistSong.track.title.startsWithIgnoreCase(keyword)).then(3)
                .when(playlistSong.track.artist.startsWithIgnoreCase(keyword)).then(4)
                .when(playlistSong.track.title.containsIgnoreCase(keyword)).then(5)
                .when(playlistSong.track.artist.containsIgnoreCase(keyword)).then(6)
                .when(matchesTrackId(safeExpansion.trackIds())).then(7)
                .when(matchesText(playlistSong.track.title, safeExpansion.titles(), false)).then(8)
                .when(matchesText(playlistSong.track.artist, safeExpansion.artists(), true)).then(9)
                .when(playlistSong.comment.containsIgnoreCase(keyword)).then(10)
                .otherwise(11);
        NumberExpression<Integer> spotifyTrackRank = rankedTrackId(safeExpansion.trackIds());
        NumberExpression<Integer> spotifyTitleRank = rankedText(playlistSong.track.title, safeExpansion.titles(), false);
        NumberExpression<Integer> spotifyArtistRank = rankedText(playlistSong.track.artist, safeExpansion.artists(), true);

        List<PlaylistSong> content = queryFactory
                .selectFrom(playlistSong)
                .join(playlistSong.track).fetchJoin()
                .where(
                        playlistSong.deletedAt.isNull(),
                        matchCondition
                )
                .orderBy(
                        matchPriority.asc(),
                        spotifyTrackRank.asc(),
                        spotifyTitleRank.asc(),
                        spotifyArtistRank.asc(),
                        playlistSong.createdAt.desc()
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(playlistSong.count())
                .from(playlistSong)
                .where(
                        playlistSong.deletedAt.isNull(),
                        matchCondition
                )
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    private BooleanExpression spotifyMatch(SpotifySearchExpansion expansion) {
        return matchesTrackId(expansion.trackIds())
                .or(matchesText(playlistSong.track.title, expansion.titles(), false))
                .or(matchesText(playlistSong.track.artist, expansion.artists(), true));
    }

    private BooleanExpression matchesTrackId(List<String> trackIds) {
        return trackIds.isEmpty()
                ? alwaysFalse(playlistSong.track.trackId)
                : playlistSong.track.trackId.in(trackIds);
    }

    private BooleanExpression matchesText(StringExpression field, List<String> values, boolean contains) {
        BooleanExpression condition = alwaysFalse(field);
        for (String value : values) {
            condition = condition.or(contains ? field.containsIgnoreCase(value) : field.equalsIgnoreCase(value));
        }
        return condition;
    }

    private NumberExpression<Integer> rankedTrackId(List<String> rankedTrackIds) {
        if (rankedTrackIds.isEmpty()) {
            return rankedNever(playlistSong.track.trackId);
        }

        CaseBuilder.Cases<Integer, NumberExpression<Integer>> cases = new CaseBuilder()
                .when(playlistSong.track.trackId.eq(rankedTrackIds.get(0)))
                .then(1);
        for (int index = 1; index < rankedTrackIds.size(); index++) {
            cases = cases.when(playlistSong.track.trackId.eq(rankedTrackIds.get(index))).then(index + 1);
        }
        return cases.otherwise(Integer.MAX_VALUE);
    }

    private NumberExpression<Integer> rankedText(StringExpression field, List<String> values, boolean contains) {
        if (values.isEmpty()) {
            return rankedNever(field);
        }

        BooleanExpression firstMatch = contains
                ? field.containsIgnoreCase(values.get(0))
                : field.equalsIgnoreCase(values.get(0));
        CaseBuilder.Cases<Integer, NumberExpression<Integer>> cases = new CaseBuilder()
                .when(firstMatch)
                .then(1);
        for (int index = 1; index < values.size(); index++) {
            BooleanExpression match = contains
                    ? field.containsIgnoreCase(values.get(index))
                    : field.equalsIgnoreCase(values.get(index));
            cases = cases.when(match).then(index + 1);
        }
        return cases.otherwise(Integer.MAX_VALUE);
    }

    private BooleanExpression alwaysFalse(StringExpression field) {
        return field.isNull().and(field.isNotNull());
    }

    private NumberExpression<Integer> rankedNever(StringExpression field) {
        return new CaseBuilder().when(alwaysFalse(field)).then(0).otherwise(Integer.MAX_VALUE);
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
