package life.hanyang.core.playlist.service;

import life.hanyang.core.playlist.dto.SpotifyTrackSearchResponse;
import life.hanyang.core.playlist.event.PlaylistTrackRegisteredEvent;
import life.hanyang.core.playlist.repository.PlaylistTrackRepository;
import life.hanyang.core.playlist.repository.PlaylistTrackHourlyPlayRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@lombok.extern.slf4j.Slf4j
@Service
@RequiredArgsConstructor
public class PlaylistTrackPlayService {
    private final PlaylistTrackRepository tracks;
    private final PlaylistTrackHourlyPlayRepository plays;
    private final ApplicationEventPublisher publisher;

    @Transactional
    public void record(String trackId, UUID deviceId, SpotifyTrackSearchResponse metadata) {
        if (metadata != null && tracks.insertIfAbsent(trackId, metadata.title(), metadata.artist(),
                metadata.albumArtUrl()) > 0) {
            publisher.publishEvent(new PlaylistTrackRegisteredEvent(trackId));
        }
        Instant now = Instant.now();
        if (plays.insertDailyDeviceIfAbsent(trackId, deviceId,
                now.atZone(ZoneId.of("Asia/Seoul")).toLocalDate()) == 0) return;
        Instant hour = now.truncatedTo(ChronoUnit.HOURS);
        plays.upsertHourlyPlayCount(trackId, hour);
        log.debug("[PlaylistPlay] 음원 재생수 기록 완료 - trackId: {}, playHour: {}", trackId, hour);
    }
}
