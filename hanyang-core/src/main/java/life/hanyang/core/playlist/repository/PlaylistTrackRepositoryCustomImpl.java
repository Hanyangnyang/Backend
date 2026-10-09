package life.hanyang.core.playlist.repository;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import life.hanyang.core.playlist.dto.PlaylistTrackSearchResponse;
import life.hanyang.core.playlist.dto.PlaylistArtistResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static life.hanyang.core.playlist.domain.QPlaylistSong.playlistSong;
import static life.hanyang.core.playlist.domain.QPlaylistTrack.playlistTrack;

@RequiredArgsConstructor
public class PlaylistTrackRepositoryCustomImpl implements PlaylistTrackRepositoryCustom {
    private final JPAQueryFactory queryFactory;

    @Override
    public Page<PlaylistTrackSearchResponse> searchTracks(String keyword, Pageable pageable) {
        BooleanExpression condition = keywordCondition(keyword);

        var recommendationCount = playlistSong.count();
        var totalHearts = playlistSong.heartCount.sum().coalesce(0).longValue();
        List<Tuple> rows = queryFactory
                .select(playlistTrack, recommendationCount, totalHearts)
                .from(playlistTrack)
                .leftJoin(playlistSong).on(playlistSong.track.eq(playlistTrack).and(playlistSong.deletedAt.isNull()))
                .where(condition)
                .groupBy(playlistTrack)
                .orderBy(playlistSong.heartCount.sum().coalesce(0).desc(), playlistTrack.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        List<PlaylistTrackSearchResponse> content = rows.stream().map(row -> {
            var track = row.get(playlistTrack);
            return new PlaylistTrackSearchResponse(track.getTrackId(), track.getTitle(), track.getArtist(),
                    track.getAlbumArtUrl(), row.get(recommendationCount), row.get(totalHearts), PlaylistArtistResponse.fromTrack(track));
        }).toList();

        Long total = queryFactory
                .select(playlistTrack.count())
                .from(playlistTrack)
                .where(condition)
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    private BooleanExpression keywordCondition(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return playlistTrack.title.containsIgnoreCase(keyword)
                .or(PlaylistArtistSearchExpressions.matches(playlistTrack, name -> name.containsIgnoreCase(keyword)));
    }
}
