package life.hanyang.core.playlist.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import life.hanyang.core.playlist.domain.Genre;

import java.util.Set;
import java.util.UUID;

public record PlaylistSongCreateRequest(
        @NotBlank(message = "Spotify 트랙 ID는 필수입니다.")
        String trackId,

        @NotBlank(message = "곡 제목은 필수입니다.")
        String title,

        @NotBlank(message = "아티스트명은 필수입니다.")
        String artist,

        String albumArtUrl,

        String comment,

        @NotNull(message = "사용자 ID는 필수입니다.")
        UUID userId,

        @NotEmpty(message = "장르는 최소 1개 이상 선택해야 합니다.")
        @Size(min = 1, max = 3, message = "장르는 최소 1개에서 최대 3개까지 선택 가능합니다.")
        Set<Genre> genres
) {}
