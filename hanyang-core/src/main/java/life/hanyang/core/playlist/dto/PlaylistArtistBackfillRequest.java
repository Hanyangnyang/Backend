package life.hanyang.core.playlist.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

@Schema(description = "아티스트 백필 요청. 첫 실행은 afterTrackId를 생략하고, 다음 실행은 응답의 nextAfterTrackId를 전달합니다.",
        example = "{\"limit\": 5}")
public record PlaylistArtistBackfillRequest(
        @Schema(description = "첫 실행 및 실패 건 재시도 시 생략하거나 null. 다음 구간 처리 시 직전 응답의 nextAfterTrackId를 입력합니다.",
                nullable = true)
        @Size(max = 255) String afterTrackId,
        @Schema(description = "이번 요청에서 처리할 최대 곡 수 (1~20)", example = "5")
        @Min(1) @Max(20) int limit
) {
}
