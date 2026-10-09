package life.hanyang.core.playlist.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "기기별 가수·대표곡 추천. 중복 없이 최대 5개이며 후보가 없으면 빈 목록")
public record PlaylistRecommendationResponse(List<Item> items) {
    public PlaylistRecommendationResponse {
        items = List.copyOf(items);
    }

    public enum Source { INTEREST, DISCOVERY, WEEKLY_CHART }

    public record Item(PlaylistArtistResponse artist, Track track, Source source) { }

    public record Track(String trackId, String title, String albumArtUrl, List<PlaylistArtistResponse> artists) {
        public Track {
            artists = List.copyOf(artists);
        }
    }
}
