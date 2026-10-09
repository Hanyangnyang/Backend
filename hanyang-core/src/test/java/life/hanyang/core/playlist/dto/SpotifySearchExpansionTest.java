package life.hanyang.core.playlist.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SpotifySearchExpansionTest {

    @Test
    void keepsCoartistsAsSeparateCandidatesRatherThanSplittingTheirNames() {
        var track = new SpotifyTrackSearchResponse("track", "곡", "Earth, Wind & Fire, 가수 B", null, 1,
                List.of(new PlaylistArtistResponse(null, "a", "Earth, Wind & Fire", null),
                        new PlaylistArtistResponse(null, "b", "가수 B", null)));
        assertThat(SpotifySearchExpansion.from(List.of(track)).artists()).containsExactly("Earth, Wind & Fire", "가수 B");
    }

    @Test
    @DisplayName("Spotify 순위 순으로 검색 후보를 만들고 제목과 가수 중복을 제거한다")
    void from_DeduplicatesWhilePreservingRank() {
        List<SpotifyTrackSearchResponse> tracks = List.of(
                new SpotifyTrackSearchResponse("track-2", "후라이의 꿈", "AKMU", null, 2, List.of()),
                new SpotifyTrackSearchResponse("track-1", "Love Lee", "AKMU", null, 1, List.of()),
                new SpotifyTrackSearchResponse("track-1", "Love Lee", "AKMU", null, 3, List.of())
        );

        SpotifySearchExpansion expansion = SpotifySearchExpansion.from(tracks);

        assertThat(expansion.trackIds()).containsExactly("track-1", "track-2");
        assertThat(expansion.titles()).containsExactly("Love Lee", "후라이의 꿈");
        assertThat(expansion.artists()).containsExactly("AKMU");
    }
}
