package life.hanyang.core.playlist.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record PlaylistTrackPlayRequest(
        @Schema(description = "기기 식별자 ID (UUID)")
        @NotNull(message = "기기 식별자 ID는 필수입니다.") UUID deviceId
) {}
