package life.hanyang.core.playlist.service;

import life.hanyang.core.playlist.domain.ChartType;
import life.hanyang.core.playlist.domain.Genre;
import life.hanyang.core.playlist.dto.PlaylistChartItemResponse;
import life.hanyang.core.playlist.dto.PlaylistChartResponse;
import life.hanyang.core.playlist.repository.PlaylistTrackLikeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PlaylistChartQueryService {
    private final PlaylistService playlistService;
    private final PlaylistTrackLikeRepository playlistTrackLikeRepository;

    public PlaylistChartResponse getChart(ChartType type, Genre genre, UUID deviceId) {
        // 공용 차트 캐시를 조회한 뒤 새 응답에 기기별 상태를 붙인다.
        PlaylistChartResponse chart = playlistService.getChart(type, genre);
        var trackIds = chart.tracks().stream().map(PlaylistChartItemResponse::trackId).distinct().toList();
        Set<String> likedTrackIds = deviceId == null || trackIds.isEmpty() ? Set.of()
                : playlistTrackLikeRepository.findLikedTrackIds(deviceId, trackIds);
        var tracks = chart.tracks().stream().map(track -> new PlaylistChartItemResponse(
                track.rank(), track.trackId(), track.title(), track.artist(), track.albumArtUrl(),
                likedTrackIds.contains(track.trackId()), track.artists()
        )).toList();
        return PlaylistChartResponse.of(chart.chartType(), chart.genre(), chart.snapshotTime(),
                chart.startPeriod(), chart.endPeriod(), chart.displayTitle(), tracks);
    }
}
