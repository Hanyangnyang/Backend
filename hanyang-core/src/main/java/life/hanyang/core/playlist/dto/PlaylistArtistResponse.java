package life.hanyang.core.playlist.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import life.hanyang.core.playlist.domain.PlaylistArtist;
import life.hanyang.core.playlist.domain.PlaylistTrack;

import java.io.Serializable;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public record PlaylistArtistResponse(
        @Schema(description = "내부 아티스트 UUID. DB에 없는 카탈로그 검색 결과는 null", nullable = true) UUID id,
        @Schema(description = "Spotify 아티스트 ID") String spotifyArtistId,
        @Schema(description = "아티스트 이름") String name,
        @Schema(description = "아티스트 사진 URL. 사진이 없으면 null", nullable = true) String imageUrl
) implements Serializable {
    public static PlaylistArtistResponse from(PlaylistArtist artist) {
        return new PlaylistArtistResponse(artist.getId(), artist.getSpotifyArtistId(), artist.getName(), artist.getImageUrl());
    }

    public static List<PlaylistArtistResponse> fromTrack(PlaylistTrack track) {
        if (track == null) return List.of();
        return track.getArtistLinks().stream()
                .sorted(Comparator.comparingInt(link -> link.getArtistOrder()))
                .map(link -> from(link.getArtist())).toList();
    }
}
