package life.hanyang.core.playlist.exception;

import life.hanyang.core.global.exception.BusinessException;
import life.hanyang.core.global.exception.ErrorCode;

public class SpotifyServiceUnavailableException extends BusinessException {

    public SpotifyServiceUnavailableException() {
        super(ErrorCode.SPOTIFY_SERVICE_UNAVAILABLE);
    }

    public SpotifyServiceUnavailableException(Throwable cause) {
        super(ErrorCode.SPOTIFY_SERVICE_UNAVAILABLE, cause);
    }
}
