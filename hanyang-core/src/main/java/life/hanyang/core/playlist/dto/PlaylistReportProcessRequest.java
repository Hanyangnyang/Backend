package life.hanyang.core.playlist.dto;

import jakarta.validation.constraints.NotNull;
import life.hanyang.core.playlist.domain.ReportStatus;

public record PlaylistReportProcessRequest(
        @NotNull(message = "처리 상태는 필수입니다.")
        ReportStatus status,

        String adminMemo
) {}
