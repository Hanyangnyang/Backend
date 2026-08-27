package life.hanyang.core.playlist.service;

import life.hanyang.core.global.exception.BusinessException;
import life.hanyang.core.global.exception.EntityNotFoundException;
import life.hanyang.core.global.exception.ErrorCode;
import life.hanyang.core.playlist.domain.Genre;
import life.hanyang.core.playlist.domain.PlaylistSong;
import life.hanyang.core.playlist.domain.PlaylistSongLike;
import life.hanyang.core.playlist.domain.PlaylistSongReport;
import life.hanyang.core.playlist.dto.*;
import life.hanyang.core.playlist.repository.PlaylistSongLikeRepository;
import life.hanyang.core.playlist.repository.PlaylistSongReportRepository;
import life.hanyang.core.playlist.repository.PlaylistSongRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlaylistService {

    private final PlaylistSongRepository playlistSongRepository;
    private final PlaylistSongLikeRepository playlistSongLikeRepository;
    private final PlaylistSongReportRepository playlistSongReportRepository;

    /**
     * 1. 곡 추천/등록
     */
    @Transactional
    public PlaylistSongResponse createSong(PlaylistSongCreateRequest request, String clientIp) {
        if (request.genres() == null || request.genres().isEmpty() || request.genres().size() > 3) {
            throw new BusinessException("장르는 최소 1개에서 최대 3개까지 선택해야 합니다.", ErrorCode.INVALID_INPUT_VALUE);
        }

        PlaylistSong song = PlaylistSong.builder()
                .trackId(request.trackId())
                .title(request.title())
                .artist(request.artist())
                .albumArtUrl(request.albumArtUrl())
                .comment(request.comment())
                .deviceId(request.deviceId())
                .ipAddress(clientIp != null ? clientIp : "UNKNOWN")
                .genres(request.genres())
                .build();

        PlaylistSong saved = playlistSongRepository.save(song);
        return PlaylistSongResponse.of(saved, false);
    }

    /**
     * 2. 피드 목록 조회 (장르 선택 필터, 최신순 페이징, isLiked 배치 조회)
     */
    public Page<PlaylistSongResponse> getFeedSongs(Genre genre, Pageable pageable, UUID currentDeviceId) {
        Page<PlaylistSong> songPage = playlistSongRepository.searchSongs(genre, pageable);
        List<PlaylistSong> songs = songPage.getContent();

        if (songs.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, songPage.getTotalElements());
        }

        // isLiked N+1 방지를 위한 1번의 Batch IN 쿼리
        Set<UUID> likedSongIds = (currentDeviceId != null)
                ? playlistSongLikeRepository.findLikedSongIdsByDeviceIdAndSongIdIn(
                        currentDeviceId,
                        songs.stream().map(PlaylistSong::getId).toList()
                )
                : Collections.emptySet();

        List<PlaylistSongResponse> responses = songs.stream()
                .map(song -> PlaylistSongResponse.of(song, likedSongIds.contains(song.getId())))
                .toList();

        return new PageImpl<>(responses, pageable, songPage.getTotalElements());
    }


    /**
     * 4. 좋아요 토글 (동시성 제어 및 원자적 카운트 증감)
     */
    @Transactional
    public PlaylistLikeToggleResponse toggleLike(UUID songId, UUID deviceId) {
        PlaylistSong song = playlistSongRepository.findByIdAndDeletedAtIsNull(songId)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않거나 삭제된 곡입니다. id: " + songId));

        Optional<PlaylistSongLike> existingLike = playlistSongLikeRepository.findBySongIdAndDeviceId(songId, deviceId);

        boolean isLiked;
        if (existingLike.isPresent()) {
            // [좋아요 취소]
            playlistSongLikeRepository.delete(existingLike.get());
            playlistSongRepository.decrementHeartCount(songId);
            isLiked = false;
        } else {
            // [좋아요 등록]
            try {
                PlaylistSongLike newLike = PlaylistSongLike.builder()
                        .song(song)
                        .deviceId(deviceId)
                        .build();
                playlistSongLikeRepository.save(newLike);
                playlistSongRepository.incrementHeartCount(songId);
                isLiked = true;
            } catch (DataIntegrityViolationException e) {
                log.warn("[PlaylistLike] 중복 좋아요 요청 감지 (동시성 방어됨) - songId: {}, deviceId: {}", songId, deviceId);
                isLiked = true;
            }
        }

        Integer currentHeartCount = playlistSongRepository.getHeartCount(songId).orElse(0);
        return new PlaylistLikeToggleResponse(isLiked, currentHeartCount);
    }

    /**
     * 5. 내가 좋아요 누른 곡 목록 조회
     */
    public Page<PlaylistSongResponse> getLikedSongs(UUID deviceId, Pageable pageable) {
        Page<PlaylistSong> likedSongs = playlistSongLikeRepository.findLikedSongsByDeviceId(deviceId, pageable);
        List<PlaylistSongResponse> responses = likedSongs.getContent().stream()
                .map(song -> PlaylistSongResponse.of(song, true))
                .toList();

        return new PageImpl<>(responses, pageable, likedSongs.getTotalElements());
    }

    /**
     * 6. 곡 신고하기
     */
    @Transactional
    public PlaylistSongReportResponse reportSong(UUID songId, PlaylistSongReportRequest request) {
        PlaylistSong song = playlistSongRepository.findByIdAndDeletedAtIsNull(songId)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않거나 삭제된 곡입니다. id: " + songId));

        PlaylistSongReport report = PlaylistSongReport.builder()
                .song(song)
                .reporterDeviceId(request.reporterDeviceId())
                .reason(request.reason())
                .build();

        PlaylistSongReport saved = playlistSongReportRepository.save(report);
        return PlaylistSongReportResponse.from(saved);
    }
}
