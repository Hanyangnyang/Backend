package life.hanyang.core.playlist.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PlaylistCommentReportRequest(@NotBlank @Size(max = 500) String reason) {}
