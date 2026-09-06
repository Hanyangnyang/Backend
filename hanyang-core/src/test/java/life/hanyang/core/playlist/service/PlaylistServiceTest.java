package life.hanyang.core.playlist.service;

import life.hanyang.core.global.exception.BusinessException;
import life.hanyang.core.global.exception.EntityNotFoundException;
import life.hanyang.core.global.exception.ErrorCode;
import life.hanyang.core.playlist.domain.*;
import life.hanyang.core.playlist.dto.*;
import life.hanyang.core.playlist.exception.SpotifyServiceUnavailableException;
import life.hanyang.core.playlist.repository.PlaylistChartRepository;
import life.hanyang.core.playlist.repository.PlaylistTrackLikeRepository;
import life.hanyang.core.playlist.repository.PlaylistSongReactionRepository;
import life.hanyang.core.playlist.repository.PlaylistSongReportRepository;
import life.hanyang.core.playlist.repository.PlaylistSongRepository;
import life.hanyang.core.playlist.repository.PlaylistTrackHourlyPlayRepository;
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

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PlaylistServiceTest {

    @Mock
    private PlaylistTrackRepository playlistTrackRepository;

    @Mock
    private PlaylistSongRepository playlistSongRepository;

    @Mock
    private PlaylistTrackLikeRepository playlistTrackLikeRepository;

    @Mock
    private PlaylistSongReactionRepository playlistSongReactionRepository;

    @Mock
    private PlaylistSongReportRepository playlistSongReportRepository;

    @Mock
    private PlaylistModerationService playlistModerationService;

    @Mock
    private PlaylistTrackHourlyPlayRepository playlistTrackHourlyPlayRepository;

    @Mock
    private PlaylistChartRepository playlistChartRepository;

    @Mock
    private SpotifyTrackSearchService spotifyTrackSearchService;

    @InjectMocks
    private PlaylistService playlistService;

    @Test
    @DisplayName("곡 등록 성공")
    void createSong_Success() {
        // given
        UUID deviceId = UUID.randomUUID();
        PlaylistSongCreateRequest request = new PlaylistSongCreateRequest(
                "track-123", "Ditto", "NewJeans", "https://i.scdn.co/image/ab67616d0000b273bd15713cf9824b7842bcd290", "좋아요", deviceId, Set.of(Genre.KPOP)
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
                .isAiModerated(true)
                .build();

        given(playlistModerationService.validateSongContent(any(), any(), any())).willReturn(true);
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
    }

    @Test
    @DisplayName("유해 코멘트 감지 시 곡 등록이 차단된다")
    void createSong_ThrowsException_WhenModerationFails() {
        // given
        UUID deviceId = UUID.randomUUID();
        PlaylistSongCreateRequest request = new PlaylistSongCreateRequest(
                "track-123", "곡명", "가수", "https://i.scdn.co/image/ab67616d0000b273bd15713cf9824b7842bcd290", "부적절한 욕설 코멘트", deviceId, Set.of(Genre.KPOP)
        );

        given(playlistModerationService.validateSongContent(any(), any(), any()))
                .willThrow(new BusinessException("부적절한 표현이 감지되었습니다.", ErrorCode.PLAYLIST_INAPPROPRIATE_COMMENT));

        // when & then
        assertThatThrownBy(() -> playlistService.createSong(request, "127.0.0.1"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("부적절한 표현이 감지되었습니다.");
    }

    @Test
    @DisplayName("하루에 3곡 이상 등록 시 예외가 발생한다")
    void createSong_ThrowsException_WhenDailyLimitExceeded() {
        // given
        UUID deviceId = UUID.randomUUID();
        PlaylistSongCreateRequest request = new PlaylistSongCreateRequest(
                "track-123", "Ditto", "NewJeans", "https://i.scdn.co/image/ab67616d0000b273bd15713cf9824b7842bcd290", "좋아요", deviceId, Set.of(Genre.KPOP)
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
                "track-123", "Ditto", "NewJeans", "https://i.scdn.co/image/ab67616d0000b273bd15713cf9824b7842bcd290", "좋아요", deviceId, Set.of(Genre.KPOP)
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
                "track-123", "Ditto", "NewJeans", "https://i.scdn.co/image/ab67616d0000b273bd15713cf9824b7842bcd290", "좋아요", UUID.randomUUID(), Set.of()
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
                "track-123", "Ditto", "NewJeans", "https://i.scdn.co/image/ab67616d0000b273bd15713cf9824b7842bcd290", "좋아요", UUID.randomUUID(),
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

        // when
        Page<PlaylistSongResponse> result = playlistService.getFeedSongs(Genre.KPOP, pageable, deviceId);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).title()).isEqualTo("Ditto");
    }

    @Test
    @DisplayName("내가 작성한 추천글 목록 조회 성공 (isLiked 배치 포함)")
    void getMySongs_Success() {
        // given
        UUID deviceId = UUID.randomUUID();
        UUID songId = UUID.randomUUID();
        PlaylistTrack track = PlaylistTrack.builder()
                .trackId("track-123")
                .title("Ditto")
                .artist("NewJeans")
                .albumArtUrl("https://i.scdn.co/image/ab67616d0000b273bd15713cf9824b7842bcd290")
                .build();
        PlaylistSong song = PlaylistSong.builder()
                .track(track)
                .deviceId(deviceId)
                .ipAddress("127.0.0.1")
                .comment("내가 쓴 추천글")
                .genres(Set.of(Genre.KPOP))
                .build();
        org.springframework.test.util.ReflectionTestUtils.setField(song, "id", songId);

        Pageable pageable = PageRequest.of(0, 20);
        Page<PlaylistSong> page = new PageImpl<>(List.of(song), pageable, 1);

        given(playlistSongRepository.searchMySongs(deviceId, pageable)).willReturn(page);

        // when
        Page<PlaylistSongResponse> result = playlistService.getMySongs(deviceId, pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).title()).isEqualTo("Ditto");
        assertThat(result.getContent().get(0).comment()).isEqualTo("내가 쓴 추천글");
    }

    @Test
    @DisplayName("특정 곡의 상세 정보 및 추천글 목록 조회 성공")
    void getTrackDetailAndSongs_Success() {
        // given
        String trackId = "track-123";
        UUID deviceId = UUID.randomUUID();
        UUID songId = UUID.randomUUID();

        PlaylistTrack track = PlaylistTrack.builder()
                .trackId(trackId)
                .title("LOVE SONG")
                .artist("유다빈밴드")
                .albumArtUrl("https://i.scdn.co/image/ab67616d0000b273bd15713cf9824b7842bcd290")
                .build();

        PlaylistSong song = PlaylistSong.builder()
                .track(track)
                .comment("과제할 때 들으면 극락")
                .deviceId(UUID.randomUUID())
                .ipAddress("127.0.0.1")
                .genres(Set.of(Genre.ROCK))
                .build();
        org.springframework.test.util.ReflectionTestUtils.setField(song, "id", songId);

        Pageable pageable = PageRequest.of(0, 20);
        Page<PlaylistSong> page = new PageImpl<>(List.of(song), pageable, 1);

        given(playlistTrackRepository.findById(trackId)).willReturn(Optional.of(track));
        given(playlistSongRepository.searchSongsByTrackId(trackId, pageable)).willReturn(page);
        given(playlistTrackLikeRepository.existsByTrackTrackIdAndDeviceId(trackId, deviceId)).willReturn(true);

        // when
        PlaylistTrackDetailResponse response = playlistService.getTrackDetailAndSongs(trackId, pageable, deviceId);

        // then
        assertThat(response.trackId()).isEqualTo(trackId);
        assertThat(response.title()).isEqualTo("LOVE SONG");
        assertThat(response.artist()).isEqualTo("유다빈밴드");
        assertThat(response.totalSongsCount()).isEqualTo(1L);
        assertThat(response.isLiked()).isTrue();
        assertThat(response.songs().getContent()).hasSize(1);
    }

    @Test
    @DisplayName("추천글 가중치 검색 성공")
    void searchSongsWithWeight_Success() {
        // given
        String keyword = "유다빈";
        UUID deviceId = UUID.randomUUID();
        UUID songId = UUID.randomUUID();

        PlaylistTrack track = PlaylistTrack.builder()
                .trackId("track-1")
                .title("LOVE SONG")
                .artist("유다빈밴드")
                .build();
        PlaylistSong song = PlaylistSong.builder()
                .track(track)
                .comment("추천합니다")
                .deviceId(UUID.randomUUID())
                .ipAddress("127.0.0.1")
                .genres(Set.of(Genre.ROCK))
                .build();
        org.springframework.test.util.ReflectionTestUtils.setField(song, "id", songId);

        Pageable pageable = PageRequest.of(0, 20);
        Page<PlaylistSong> page = new PageImpl<>(List.of(song), pageable, 1);

        given(playlistSongRepository.searchSongsWithWeight(keyword, SpotifySearchExpansion.empty(), pageable)).willReturn(page);

        // when
        Page<PlaylistSongResponse> result = playlistService.searchSongsWithWeight(keyword, pageable, deviceId);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).artist()).isEqualTo("유다빈밴드");
    }

    @Test
    @DisplayName("추천글 단건 상세 조회 성공")
    void getSong_Success() {
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
        org.springframework.test.util.ReflectionTestUtils.setField(song, "id", songId);

        given(playlistSongRepository.findByIdAndDeletedAtIsNull(songId)).willReturn(Optional.of(song));

        // when
        PlaylistSongResponse response = playlistService.getSong(songId, deviceId);

        // then
        assertThat(response.id()).isEqualTo(songId);
        assertThat(response.title()).isEqualTo("Ditto");
    }

    @Test
    @DisplayName("존재하지 않는 추천글 단건 조회 시 예외 발생")
    void getSong_ThrowsException_WhenNotFound() {
        // given
        UUID songId = UUID.randomUUID();
        given(playlistSongRepository.findByIdAndDeletedAtIsNull(songId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> playlistService.getSong(songId, UUID.randomUUID()))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("존재하지 않거나 삭제된 추천글입니다.");
    }


    @Test
    @DisplayName("곡 좋아요 등록 토글 성공")
    void toggleTrackLike_Success_AddLike() {
        // given
        String trackId = "track-1";
        UUID deviceId = UUID.randomUUID();
        PlaylistTrack track = PlaylistTrack.builder()
                .trackId("track-1")
                .title("Ditto")
                .artist("NewJeans")
                .build();
        given(playlistTrackRepository.findById(trackId)).willReturn(Optional.of(track));
        given(playlistTrackLikeRepository.findByTrackTrackIdAndDeviceId(trackId, deviceId)).willReturn(Optional.empty());
        given(playlistTrackRepository.getLikeCount(trackId)).willReturn(Optional.of(1));

        // when
        PlaylistLikeToggleResponse response = playlistService.toggleTrackLike(trackId, deviceId);

        // then
        assertThat(response.isLiked()).isTrue();
        assertThat(response.likeCount()).isEqualTo(1);
        verify(playlistTrackRepository).incrementLikeCount(trackId);
        verify(playlistTrackLikeRepository).save(any(PlaylistTrackLike.class));
    }

    @Test
    @DisplayName("곡 좋아요 취소 토글 성공")
    void toggleTrackLike_Success_RemoveLike() {
        // given
        String trackId = "track-1";
        UUID deviceId = UUID.randomUUID();
        PlaylistTrack track = PlaylistTrack.builder()
                .trackId("track-1")
                .title("Ditto")
                .artist("NewJeans")
                .build();
        PlaylistTrackLike existingLike = PlaylistTrackLike.builder().track(track).deviceId(deviceId).build();

        given(playlistTrackRepository.findById(trackId)).willReturn(Optional.of(track));
        given(playlistTrackLikeRepository.findByTrackTrackIdAndDeviceId(trackId, deviceId)).willReturn(Optional.of(existingLike));
        given(playlistTrackRepository.getLikeCount(trackId)).willReturn(Optional.of(0));

        // when
        PlaylistLikeToggleResponse response = playlistService.toggleTrackLike(trackId, deviceId);

        // then
        assertThat(response.isLiked()).isFalse();
        assertThat(response.likeCount()).isEqualTo(0);
        verify(playlistTrackRepository).decrementLikeCount(trackId);
        verify(playlistTrackLikeRepository).delete(existingLike);
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

    @Test
    @DisplayName("음원 재생수 기록 성공")
    void recordTrackPlay_Success() {
        // given
        String trackId = "track-123";
        given(playlistTrackRepository.existsById(trackId)).willReturn(true);

        // when
        playlistService.recordTrackPlay(trackId);

        // then
        verify(playlistTrackHourlyPlayRepository).upsertHourlyPlayCount(org.mockito.ArgumentMatchers.eq(trackId), any());
    }

    @Test
    @DisplayName("존재하지 않는 음원 트랙 재생수 기록 시 예외 발생")
    void recordTrackPlay_ThrowsException_WhenTrackNotFound() {
        // given
        String trackId = "invalid-track";
        given(playlistTrackRepository.existsById(trackId)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> playlistService.recordTrackPlay(trackId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("존재하지 않는 음원 트랙입니다.");
    }

    @Test
    @DisplayName("실시간 급상승 차트 조회 성공 (DB 스냅샷 존재 시)")
    void getChart_Rising_FromSnapshot_Success() {
        // given
        PlaylistTrack track = PlaylistTrack.builder()
                .trackId("track-1")
                .title("LOVE SONG")
                .artist("유다빈밴드")
                .albumArtUrl("https://i.scdn.co/image/ab67616d0000b273951f05b855b09c8b4d7d2ee5")
                .build();

        PlaylistChart chartEntity = PlaylistChart.builder()
                .chartType(ChartType.RISING)
                .snapshotTime(java.time.Instant.now())
                .startPeriod(java.time.Instant.now().minus(24, java.time.temporal.ChronoUnit.HOURS))
                .endPeriod(java.time.Instant.now())
                .rank(1)
                .track(track)
                .totalScore(500L)
                .build();

        given(playlistChartRepository.findLatestChartByChartTypeAndGenre(ChartType.RISING, null))
                .willReturn(List.of(chartEntity));

        // when
        PlaylistChartResponse response = playlistService.getChart(ChartType.RISING);

        // then
        assertThat(response.chartType()).isEqualTo(ChartType.RISING);
        assertThat(response.tracks()).hasSize(1);
        assertThat(response.tracks().get(0).rank()).isEqualTo(1);
        assertThat(response.tracks().get(0).title()).isEqualTo("LOVE SONG");
    }

    @Test
    @DisplayName("주간 차트 계산 및 저장 성공")
    void calculateAndSaveChart_Weekly_Success() {
        // given
        Object[] row = new Object[]{"track-1", "LOVE SONG", "유다빈밴드", "https://image1.url", 1000L};
        List<Object[]> rows = Collections.singletonList(row);
        given(playlistTrackHourlyPlayRepository.findWeeklyChartRaw(any(), any(), any(), anyInt()))
                .willReturn(rows);
        PlaylistTrack track = PlaylistTrack.builder().trackId("track-1").title("LOVE SONG").artist("유다빈밴드").build();
        given(playlistTrackRepository.getReferenceById("track-1")).willReturn(track);

        // when
        PlaylistChartResponse response = playlistService.calculateAndSaveChart(ChartType.WEEKLY, java.time.Instant.now());

        // then
        assertThat(response.chartType()).isEqualTo(ChartType.WEEKLY);
        assertThat(response.tracks()).hasSize(1);
        assertThat(response.tracks().get(0).rank()).isEqualTo(1);
        verify(playlistChartRepository).saveAll(any());
        verify(playlistTrackHourlyPlayRepository).findWeeklyChartRaw(any(), any(), eq(Genre.KPOP.name()), anyInt());
    }

    @Test
    @DisplayName("월간 차트 계산 및 저장 성공")
    void calculateAndSaveChart_Monthly_Success() {
        // given
        Object[] row = new Object[]{"track-1", "Hype Boy", "NewJeans", "https://i.scdn.co/image/ab67616d0000b273bd15713cf9824b7842bcd290", 5000L};
        List<Object[]> rows = Collections.singletonList(row);
        given(playlistTrackHourlyPlayRepository.findMonthlyChartRaw(any(), any(), any(), anyInt()))
                .willReturn(rows);
        PlaylistTrack track = PlaylistTrack.builder().trackId("track-1").title("Hype Boy").artist("NewJeans").build();
        given(playlistTrackRepository.getReferenceById("track-1")).willReturn(track);

        // when
        PlaylistChartResponse response = playlistService.calculateAndSaveChart(ChartType.MONTHLY, java.time.Instant.now());

        // then
        assertThat(response.chartType()).isEqualTo(ChartType.MONTHLY);
        assertThat(response.tracks()).hasSize(1);
        assertThat(response.tracks().get(0).title()).isEqualTo("Hype Boy");
        verify(playlistChartRepository).saveAll(any());
    }

    @Test
    @DisplayName("이모지 리액션 추가 성공 (최초 등록)")
    void toggleReaction_Add_Success() {
        // given
        UUID songId = UUID.randomUUID();
        UUID deviceId = UUID.randomUUID();
        PlaylistSong song = PlaylistSong.builder().build();

        given(playlistSongRepository.findByIdAndDeletedAtIsNull(songId)).willReturn(Optional.of(song));
        given(playlistSongReactionRepository.findBySongIdAndDeviceIdAndReactionType(songId, deviceId, ReactionType.FIRE))
                .willReturn(Optional.empty());
        List<Object[]> countRows = Collections.singletonList(new Object[]{ReactionType.FIRE, 1L});
        given(playlistSongReactionRepository.countReactionsBySongId(songId))
                .willReturn(countRows);
        given(playlistSongReactionRepository.findUserReactionTypesByDeviceIdAndSongId(deviceId, songId))
                .willReturn(Set.of(ReactionType.FIRE));

        PlaylistReactionToggleRequest request = new PlaylistReactionToggleRequest(deviceId, ReactionType.FIRE);

        // when
        PlaylistReactionToggleResponse response = playlistService.toggleReaction(songId, request);

        // then
        assertThat(response.songId()).isEqualTo(songId);
        assertThat(response.reactionType()).isEqualTo(ReactionType.FIRE);
        assertThat(response.isReacted()).isTrue();
        assertThat(response.reactions()).hasSize(9);
        PlaylistReactionItemResponse fireItem = response.reactions().stream()
                .filter(r -> r.type() == ReactionType.FIRE)
                .findFirst().orElseThrow();
        assertThat(fireItem.count()).isEqualTo(1L);
        assertThat(fireItem.isReacted()).isTrue();
        verify(playlistSongReactionRepository).save(any());
    }

    @Test
    @DisplayName("이모지 리액션 취소 성공 (이미 등록된 경우)")
    void toggleReaction_Cancel_Success() {
        // given
        UUID songId = UUID.randomUUID();
        UUID deviceId = UUID.randomUUID();
        PlaylistSong song = PlaylistSong.builder().build();
        PlaylistSongReaction existing = PlaylistSongReaction.builder().song(song).deviceId(deviceId).reactionType(ReactionType.FIRE).build();

        given(playlistSongRepository.findByIdAndDeletedAtIsNull(songId)).willReturn(Optional.of(song));
        given(playlistSongReactionRepository.findBySongIdAndDeviceIdAndReactionType(songId, deviceId, ReactionType.FIRE))
                .willReturn(Optional.of(existing));
        given(playlistSongReactionRepository.countReactionsBySongId(songId))
                .willReturn(Collections.emptyList());
        given(playlistSongReactionRepository.findUserReactionTypesByDeviceIdAndSongId(deviceId, songId))
                .willReturn(Collections.emptySet());

        PlaylistReactionToggleRequest request = new PlaylistReactionToggleRequest(deviceId, ReactionType.FIRE);

        // when
        PlaylistReactionToggleResponse response = playlistService.toggleReaction(songId, request);

        // then
        assertThat(response.isReacted()).isFalse();
        verify(playlistSongReactionRepository).deleteBySongIdAndDeviceIdAndReactionType(songId, deviceId, ReactionType.FIRE);
    }
}
