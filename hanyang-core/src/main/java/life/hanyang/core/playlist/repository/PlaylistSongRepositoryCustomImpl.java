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
