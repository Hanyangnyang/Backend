package life.hanyang.core.playlist.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PlaylistCommentRequest(@NotBlank @Size(max = MAX_CONTENT_LENGTH) String content) {
    public static final int MAX_CONTENT_LENGTH = 200;
}
