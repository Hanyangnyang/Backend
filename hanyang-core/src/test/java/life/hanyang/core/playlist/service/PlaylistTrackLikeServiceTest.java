package life.hanyang.core.playlist.service;

import life.hanyang.core.playlist.dto.PlaylistLikeToggleResponse;
import life.hanyang.core.playlist.dto.SpotifyTrackSearchResponse;
import life.hanyang.core.playlist.repository.PlaylistTrackLikeRepository;
import life.hanyang.core.playlist.repository.PlaylistTrackRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PlaylistTrackLikeServiceTest {
    @Mock private PlaylistTrackRepository playlistTrackRepository;
    @Mock private PlaylistTrackLikeRepository playlistTrackLikeRepository;
    @InjectMocks private PlaylistTrackLikeService playlistTrackLikeService;

    @Test
    @DisplayName("곡 좋아요 등록 토글 성공")
    void toggleTrackLike_Success_AddLike() {
        // given
        String trackId = "track-1";
        UUID deviceId = UUID.randomUUID();
        given(playlistTrackLikeRepository.deleteIfPresent(trackId, deviceId)).willReturn(0);
        given(playlistTrackLikeRepository.insertIfAbsent(trackId, deviceId)).willReturn(1);
        given(playlistTrackRepository.getLikeCount(trackId)).willReturn(Optional.of(1));

        // when
        PlaylistLikeToggleResponse response = playlistTrackLikeService.toggle(trackId, deviceId, null);

        // then
        assertThat(response.isLiked()).isTrue();
        assertThat(response.likeCount()).isEqualTo(1);
        verify(playlistTrackRepository).incrementLikeCount(trackId);
        verify(playlistTrackLikeRepository).insertIfAbsent(trackId, deviceId);
    }

    @Test
    @DisplayName("미등록 트랙은 Spotify 조회 후 저장하고 좋아요를 등록한다")
    void toggleTrackLike_RegistersMissingTrack() {
        String trackId = "track-1";
        UUID deviceId = UUID.randomUUID();
        given(playlistTrackLikeRepository.insertIfAbsent(trackId, deviceId)).willReturn(1);
        given(playlistTrackRepository.getLikeCount(trackId)).willReturn(Optional.of(1));

        PlaylistLikeToggleResponse response = playlistTrackLikeService.toggle(trackId, deviceId,
                new SpotifyTrackSearchResponse(trackId, "Ditto", "NewJeans", "cover", 1));

        assertThat(response.isLiked()).isTrue();
        assertThat(response.likeCount()).isEqualTo(1);
        var order = org.mockito.Mockito.inOrder(playlistTrackRepository, playlistTrackLikeRepository);
        order.verify(playlistTrackRepository).insertIfAbsent(trackId, "Ditto", "NewJeans", "cover");
        order.verify(playlistTrackLikeRepository).deleteIfPresent(trackId, deviceId);
        order.verify(playlistTrackLikeRepository).insertIfAbsent(trackId, deviceId);
        order.verify(playlistTrackRepository).incrementLikeCount(trackId);
    }

    @Test
    @DisplayName("곡 좋아요 취소 토글 성공")
    void toggleTrackLike_Success_RemoveLike() {
        // given
        String trackId = "track-1";
        UUID deviceId = UUID.randomUUID();
        given(playlistTrackLikeRepository.deleteIfPresent(trackId, deviceId)).willReturn(1);
        given(playlistTrackRepository.getLikeCount(trackId)).willReturn(Optional.of(0));

        // when
        PlaylistLikeToggleResponse response = playlistTrackLikeService.toggle(trackId, deviceId, null);

        // then
        assertThat(response.isLiked()).isFalse();
        assertThat(response.likeCount()).isEqualTo(0);
        verify(playlistTrackRepository).decrementLikeCount(trackId);
        verify(playlistTrackLikeRepository).deleteIfPresent(trackId, deviceId);
    }

}
