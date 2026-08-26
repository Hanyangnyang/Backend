package life.hanyang.core.playlist.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record PlaylistLikeToggleRequest(
        @NotNull(message = "사용자 ID는 필수입니다.")
        UUID userId
) {}
