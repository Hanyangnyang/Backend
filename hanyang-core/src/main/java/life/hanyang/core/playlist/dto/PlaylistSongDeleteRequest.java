package life.hanyang.core.playlist.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "관리자 곡 삭제 요청 DTO")
public record PlaylistSongDeleteRequest(
        @Schema(description = "삭제 사유 및 관리자 조치 메모", example = "부적절한 비속어 코멘트 포함으로 인한 삭제 조치")
        String reason
) {}
