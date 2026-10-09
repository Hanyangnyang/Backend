package life.hanyang.core.playlist.dto;

import java.util.List;

public record PlaylistArtistBackfillResponse(
        int attempted,
        int linked,
        int skipped,
        List<String> failedTrackIds,
        String nextAfterTrackId,
        boolean scanComplete,
        Long retryAfterSeconds
) {
}
