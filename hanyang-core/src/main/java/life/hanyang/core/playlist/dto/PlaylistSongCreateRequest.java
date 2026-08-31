package life.hanyang.core.playlist.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import life.hanyang.core.playlist.domain.Genre;

import java.util.Set;
import java.util.UUID;

@Schema(description = "곡 추천 및 등록 요청 DTO")
public record PlaylistSongCreateRequest(
        @Schema(description = "Spotify 트랙 ID", example = "4cOdK2wGLETKBW3PvgPWqT")
        @NotBlank(message = "Spotify 트랙 ID는 필수입니다.")
        String trackId,

        @Schema(description = "곡 제목", example = "Ditto")
        @NotBlank(message = "곡 제목은 필수입니다.")
        String title,

        @Schema(description = "아티스트명", example = "NewJeans")
        @NotBlank(message = "아티스트명은 필수입니다.")
        String artist,

        @Schema(description = "앨범 커버 이미지 URL", example = "https://i.scdn.co/image/ab67616d0000b273bd15713cf9824b7842bcd290")
        @Pattern(
                regexp = "^https://i\\.scdn\\.co/image/[a-fA-F0-9]{40}$",
                message = "스포티파이 공식 이미지 URL 형식이어야 합니다."
        )
        String albumArtUrl,

        @Schema(description = "추천 멘트 및 코멘트", example = "과제할 때 집중하기 좋아요!")
        String comment,

        @Schema(description = "등록 기기 식별자 ID (UUID)", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        @NotNull(message = "기기 식별자 ID는 필수입니다.")
        UUID deviceId,

        @Schema(
                description = "선택 장르 목록 (최소 1개 ~ 최대 3개 선택 가능)\n" +
                        "• KPOP : K-POP\n" +
                        "• BAND : 밴드\n" +
                        "• ROCK : 락\n" +
                        "• R_AND_B : R&B\n" +
                        "• HIPHOP : 힙합\n" +
                        "• INDIE : 인디\n" +
                        "• BALLAD : 발라드\n" +
                        "• POP : POP\n" +
                        "• JPOP : J-POP\n" +
                        "• OTHER : 기타",
                example = "[\"KPOP\", \"BAND\"]"
        )
        @NotEmpty(message = "장르는 최소 1개 이상 선택해야 합니다.")
        @Size(min = 1, max = 3, message = "장르는 최소 1개에서 최대 3개까지 선택 가능합니다.")
        Set<Genre> genres
) {}
