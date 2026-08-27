package life.hanyang.core.playlist.service;

import life.hanyang.core.global.exception.BusinessException;
import life.hanyang.core.global.exception.EntityNotFoundException;
import life.hanyang.core.playlist.domain.*;
import life.hanyang.core.playlist.dto.*;
import life.hanyang.core.playlist.repository.PlaylistSongLikeRepository;
import life.hanyang.core.playlist.repository.PlaylistSongReportRepository;
import life.hanyang.core.playlist.repository.PlaylistSongRepository;
import life.hanyang.core.playlist.repository.PlaylistTrackRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PlaylistServiceTest {

    @Mock
    private PlaylistTrackRepository playlistTrackRepository;

    @Mock
    private PlaylistSongRepository playlistSongRepository;

    @Mock
    private PlaylistSongLikeRepository playlistSongLikeRepository;

    @Mock
    private PlaylistSongReportRepository playlistSongReportRepository;

    @InjectMocks
    private PlaylistService playlistService;

    @Test
    @DisplayName("곡 등록 성공")
    void createSong_Success() {
        // given
        UUID deviceId = UUID.randomUUID();
        PlaylistSongCreateRequest request = new PlaylistSongCreateRequest(
                "track-123", "Ditto", "NewJeans", "https://image.url", "좋아요", deviceId, Set.of(Genre.KPOP)
        );

        PlaylistTrack track = PlaylistTrack.builder()
                .trackId(request.trackId())
                .title(request.title())
                .artist(request.artist())
                .albumArtUrl(request.albumArtUrl())
                .build();

        PlaylistSong song = PlaylistSong.builder()
                .track(track)
                .comment(request.comment())
                .deviceId(request.deviceId())
                .ipAddress("127.0.0.1")
                .genres(request.genres())
                .build();

        given(playlistSongRepository.countByDeviceIdAndCreatedAtAfterAndDeletedAtIsNull(any(), any())).willReturn(0L);
        given(playlistSongRepository.existsByDeviceIdAndTrackTrackIdAndCreatedAtAfterAndDeletedAtIsNull(any(), any(), any())).willReturn(false);
        given(playlistTrackRepository.findById(request.trackId())).willReturn(Optional.empty());
        given(playlistTrackRepository.save(any(PlaylistTrack.class))).willReturn(track);
        given(playlistSongRepository.save(any(PlaylistSong.class))).willReturn(song);

        // when
        PlaylistSongResponse response = playlistService.createSong(request, "127.0.0.1");

        // then
        assertThat(response.title()).isEqualTo("Ditto");
        assertThat(response.artist()).isEqualTo("NewJeans");
        assertThat(response.genres()).containsExactly(Genre.KPOP);
        assertThat(response.isLiked()).isFalse();
    }

    @Test
    @DisplayName("하루에 3곡 이상 등록 시 예외가 발생한다")
    void createSong_ThrowsException_WhenDailyLimitExceeded() {
        // given
        UUID deviceId = UUID.randomUUID();
        PlaylistSongCreateRequest request = new PlaylistSongCreateRequest(
                "track-123", "Ditto", "NewJeans", "https://image.url", "좋아요", deviceId, Set.of(Genre.KPOP)
        );

        given(playlistSongRepository.countByDeviceIdAndCreatedAtAfterAndDeletedAtIsNull(any(), any())).willReturn(3L);

        // when & then
        assertThatThrownBy(() -> playlistService.createSong(request, "127.0.0.1"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("오늘 추천 가능한 곡 수(최대 3곡)를 초과했습니다.");
    }

    @Test
    @DisplayName("최근 7일 이내에 이미 추천한 곡이면 예외가 발생한다")
    void createSong_ThrowsException_WhenDuplicateSongIn7Days() {
        // given
        UUID deviceId = UUID.randomUUID();
        PlaylistSongCreateRequest request = new PlaylistSongCreateRequest(
                "track-123", "Ditto", "NewJeans", "https://image.url", "좋아요", deviceId, Set.of(Genre.KPOP)
        );

        given(playlistSongRepository.countByDeviceIdAndCreatedAtAfterAndDeletedAtIsNull(any(), any())).willReturn(1L);
        given(playlistSongRepository.existsByDeviceIdAndTrackTrackIdAndCreatedAtAfterAndDeletedAtIsNull(any(), any(), any())).willReturn(true);

        // when & then
        assertThatThrownBy(() -> playlistService.createSong(request, "127.0.0.1"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("최근 7일 이내에 이미 추천한 곡입니다.");
    }

    @Test
    @DisplayName("기기 상태 사전 조회 시 잔여 횟수 및 최근 트랙 목록이 정상 반환된다")
    void getCreationStatus_Success() {
        // given
        UUID deviceId = UUID.randomUUID();
        given(playlistSongRepository.countByDeviceIdAndCreatedAtAfterAndDeletedAtIsNull(any(), any())).willReturn(1L);
        given(playlistSongRepository.findRecentTrackIdsByDeviceIdAndCreatedAtAfter(any(), any())).willReturn(Set.of("track-123"));

        // when
        PlaylistCreationStatusResponse status = playlistService.getCreationStatus(deviceId);

        // then
        assertThat(status.canCreate()).isTrue();
        assertThat(status.dailyCount()).isEqualTo(1L);
        assertThat(status.remainingCount()).isEqualTo(2L);
        assertThat(status.dailyMaxLimit()).isEqualTo(3);
        assertThat(status.recentTrackIdsIn7Days()).containsExactly("track-123");
    }

    @Test
    @DisplayName("장르가 비어있으면 곡 등록 시 예외가 발생한다")
    void createSong_ThrowsException_WhenGenresEmpty() {
        // given
        PlaylistSongCreateRequest request = new PlaylistSongCreateRequest(
                "track-123", "Ditto", "NewJeans", "https://image.url", "좋아요", UUID.randomUUID(), Set.of()
        );

        // when & then
        assertThatThrownBy(() -> playlistService.createSong(request, "127.0.0.1"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("장르는 최소 1개에서 최대 3개까지");
    }

    @Test
    @DisplayName("장르가 4개 이상이면 곡 등록 시 예외가 발생한다")
    void createSong_ThrowsException_WhenGenresExceed3() {
        // given
        PlaylistSongCreateRequest request = new PlaylistSongCreateRequest(
                "track-123", "Ditto", "NewJeans", "https://image.url", "좋아요", UUID.randomUUID(),
                Set.of(Genre.KPOP, Genre.ROCK, Genre.INDIE, Genre.BALLAD)
        );

        // when & then
        assertThatThrownBy(() -> playlistService.createSong(request, "127.0.0.1"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("장르는 최소 1개에서 최대 3개까지");
    }

    @Test
    @DisplayName("피드 조회 시 현재 기기의 좋아요(isLiked) 여부가 올바르게 계산된다")
    void getFeedSongs_Success_WithIsLiked() {
        // given
        UUID deviceId = UUID.randomUUID();
        UUID songId = UUID.randomUUID();
        PlaylistTrack track = PlaylistTrack.builder()
                .trackId("track-1")
                .title("Ditto")
                .artist("NewJeans")
                .build();
        PlaylistSong song = PlaylistSong.builder()
                .track(track)
                .deviceId(UUID.randomUUID())
                .ipAddress("127.0.0.1")
                .genres(Set.of(Genre.KPOP))
                .build();
        org.springframework.test.util.ReflectionTestUtils.setField(song, "id", songId);

        Pageable pageable = PageRequest.of(0, 20);
        Page<PlaylistSong> page = new PageImpl<>(List.of(song), pageable, 1);

        given(playlistSongRepository.searchSongs(Genre.KPOP, pageable)).willReturn(page);
        given(playlistSongLikeRepository.findLikedSongIdsByDeviceIdAndSongIdIn(any(UUID.class), any()))
                .willReturn(Set.of(songId));

        // when
        Page<PlaylistSongResponse> result = playlistService.getFeedSongs(Genre.KPOP, pageable, deviceId);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).title()).isEqualTo("Ditto");
    }


    @Test
    @DisplayName("좋아요 등록 토글 성공")
    void toggleLike_Success_AddLike() {
        // given
        UUID songId = UUID.randomUUID();
        UUID deviceId = UUID.randomUUID();
        PlaylistTrack track = PlaylistTrack.builder()
                .trackId("track-1")
                .title("Ditto")
                .artist("NewJeans")
                .build();
        PlaylistSong song = PlaylistSong.builder()
                .track(track)
                .deviceId(UUID.randomUUID())
                .ipAddress("127.0.0.1")
                .genres(Set.of(Genre.KPOP))
                .build();

        given(playlistSongRepository.findByIdAndDeletedAtIsNull(songId)).willReturn(Optional.of(song));
        given(playlistSongLikeRepository.findBySongIdAndDeviceId(songId, deviceId)).willReturn(Optional.empty());
        given(playlistSongRepository.getHeartCount(songId)).willReturn(Optional.of(1));

        // when
        PlaylistLikeToggleResponse response = playlistService.toggleLike(songId, deviceId);

        // then
        assertThat(response.isLiked()).isTrue();
        assertThat(response.heartCount()).isEqualTo(1);
        verify(playlistSongRepository).incrementHeartCount(songId);
        verify(playlistSongLikeRepository).save(any(PlaylistSongLike.class));
    }

    @Test
    @DisplayName("좋아요 취소 토글 성공")
    void toggleLike_Success_RemoveLike() {
        // given
        UUID songId = UUID.randomUUID();
        UUID deviceId = UUID.randomUUID();
        PlaylistTrack track = PlaylistTrack.builder()
                .trackId("track-1")
                .title("Ditto")
                .artist("NewJeans")
                .build();
        PlaylistSong song = PlaylistSong.builder()
                .track(track)
                .deviceId(UUID.randomUUID())
                .ipAddress("127.0.0.1")
                .genres(Set.of(Genre.KPOP))
                .build();

        PlaylistSongLike existingLike = PlaylistSongLike.builder().song(song).deviceId(deviceId).build();

        given(playlistSongRepository.findByIdAndDeletedAtIsNull(songId)).willReturn(Optional.of(song));
        given(playlistSongLikeRepository.findBySongIdAndDeviceId(songId, deviceId)).willReturn(Optional.of(existingLike));
        given(playlistSongRepository.getHeartCount(songId)).willReturn(Optional.of(0));

        // when
        PlaylistLikeToggleResponse response = playlistService.toggleLike(songId, deviceId);

        // then
        assertThat(response.isLiked()).isFalse();
        assertThat(response.heartCount()).isEqualTo(0);
        verify(playlistSongRepository).decrementHeartCount(songId);
        verify(playlistSongLikeRepository).delete(existingLike);
    }

    @Test
    @DisplayName("곡 신고 접수 성공")
    void reportSong_Success() {
        // given
        UUID songId = UUID.randomUUID();
        UUID reporterDeviceId = UUID.randomUUID();
        PlaylistTrack track = PlaylistTrack.builder()
                .trackId("track-1")
                .title("Ditto")
                .artist("NewJeans")
                .build();
        PlaylistSong song = PlaylistSong.builder()
                .track(track)
                .deviceId(UUID.randomUUID())
                .ipAddress("127.0.0.1")
                .genres(Set.of(Genre.KPOP))
                .build();

        PlaylistSongReportRequest request = new PlaylistSongReportRequest(reporterDeviceId, "부적절한 멘트");
        PlaylistSongReport report = PlaylistSongReport.builder()
                .song(song)
                .reporterDeviceId(reporterDeviceId)
                .reason("부적절한 멘트")
                .build();

        given(playlistSongRepository.findByIdAndDeletedAtIsNull(songId)).willReturn(Optional.of(song));
        given(playlistSongReportRepository.save(any(PlaylistSongReport.class))).willReturn(report);

        // when
        PlaylistSongReportResponse response = playlistService.reportSong(songId, request);

        // then
        assertThat(response.songTitle()).isEqualTo("Ditto");
        assertThat(response.reason()).isEqualTo("부적절한 멘트");
        assertThat(response.reporterDeviceId()).isEqualTo(reporterDeviceId);
    }
}
