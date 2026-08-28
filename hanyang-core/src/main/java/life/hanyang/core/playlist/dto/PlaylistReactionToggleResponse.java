package life.hanyang.core.playlist.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import life.hanyang.core.playlist.domain.ReactionType;

import java.util.List;
import java.util.UUID;

@Schema(description = "이모지 리액션 토글 결과 응답 DTO")
public record PlaylistReactionToggleResponse(
        @Schema(description = "추천곡 ID", example = "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d")
        UUID songId,

        @Schema(description = "토글한 리액션 이모지 코드", example = "FIRE")
        ReactionType reactionType,

        @Schema(description = "토글 후 현재 기기의 반응 여부 (true: 추가됨, false: 취소됨)", example = "true")
        boolean isReacted,

        @Schema(description = "해당 곡의 10대 이모지 전체 최신 카운트 및 내 반응 상태 목록")
        List<PlaylistReactionItemResponse> reactions
) {
    public static PlaylistReactionToggleResponse of(
            UUID songId,
            ReactionType reactionType,
            boolean isReacted,
            List<PlaylistReactionItemResponse> reactions
    ) {
        return new PlaylistReactionToggleResponse(songId, reactionType, isReacted, reactions);
    }
}
