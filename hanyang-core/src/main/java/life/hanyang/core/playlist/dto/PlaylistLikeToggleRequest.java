package life.hanyang.core.playlist.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Schema(description = "좋아요 토글 요청 DTO")
public record PlaylistLikeToggleRequest(
        @Schema(description = "기기 식별자 ID (UUID)", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        @NotNull(message = "기기 식별자 ID는 필수입니다.")
        UUID deviceId
) {}
