package life.hanyang.core.playlist.exception;

public class SpotifyRateLimitException extends SpotifyServiceUnavailableException {

    private final long retryAfterSeconds;

    public SpotifyRateLimitException(long retryAfterSeconds, Throwable cause) {
        super(cause);
        this.retryAfterSeconds = Math.max(retryAfterSeconds, 1);
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
