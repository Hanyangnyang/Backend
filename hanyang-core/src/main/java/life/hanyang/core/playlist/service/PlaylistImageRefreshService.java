package life.hanyang.core.playlist.service;

import life.hanyang.core.global.exception.BusinessException;
import life.hanyang.core.global.exception.ErrorCode;
import life.hanyang.core.global.util.TransactionCacheEvictor;
import life.hanyang.core.playlist.client.SpotifyApiClient;
import life.hanyang.core.playlist.dto.PlaylistImageRefreshRequest;
import life.hanyang.core.playlist.dto.PlaylistImageRefreshResponse;
import life.hanyang.core.playlist.exception.SpotifyRateLimitException;
import life.hanyang.core.playlist.repository.PlaylistArtistRepository;
import life.hanyang.core.playlist.repository.PlaylistTrackRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

@Slf4j
@Service
public class PlaylistImageRefreshService {
    private final SpotifyApiClient spotify;
    private final PlaylistTrackRepository tracks;
    private final PlaylistArtistRepository artists;
    private final TransactionCacheEvictor caches;
    private final TransactionTemplate transaction;

    public PlaylistImageRefreshService(SpotifyApiClient spotify, PlaylistTrackRepository tracks,
            PlaylistArtistRepository artists, TransactionCacheEvictor caches, PlatformTransactionManager manager) {
        this.spotify = spotify;
        this.tracks = tracks;
        this.artists = artists;
        this.caches = caches;
        transaction = new TransactionTemplate(manager);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        transaction.setTimeout(5);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public PlaylistImageRefreshResponse refresh(PlaylistImageRefreshRequest request) {
        boolean retry = request.ids() != null && !request.ids().isEmpty();
        if (request.target() == null || request.limit() < 1 || request.limit() > 20
                || (request.afterId() != null && request.afterId().length() > 255)
                || (retry && (StringUtils.hasText(request.afterId()) || request.ids().size() > request.limit()
                    || request.ids().stream().anyMatch(id -> !StringUtils.hasText(id) || id.length() > 255)))) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        String cursor = request.afterId() == null ? "" : request.afterId();
        boolean track = request.target() == PlaylistImageRefreshRequest.Target.TRACK;
        List<String> ids = retry ? List.copyOf(new LinkedHashSet<>(request.ids()))
                : track ? tracks.findImageRefreshIds(cursor, PageRequest.of(0, request.limit() + 1))
                        : artists.findImageRefreshIds(cursor, PageRequest.of(0, request.limit() + 1));
        var failed = new ArrayList<String>();
        int attempted = 0, updated = 0, skipped = 0;
        Long retryAfter = null;
        try {
            for (String id : ids.subList(0, Math.min(ids.size(), request.limit()))) {
                attempted++;
                try {
                    // Network requests never hold a database write transaction or row lock.
                    String url = track ? spotify.getTrack(id).albumArtUrl() : spotify.getArtist(id).imageUrl();
                    if (!StringUtils.hasText(url)) skipped++;
                    else {
                        Integer changed = transaction.execute(status -> track
                                ? tracks.updateImageUrl(id, url, java.time.Instant.now()) : artists.updateImageUrl(id, url, java.time.Instant.now()));
                        if (changed != null && changed > 0) updated++;
                        else skipped++;
                    }
                    if (!retry) cursor = id;
                } catch (SpotifyRateLimitException exception) {
                    retryAfter = exception.getRetryAfterSeconds();
                    // Keep the scan cursor before the limited record so the next call retries it.
                    failed.addAll(ids.subList(attempted - 1, Math.min(ids.size(), request.limit())));
                    break;
                } catch (RuntimeException exception) {
                    failed.add(id);
                    if (!retry) cursor = id;
                    log.warn("[PlaylistImageRefresh] 갱신 실패 - target={}, id={}, exception={}",
                            request.target(), id, exception.getClass().getSimpleName());
                }
            }
        } finally {
            // Clear once per batch after completed per-record commits, including partial batches.
            if (updated > 0) {
                caches.evictCacheAfterCommit("playlistChart");
                caches.evictCacheAfterCommit("playlistRecommendations");
                caches.evictCacheAfterCommit("spotifyTrackSearch");
            }
        }
        return new PlaylistImageRefreshResponse(attempted, updated, skipped, List.copyOf(failed),
                retry ? null : cursor, retryAfter == null && (retry || ids.size() <= request.limit()), retryAfter);
    }
}
