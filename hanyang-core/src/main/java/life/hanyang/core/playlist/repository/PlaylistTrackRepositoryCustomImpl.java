package life.hanyang.core.playlist.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import life.hanyang.core.playlist.dto.PlaylistTrackSearchResponse;
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

        List<PlaylistTrackSearchResponse> content = queryFactory
                .select(Projections.constructor(
                        PlaylistTrackSearchResponse.class,
                        playlistTrack.trackId,
                        playlistTrack.title,
                        playlistTrack.artist,
                        playlistTrack.albumArtUrl,
                        playlistSong.count(),
                        playlistSong.heartCount.sum().coalesce(0).longValue()
                ))
                .from(playlistTrack)
                .leftJoin(playlistSong).on(playlistSong.track.eq(playlistTrack).and(playlistSong.deletedAt.isNull()))
                .where(condition)
                .groupBy(playlistTrack.trackId, playlistTrack.title, playlistTrack.artist, playlistTrack.albumArtUrl)
                .orderBy(playlistSong.heartCount.sum().coalesce(0).desc(), playlistTrack.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

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
                .or(playlistTrack.artist.containsIgnoreCase(keyword));
    }
}
