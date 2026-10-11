package life.hanyang.core.playlist.exception;

import life.hanyang.core.global.exception.ErrorCode;

public class SpotifyRateLimitException extends SpotifyServiceUnavailableException {

    private final long retryAfterSeconds;

    public SpotifyRateLimitException(long retryAfterSeconds, Throwable cause) {
        this(retryAfterSeconds, false, cause);
    }

    public SpotifyRateLimitException(long retryAfterSeconds, boolean quotaExceeded, Throwable cause) {
        super(quotaExceeded ? ErrorCode.SPOTIFY_QUOTA_EXCEEDED : ErrorCode.SPOTIFY_RATE_LIMITED, cause);
        this.retryAfterSeconds = Math.max(retryAfterSeconds, 1);
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
