package life.hanyang.core.playlist.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Schema(description = "곡 신고 요청 DTO")
public record PlaylistSongReportRequest(
        @Schema(description = "신고자 기기 식별자 ID (UUID)", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        @NotNull(message = "신고자 기기 식별자 ID는 필수입니다.")
        UUID reporterDeviceId,

        @Schema(description = "신고 사유", example = "부적절하거나 유해한 멘트가 포함되어 있습니다.")
        @NotBlank(message = "신고 사유는 필수입니다.")
        String reason
) {}
