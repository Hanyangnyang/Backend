package life.hanyang.admin.playlist.scheduler;

import life.hanyang.core.playlist.domain.ChartType;
import life.hanyang.core.playlist.service.PlaylistService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PlaylistChartSchedulerTest {

    @Test
    @DisplayName("한 차트 Warm-up이 실패해도 나머지 차트를 계속 처리한다")
    void warmupChartsOnStartup_ContinuesAfterFailure() {
        PlaylistService playlistService = mock(PlaylistService.class);
        PlaylistChartScheduler scheduler = new PlaylistChartScheduler(playlistService);
        doThrow(new RuntimeException("rising warm-up failed"))
                .when(playlistService).getChart(ChartType.RISING);

        scheduler.warmupChartsOnStartup();

        verify(playlistService).getChart(ChartType.RISING);
        verify(playlistService).getChart(ChartType.WEEKLY);
        verify(playlistService).getChart(ChartType.MONTHLY);
    }
}
