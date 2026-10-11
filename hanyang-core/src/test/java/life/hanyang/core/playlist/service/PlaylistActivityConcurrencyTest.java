package life.hanyang.core.playlist.service;

import life.hanyang.core.playlist.dto.SpotifyTrackSearchResponse;
import life.hanyang.core.playlist.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Real isolated H2 transactions exercise locks and counters. PostgreSQL UPSERT syntax is adapted here. */
class PlaylistActivityConcurrencyTest {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transaction;
    private final PlaylistTrackRepository tracks = mock(PlaylistTrackRepository.class);
    private final PlaylistTrackLikeRepository likes = mock(PlaylistTrackLikeRepository.class);
    private final PlaylistTrackHourlyPlayRepository plays = mock(PlaylistTrackHourlyPlayRepository.class);
    private final ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);

    PlaylistActivityConcurrencyTest() {
        var ds = new DriverManagerDataSource("jdbc:h2:mem:activity-" + UUID.randomUUID()
                + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000", "sa", "");
        jdbc = new JdbcTemplate(ds);
        transaction = new TransactionTemplate(new DataSourceTransactionManager(ds));
        jdbc.execute("CREATE TABLE tracks(id varchar PRIMARY KEY, like_count int NOT NULL DEFAULT 0)");
        jdbc.execute("CREATE TABLE likes(track_id varchar REFERENCES tracks(id), device_id uuid, PRIMARY KEY(track_id, device_id))");
        jdbc.execute("CREATE TABLE daily(track_id varchar REFERENCES tracks(id), device_id uuid, play_date date, PRIMARY KEY(track_id, device_id, play_date))");
        jdbc.execute("CREATE TABLE hourly(track_id varchar REFERENCES tracks(id), play_count int NOT NULL)");
        when(tracks.insertIfAbsent(anyString(), anyString(), anyString(), any())).thenAnswer(call -> {
            try { return jdbc.update("INSERT INTO tracks(id) VALUES (?)", call.getArgument(0, String.class)); }
            catch (DuplicateKeyException duplicate) { return 0; }
        });
        when(tracks.findByIdForUpdate(anyString())).thenAnswer(call -> {
            jdbc.queryForObject("SELECT id FROM tracks WHERE id = ? FOR UPDATE", String.class, call.getArgument(0, String.class));
            return Optional.empty();
        });
        when(likes.deleteIfPresent(anyString(), any())).thenAnswer(call ->
                jdbc.update("DELETE FROM likes WHERE track_id = ? AND device_id = ?", call.getArgument(0, String.class), call.getArgument(1)));
        when(likes.insertIfAbsent(anyString(), any())).thenAnswer(call -> {
            try { return jdbc.update("INSERT INTO likes VALUES (?, ?)", call.getArgument(0, String.class), call.getArgument(1)); }
            catch (DuplicateKeyException duplicate) { return 0; }
        });
        doAnswer(call -> jdbc.update("UPDATE tracks SET like_count = like_count + 1 WHERE id = ?", call.getArgument(0, String.class)))
                .when(tracks).incrementLikeCount(anyString());
        doAnswer(call -> jdbc.update("UPDATE tracks SET like_count = like_count - 1 WHERE id = ?", call.getArgument(0, String.class)))
                .when(tracks).decrementLikeCount(anyString());
        when(tracks.getLikeCount(anyString())).thenAnswer(call ->
                Optional.of(jdbc.queryForObject("SELECT like_count FROM tracks WHERE id = ?", Integer.class, call.getArgument(0, String.class))));
        when(plays.insertDailyDeviceIfAbsent(anyString(), any(), any())).thenAnswer(call -> {
            try { return jdbc.update("INSERT INTO daily VALUES (?, ?, ?)", call.getArgument(0, String.class), call.getArgument(1), call.getArgument(2)); }
            catch (DuplicateKeyException duplicate) { return 0; }
        });
        doAnswer(call -> jdbc.update("UPDATE hourly SET play_count = play_count + 1 WHERE track_id = ?", call.getArgument(0, String.class)))
                .when(plays).upsertHourlyPlayCount(anyString(), any());
    }

    @Test
    void overlappingFirstLikesAndEvenNumberOfSameDeviceTogglesRemainConsistent() throws Exception {
        UUID device = UUID.randomUUID();
        var metadata = new SpotifyTrackSearchResponse("new", "title", "artist", null, 1, List.of());
        var service = new PlaylistTrackLikeService(tracks, likes, publisher);
        parallel(24, i -> service.toggle("new", device, metadata));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM tracks", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM likes", Integer.class)).isZero();
        assertThat(tracks.getLikeCount("new")).contains(0);
        verify(publisher, times(1)).publishEvent(any(life.hanyang.core.playlist.event.PlaylistTrackRegisteredEvent.class));
        parallel(24, i -> service.toggle("new", new UUID(0, i + 1), null));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM likes", Integer.class)).isEqualTo(24);
        assertThat(tracks.getLikeCount("new")).contains(24);
    }

    @Test
    void repeatedPlaysCountOnceAndOtherDevicesIncrementAtomically() throws Exception {
        jdbc.update("INSERT INTO tracks(id) VALUES ('track')");
        jdbc.update("INSERT INTO hourly VALUES ('track', 0)");
        UUID device = UUID.randomUUID();
        var service = new PlaylistTrackPlayService(tracks, plays, publisher);
        parallel(24, i -> service.record("track", device, null));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM daily", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT play_count FROM hourly", Integer.class)).isEqualTo(1);
        parallel(24, i -> service.record("track", new UUID(0, i + 1), null));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM daily", Integer.class)).isEqualTo(25);
        assertThat(jdbc.queryForObject("SELECT play_count FROM hourly", Integer.class)).isEqualTo(25);
    }

    @Test
    void playCounterFailureRollsBackDailyDeduplication() {
        jdbc.update("INSERT INTO tracks(id) VALUES ('track')");
        doThrow(new IllegalStateException("counter failure")).when(plays).upsertHourlyPlayCount(anyString(), any());
        var service = new PlaylistTrackPlayService(tracks, plays, publisher);
        assertThatThrownBy(() -> transaction.executeWithoutResult(status ->
                service.record("track", UUID.randomUUID(), null))).isInstanceOf(IllegalStateException.class);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM daily", Integer.class)).isZero();
    }

    private void parallel(int size, java.util.function.IntConsumer action) throws Exception {
        var executor = Executors.newFixedThreadPool(8);
        var start = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();
        try {
            for (int i = 0; i < size; i++) {
                int index = i;
                futures.add(executor.submit(() -> {
                    try { start.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IllegalStateException(e); }
                    transaction.executeWithoutResult(status -> action.accept(index));
                }));
            }
            start.countDown();
            for (var future : futures) future.get(20, TimeUnit.SECONDS);
        } finally { executor.shutdownNow(); }
    }
}
