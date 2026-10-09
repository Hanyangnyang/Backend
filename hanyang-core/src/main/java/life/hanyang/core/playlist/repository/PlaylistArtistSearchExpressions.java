package life.hanyang.core.playlist.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.StringExpression;
import com.querydsl.jpa.JPAExpressions;
import life.hanyang.core.playlist.domain.QPlaylistTrack;
import life.hanyang.core.playlist.domain.QPlaylistTrackArtist;

import java.util.function.Function;

final class PlaylistArtistSearchExpressions {
    private PlaylistArtistSearchExpressions() {
    }

    static BooleanExpression matches(QPlaylistTrack track, Function<StringExpression, BooleanExpression> match) {
        QPlaylistTrackArtist link = new QPlaylistTrackArtist("artistSearchLink");
        BooleanExpression linkedMatch = JPAExpressions.selectOne().from(link)
                .where(link.track.eq(track), match.apply(link.artist.name)).exists();
        BooleanExpression hasLinks = JPAExpressions.selectOne().from(link).where(link.track.eq(track)).exists();
        // EXISTS keeps a multi-artist track from duplicating posts, counts or pagination rows.
        return linkedMatch.or(hasLinks.not().and(match.apply(track.artist)));
    }
}
