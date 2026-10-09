package life.hanyang.core.playlist.service;

import life.hanyang.core.playlist.dto.PlaylistLikeToggleResponse;
import life.hanyang.core.playlist.dto.SpotifyTrackSearchResponse;
import life.hanyang.core.playlist.repository.PlaylistTrackLikeRepository;
import life.hanyang.core.playlist.repository.PlaylistTrackRepository;
import life.hanyang.core.playlist.event.PlaylistTrackRegisteredEvent;
import org.springframework.context.ApplicationEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PlaylistTrackLikeService {

    private final PlaylistTrackRepository playlistTrackRepository;
    private final PlaylistTrackLikeRepository playlistTrackLikeRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public PlaylistLikeToggleResponse toggle(String trackId, UUID deviceId, SpotifyTrackSearchResponse metadata) {
        if (metadata != null) {
            // 같은 트랙의 최초 좋아요 요청이 겹쳐도 트랙은 한 번만 등록한다.
            if (playlistTrackRepository.insertIfAbsent(
                    trackId, metadata.title(), metadata.artist(), metadata.albumArtUrl()) > 0) {
                eventPublisher.publishEvent(new PlaylistTrackRegisteredEvent(trackId));
            }
        }

        boolean isLiked;
        if (playlistTrackLikeRepository.deleteIfPresent(trackId, deviceId) > 0) {
            // [좋아요 취소]
            playlistTrackRepository.decrementLikeCount(trackId);
            isLiked = false;
        } else {
            // [좋아요 등록]
            if (playlistTrackLikeRepository.insertIfAbsent(trackId, deviceId) > 0) {
                playlistTrackRepository.incrementLikeCount(trackId);
                isLiked = true;
            } else {
                isLiked = true;
            }
        }

        Integer currentLikeCount = playlistTrackRepository.getLikeCount(trackId).orElse(0);
        return new PlaylistLikeToggleResponse(isLiked, currentLikeCount);
    }
}
