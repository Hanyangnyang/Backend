package life.hanyang.core.playlist.service;

import life.hanyang.core.playlist.domain.ChartType;
import life.hanyang.core.playlist.dto.PlaylistChartItemResponse;
import life.hanyang.core.playlist.dto.PlaylistChartResponse;
import life.hanyang.core.playlist.dto.PlaylistTrackLikeResponse;
import life.hanyang.core.playlist.domain.PlaylistTrack;
import life.hanyang.core.playlist.repository.PlaylistTrackLikeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class PlaylistChartQueryServiceTest {
    @Mock private PlaylistService playlistService;
    @Mock private PlaylistTrackLikeRepository playlistTrackLikeRepository;
    @InjectMocks private PlaylistChartQueryService service;

    private PlaylistChartResponse chart() {
        Instant now = Instant.now();
        return PlaylistChartResponse.of(ChartType.RISING, null, now, now, now, "차트", List.of(
                new PlaylistChartItemResponse(1, "track-1", "곡", "가수", null),
                new PlaylistChartItemResponse(2, "track-2", "곡2", "가수", null)));
    }

    @Test
    void decoratesFreshLikesWithoutChangingSharedChart() {
        PlaylistChartResponse cached = chart();
        UUID firstDevice = UUID.randomUUID();
        UUID secondDevice = UUID.randomUUID();
        given(playlistService.getChart(ChartType.RISING, null)).willReturn(cached);
        given(playlistTrackLikeRepository.findLikedTrackIds(firstDevice, List.of("track-1", "track-2")))
                .willReturn(Set.of("track-1"), Set.of());
        given(playlistTrackLikeRepository.findLikedTrackIds(secondDevice, List.of("track-1", "track-2")))
                .willReturn(Set.of("track-2"));

        assertThat(service.getChart(ChartType.RISING, null, firstDevice).tracks())
                .extracting(PlaylistChartItemResponse::isLiked).containsExactly(true, false);
        assertThat(service.getChart(ChartType.RISING, null, secondDevice).tracks())
                .extracting(PlaylistChartItemResponse::isLiked).containsExactly(false, true);
        assertThat(service.getChart(ChartType.RISING, null, firstDevice).tracks())
                .extracting(PlaylistChartItemResponse::isLiked).containsExactly(false, false);
        assertThat(cached.tracks()).allMatch(track -> !track.isLiked());
    }

    @Test
    void missingDeviceSkipsLikeQuery() {
        given(playlistService.getChart(ChartType.RISING, null)).willReturn(chart());
        assertThat(service.getChart(ChartType.RISING, null, null).tracks()).allMatch(track -> !track.isLiked());
        verifyNoInteractions(playlistTrackLikeRepository);
    }

    @Test
    void likedTrackResponseAlwaysReturnsTrue() {
        PlaylistTrack track = PlaylistTrack.builder().trackId("track-1").title("곡").artist("가수").build();
        assertThat(PlaylistTrackLikeResponse.of(track).isLiked()).isTrue();
    }
}
