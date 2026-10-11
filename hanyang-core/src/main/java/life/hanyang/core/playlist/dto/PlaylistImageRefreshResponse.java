package life.hanyang.core.playlist.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record PlaylistImageRefreshResponse(
        int attempted, int updated, int skipped,
        List<String> failedIds,
        @Schema(description = "다음 순차 요청의 afterId. 제한에 걸린 ID는 커서가 진행하지 않아 다음 호출에서 재시도됩니다.") String nextAfterId,
        @Schema(description = "해당 구간 조회 완료. 실패 건까지 완료된 의미는 아닙니다.") boolean scanComplete,
        Long retryAfterSeconds
) {}
