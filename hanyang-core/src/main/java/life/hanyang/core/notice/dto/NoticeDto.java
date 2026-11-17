package life.hanyang.core.notice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import life.hanyang.core.notice.domain.Notice;
import life.hanyang.core.notice.domain.NoticeTarget;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class NoticeDto {
    private NoticeDto() {}

    public record Input(
            @NotBlank String content,
            @NotNull Boolean enabled,
            @NotNull Instant publishedAt,
            Instant expiresAt,
            @NotEmpty @Size(max = 100) List<@NotNull @Valid TargetInput> targets) {}

    public record TargetInput(
            @NotBlank @Size(max = 50) String page,
            @NotNull @Size(max = 10) Map<@NotBlank String, @NotEmpty List<@NotBlank String>> conditions) {}

    public record UserResponse(Long id, String content, Instant publishedAt) {
        public UserResponse(Notice notice) {
            this(notice.getId(), notice.getContent(), notice.getPublishedAt());
        }
    }

    public record TargetResponse(Long id, String page, Map<String, List<String>> conditions) {
        public TargetResponse(NoticeTarget target) {
            this(target.getId(), target.getPage(), target.getConditions());
        }
    }

    public record AdminResponse(Long id, String content, boolean enabled,
                                Instant publishedAt, Instant expiresAt, Instant createdAt, Instant updatedAt,
                                List<TargetResponse> targets) {
        public AdminResponse(Notice notice, List<TargetResponse> targets) {
            this(notice.getId(), notice.getContent(), notice.isEnabled(), notice.getPublishedAt(),
                    notice.getExpiresAt(), notice.getCreatedAt(), notice.getUpdatedAt(), targets);
        }
    }

    public record Option(String value, String label) {}
    public record ConditionOption(String key, String label, List<Option> values) {}
    public record PageOption(String page, String label, List<ConditionOption> conditions) {}
}
