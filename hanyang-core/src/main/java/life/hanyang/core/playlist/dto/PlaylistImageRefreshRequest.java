package life.hanyang.core.playlist.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.util.List;

@Schema(description = "첫 호출은 afterId 생략. 응답 nextAfterId로 이어서 실행. 실패한 Spotify ID는 ids에 넣어 재시도.",
        example = "{\"target\":\"TRACK\",\"limit\":5}")
public record PlaylistImageRefreshRequest(
        @NotNull @Schema(description = "TRACK: 앨범 이미지, ARTIST: 가수 사진") Target target,
        @Min(1) @Max(20) @Schema(description = "요청당 최대 처리 수", example = "5") int limit,
        @Size(max = 255) @Schema(description = "직전 응답 nextAfterId. 첫 요청은 생략", nullable = true) String afterId,
        @Size(max = 20) @Schema(description = "실패 건 재시도용 Spotify ID 목록. afterId와 함께 사용 불가", nullable = true)
        List<@NotBlank @Size(max = 255) String> ids
) {
    public enum Target { TRACK, ARTIST }
}
