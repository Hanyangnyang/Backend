package life.hanyang.core.playlist.dto;

public record PlaylistLikeToggleResponse(
        boolean isLiked,
        Integer likeCount
) {}
