package life.hanyang.core.playlist.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import life.hanyang.core.playlist.domain.ReactionType;

@Schema(description = "개별 이모지 리액션 항목 응답 DTO")
public record PlaylistReactionItemResponse(
        @Schema(description = "리액션 이모지 코드", example = "FIRE")
        ReactionType type,

        @Schema(description = "이모지 문자", example = "🔥")
        String emoji,

        @Schema(description = "누적 리액션 개수", example = "12")
        long count,

        @Schema(description = "현재 기기가 이 리액션을 눌렀는지 여부", example = "true")
        boolean isReacted
) {
    public static PlaylistReactionItemResponse of(ReactionType type, long count, boolean isReacted) {
        return new PlaylistReactionItemResponse(type, type.getEmoji(), count, isReacted);
    }
}
