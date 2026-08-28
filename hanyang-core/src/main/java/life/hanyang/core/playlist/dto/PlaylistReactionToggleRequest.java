package life.hanyang.core.playlist.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import life.hanyang.core.playlist.domain.ReactionType;

import java.util.UUID;

@Schema(description = "이모지 리액션 토글(등록/취소) 요청 DTO")
public record PlaylistReactionToggleRequest(
        @NotNull(message = "기기 식별자 ID는 필수입니다.")
        @Schema(description = "기기 식별자 ID", example = "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d")
        UUID deviceId,

        @NotNull(message = "리액션 이모지 유형은 필수입니다.")
        @Schema(description = "리액션 이모지 유형 (LOVE, EMOTIONAL, BITTERSWEET, TIPSY, COOL, FIRE, ROCK, DANCE, THUMBS_UP, BEER)", example = "FIRE")
        ReactionType reactionType
) {
}
