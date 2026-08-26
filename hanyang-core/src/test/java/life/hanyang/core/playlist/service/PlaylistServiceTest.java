package life.hanyang.core.playlist.service;

import life.hanyang.core.global.exception.BusinessException;
import life.hanyang.core.global.exception.EntityNotFoundException;
import life.hanyang.core.playlist.domain.Genre;
import life.hanyang.core.playlist.domain.PlaylistSong;
import life.hanyang.core.playlist.domain.PlaylistSongLike;
import life.hanyang.core.playlist.domain.PlaylistSongReport;
import life.hanyang.core.playlist.dto.*;
import life.hanyang.core.playlist.repository.PlaylistSongLikeRepository;
import life.hanyang.core.playlist.repository.PlaylistSongReportRepository;
import life.hanyang.core.playlist.repository.PlaylistSongRepository;
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
        UUID userId = UUID.randomUUID();
        PlaylistSongCreateRequest request = new PlaylistSongCreateRequest(
                "track-123", "Ditto", "NewJeans", "https://image.url", "좋아요", userId, Set.of(Genre.KPOP)
        );

        PlaylistSong song = PlaylistSong.builder()
                .trackId(request.trackId())
                .title(request.title())
                .artist(request.artist())
                .albumArtUrl(request.albumArtUrl())
                .comment(request.comment())
                .userId(request.userId())
                .ipAddress("127.0.0.1")
                .genres(request.genres())
                .build();

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
    @DisplayName("피드 조회 시 현재 유저의 좋아요(isLiked) 여부가 올바르게 계산된다")
    void getFeedSongs_Success_WithIsLiked() {
        // given
        UUID userId = UUID.randomUUID();
        UUID songId = UUID.randomUUID();
        PlaylistSong song = PlaylistSong.builder()
                .trackId("track-1")
                .title("Ditto")
                .artist("NewJeans")
                .userId(UUID.randomUUID())
                .ipAddress("127.0.0.1")
                .genres(Set.of(Genre.KPOP))
                .build();
        org.springframework.test.util.ReflectionTestUtils.setField(song, "id", songId);

        Pageable pageable = PageRequest.of(0, 20);
        Page<PlaylistSong> page = new PageImpl<>(List.of(song), pageable, 1);

        given(playlistSongRepository.searchSongs(Genre.KPOP, pageable)).willReturn(page);
        given(playlistSongLikeRepository.findLikedSongIdsByUserIdAndSongIdIn(any(UUID.class), any()))
                .willReturn(Set.of(songId));

        // when
        Page<PlaylistSongResponse> result = playlistService.getFeedSongs(Genre.KPOP, pageable, userId);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).title()).isEqualTo("Ditto");
    }

    @Test
    @DisplayName("등록자 본인은 곡을 삭제(소프트 딜리트)할 수 있다")
    void deleteSong_Success_WhenOwner() {
        // given
        UUID userId = UUID.randomUUID();
        UUID songId = UUID.randomUUID();
        PlaylistSong song = PlaylistSong.builder()
                .trackId("track-1")
                .title("Ditto")
                .artist("NewJeans")
                .userId(userId)
                .ipAddress("127.0.0.1")
                .genres(Set.of(Genre.KPOP))
                .build();

        given(playlistSongRepository.findByIdAndDeletedAtIsNull(songId)).willReturn(Optional.of(song));

        // when
        playlistService.deleteSong(songId, userId);

        // then
        assertThat(song.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("다른 사람의 곡을 삭제하려고 하면 예외가 발생한다")
    void deleteSong_ThrowsException_WhenNotOwner() {
        // given
        UUID ownerId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        UUID songId = UUID.randomUUID();
        PlaylistSong song = PlaylistSong.builder()
                .trackId("track-1")
                .title("Ditto")
                .artist("NewJeans")
                .userId(ownerId)
                .ipAddress("127.0.0.1")
                .genres(Set.of(Genre.KPOP))
                .build();

        given(playlistSongRepository.findByIdAndDeletedAtIsNull(songId)).willReturn(Optional.of(song));

        // when & then
        assertThatThrownBy(() -> playlistService.deleteSong(songId, otherUserId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("본인이 등록한 곡만");
    }

    @Test
    @DisplayName("좋아요 등록 토글 성공")
    void toggleLike_Success_AddLike() {
        // given
        UUID songId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        PlaylistSong song = PlaylistSong.builder()
                .trackId("track-1")
                .title("Ditto")
                .artist("NewJeans")
                .userId(UUID.randomUUID())
                .ipAddress("127.0.0.1")
                .genres(Set.of(Genre.KPOP))
                .build();

        given(playlistSongRepository.findByIdAndDeletedAtIsNull(songId)).willReturn(Optional.of(song));
        given(playlistSongLikeRepository.findBySongIdAndUserId(songId, userId)).willReturn(Optional.empty());
        given(playlistSongRepository.getHeartCount(songId)).willReturn(Optional.of(1));

        // when
        PlaylistLikeToggleResponse response = playlistService.toggleLike(songId, userId);

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
        UUID userId = UUID.randomUUID();
        PlaylistSong song = PlaylistSong.builder()
                .trackId("track-1")
                .title("Ditto")
                .artist("NewJeans")
                .userId(UUID.randomUUID())
                .ipAddress("127.0.0.1")
                .genres(Set.of(Genre.KPOP))
                .build();

        PlaylistSongLike existingLike = PlaylistSongLike.builder().song(song).userId(userId).build();

        given(playlistSongRepository.findByIdAndDeletedAtIsNull(songId)).willReturn(Optional.of(song));
        given(playlistSongLikeRepository.findBySongIdAndUserId(songId, userId)).willReturn(Optional.of(existingLike));
        given(playlistSongRepository.getHeartCount(songId)).willReturn(Optional.of(0));

        // when
        PlaylistLikeToggleResponse response = playlistService.toggleLike(songId, userId);

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
        UUID reporterUserId = UUID.randomUUID();
        PlaylistSong song = PlaylistSong.builder()
                .trackId("track-1")
                .title("Ditto")
                .artist("NewJeans")
                .userId(UUID.randomUUID())
                .ipAddress("127.0.0.1")
                .genres(Set.of(Genre.KPOP))
                .build();

        PlaylistSongReportRequest request = new PlaylistSongReportRequest(reporterUserId, "부적절한 멘트");
        PlaylistSongReport report = PlaylistSongReport.builder()
                .song(song)
                .reporterUserId(reporterUserId)
                .reason("부적절한 멘트")
                .build();

        given(playlistSongRepository.findByIdAndDeletedAtIsNull(songId)).willReturn(Optional.of(song));
        given(playlistSongReportRepository.save(any(PlaylistSongReport.class))).willReturn(report);

        // when
        PlaylistSongReportResponse response = playlistService.reportSong(songId, request);

        // then
        assertThat(response.songTitle()).isEqualTo("Ditto");
        assertThat(response.reason()).isEqualTo("부적절한 멘트");
        assertThat(response.reporterUserId()).isEqualTo(reporterUserId);
    }
}
