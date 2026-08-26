package life.hanyang.core.playlist.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record PlaylistSongReportRequest(
        @NotNull(message = "신고자 유저 ID는 필수입니다.")
        UUID reporterUserId,

        @NotBlank(message = "신고 사유는 필수입니다.")
        String reason
) {}
