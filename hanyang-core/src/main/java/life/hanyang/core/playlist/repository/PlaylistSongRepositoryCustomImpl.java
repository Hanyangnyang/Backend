package life.hanyang.core.playlist.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import life.hanyang.core.playlist.domain.Genre;
import life.hanyang.core.playlist.domain.PlaylistSong;
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
    public Page<PlaylistSong> searchSongsWithWeight(String keyword, Pageable pageable) {
        if (keyword == null || keyword.isBlank()) {
            return searchSongs(null, pageable);
        }

        BooleanExpression matchCondition = playlistSong.track.title.containsIgnoreCase(keyword)
                .or(playlistSong.track.artist.containsIgnoreCase(keyword))
                .or(playlistSong.comment.containsIgnoreCase(keyword));

        // 💡 가중치 계산: 곡 제목(100점) > 가수명(80점) > 코멘트 내용(20점)
        com.querydsl.core.types.dsl.NumberExpression<Integer> relevanceScore = new com.querydsl.core.types.dsl.CaseBuilder()
                .when(playlistSong.track.title.containsIgnoreCase(keyword)).then(100)
                .when(playlistSong.track.artist.containsIgnoreCase(keyword)).then(80)
                .when(playlistSong.comment.containsIgnoreCase(keyword)).then(20)
                .otherwise(0);

        List<PlaylistSong> content = queryFactory
                .selectFrom(playlistSong)
                .join(playlistSong.track).fetchJoin()
                .where(
                        playlistSong.deletedAt.isNull(),
                        matchCondition
                )
                .orderBy(
                        relevanceScore.desc(),
                        playlistSong.heartCount.desc(),
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
