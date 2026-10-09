package life.hanyang.core.playlist.service;

import life.hanyang.core.global.exception.BusinessException;
import life.hanyang.core.global.exception.ErrorCode;
import life.hanyang.core.playlist.client.SpotifyApiClient;
import life.hanyang.core.playlist.domain.PlaylistArtist;
import life.hanyang.core.playlist.domain.PlaylistTrack;
import life.hanyang.core.playlist.domain.PlaylistTrackArtist;
import life.hanyang.core.playlist.dto.PlaylistArtistBackfillRequest;
import life.hanyang.core.playlist.dto.PlaylistArtistBackfillResponse;
import life.hanyang.core.playlist.dto.SpotifyArtistMetadata;
import life.hanyang.core.playlist.event.PlaylistTrackRegisteredEvent;
import life.hanyang.core.playlist.exception.SpotifyRateLimitException;
import life.hanyang.core.playlist.repository.PlaylistArtistRepository;
import life.hanyang.core.playlist.repository.PlaylistTrackArtistRepository;
import life.hanyang.core.playlist.repository.PlaylistTrackRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
public class PlaylistArtistSyncService {
    private final SpotifyApiClient spotifyApiClient;
    private final PlaylistTrackRepository trackRepository;
    private final PlaylistArtistRepository artistRepository;
    private final PlaylistTrackArtistRepository linkRepository;
    private final TransactionTemplate writeTransaction;

    public PlaylistArtistSyncService(SpotifyApiClient spotifyApiClient, PlaylistTrackRepository trackRepository,
                                   PlaylistArtistRepository artistRepository, PlaylistTrackArtistRepository linkRepository,
                                   PlatformTransactionManager transactionManager) {
        this.spotifyApiClient = spotifyApiClient;
        this.trackRepository = trackRepository;
        this.artistRepository = artistRepository;
        this.linkRepository = linkRepository;
        this.writeTransaction = new TransactionTemplate(transactionManager);
        this.writeTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Async("scrapingTaskExecutor")
    @TransactionalEventListener
    public void onTrackRegistered(PlaylistTrackRegisteredEvent event) {
        try {
            syncMissingTrack(event.trackId());
        } catch (RuntimeException exception) {
            // Registration is already committed. Missing links remain eligible for manual backfill.
            log.warn("[PlaylistArtistSync] 연결 보류 - trackId: {}, error: {}", event.trackId(), exception.toString());
        }
    }

    public PlaylistArtistBackfillResponse backfill(PlaylistArtistBackfillRequest request) {
        if (request.limit() < 1 || request.limit() > 20
                || (request.afterTrackId() != null && request.afterTrackId().length() > 255)) {
            throw new BusinessException("백필 수량은 1~20이며 커서는 255자 이하여야 합니다.", ErrorCode.INVALID_INPUT_VALUE);
        }
        String cursor = request.afterTrackId() == null ? "" : request.afterTrackId();
        List<String> trackIds = trackRepository.findTrackIdsWithoutArtists(cursor, PageRequest.of(0, request.limit()));
        List<String> failed = new ArrayList<>();
        int attempted = 0;
        int linked = 0;
        int skipped = 0;
        Long retryAfterSeconds = null;
        for (String trackId : trackIds) {
            attempted++;
            cursor = trackId;
            try {
                if (syncMissingTrack(trackId)) linked++;
                else skipped++;
            } catch (SpotifyRateLimitException exception) {
                failed.add(trackId);
                retryAfterSeconds = exception.getRetryAfterSeconds();
                log.warn("[PlaylistArtistSync] 백필 제한 - trackId: {}, retryAfter: {}", trackId, retryAfterSeconds);
                break;
            } catch (RuntimeException exception) {
                failed.add(trackId);
                log.warn("[PlaylistArtistSync] 백필 실패 - trackId: {}, error: {}", trackId, exception.toString());
            }
        }
        // scanComplete means this cursor scan ended, not that all failed tracks have been migrated.
        // Restart from a null cursor to retry failures; linked tracks are excluded automatically.
        boolean scanComplete = retryAfterSeconds == null && trackIds.size() < request.limit();
        return new PlaylistArtistBackfillResponse(attempted, linked, skipped, List.copyOf(failed),
                cursor, scanComplete, retryAfterSeconds);
    }

    public boolean syncMissingTrack(String trackId) {
        if (linkRepository.existsByTrackTrackId(trackId)) return false;
        // Fetch the complete metadata before obtaining a database write lock.
        List<SpotifyArtistMetadata> metadata = spotifyApiClient.getTrackArtists(trackId);
        validateMetadata(metadata);
        return Boolean.TRUE.equals(writeTransaction.execute(status -> {
            PlaylistTrack track = trackRepository.findByIdForArtistSync(trackId).orElseThrow();
            // Serialize concurrent registration/backfill attempts for the same track.
            if (linkRepository.existsByTrackTrackId(trackId)) return false;

            Map<String, PlaylistArtist> artists = new HashMap<>();
            // Consistent lock ordering prevents cross-track artist upsert deadlocks.
            for (SpotifyArtistMetadata item : metadata.stream()
                    .sorted(Comparator.comparing(SpotifyArtistMetadata::spotifyArtistId)).toList()) {
                String imageUrl = StringUtils.hasText(item.imageUrl()) ? item.imageUrl() : null;
                artistRepository.upsert(UUID.randomUUID(), item.spotifyArtistId(), item.name(), imageUrl);
                artists.put(item.spotifyArtistId(), artistRepository.findBySpotifyArtistId(item.spotifyArtistId()).orElseThrow());
            }
            List<PlaylistTrackArtist> links = new ArrayList<>();
            for (int index = 0; index < metadata.size(); index++) {
                links.add(new PlaylistTrackArtist(track, artists.get(metadata.get(index).spotifyArtistId()), index));
            }
            linkRepository.saveAllAndFlush(links);
            return true;
        }));
    }

    private void validateMetadata(List<SpotifyArtistMetadata> metadata) {
        if (metadata == null || metadata.isEmpty()) throw new IllegalStateException("아티스트 응답이 비어 있습니다.");
        Set<String> ids = new HashSet<>();
        for (SpotifyArtistMetadata artist : metadata) {
            if (artist == null || artist.spotifyArtistId() == null
                    || !artist.spotifyArtistId().matches("[A-Za-z0-9]{22}")
                    || !StringUtils.hasText(artist.name()) || !ids.add(artist.spotifyArtistId())) {
                throw new IllegalStateException("아티스트 응답이 유효하지 않습니다.");
            }
        }
    }
}
