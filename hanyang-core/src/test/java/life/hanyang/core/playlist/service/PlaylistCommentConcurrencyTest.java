package life.hanyang.core.playlist.service;

import life.hanyang.core.playlist.domain.*;
import life.hanyang.core.playlist.dto.PlaylistCommentRequest;
import life.hanyang.core.playlist.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/** H2 행 잠금을 사용해 서로 다른 기기의 동시 댓글 번호 배정을 검증한다. */
class PlaylistCommentConcurrencyTest {
    @Test void concurrentVisitorsReceiveDistinctStableNumbers() throws Exception {
        var ds = new DriverManagerDataSource("jdbc:h2:mem:comments-" + UUID.randomUUID()
                + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000", "sa", "");
        var jdbc = new JdbcTemplate(ds);
        var tx = new TransactionTemplate(new DataSourceTransactionManager(ds));
        jdbc.execute("CREATE TABLE songs(id uuid PRIMARY KEY)");
        jdbc.execute("CREATE TABLE comments(id uuid PRIMARY KEY, song_id uuid, device_id uuid, anonymous_number int)");
        UUID songId = UUID.randomUUID();
        jdbc.update("INSERT INTO songs VALUES (?)", songId);
        var songs = mock(PlaylistSongRepository.class);
        var comments = mock(PlaylistCommentRepository.class);
        var song = PlaylistSong.builder().deviceId(UUID.randomUUID()).build();
        when(songs.findActiveByIdForUpdate(songId)).thenAnswer(call -> {
            jdbc.queryForObject("SELECT id FROM songs WHERE id = ? FOR UPDATE", UUID.class, songId);
            return Optional.of(song);
        });
        when(comments.maxAnonymousNumber(songId)).thenAnswer(call ->
                jdbc.queryForObject("SELECT COALESCE(MAX(anonymous_number), 0) FROM comments WHERE song_id = ?", Integer.class, songId));
        when(comments.saveAndFlush(any())).thenAnswer(call -> {
            PlaylistComment c = call.getArgument(0);
            jdbc.update("INSERT INTO comments VALUES (?, ?, ?, ?)", c.getId(), c.getSongId(), c.getDeviceId(), c.getAnonymousNumber());
            return c;
        });
        // PostgreSQL 기기 advisory lock은 이 테스트에서는 미사용: 모두 서로 다른 기기다.
        var moderation = mock(PlaylistModerationService.class);
        when(moderation.validateCommentContent(null, null, null, "댓글", List.of())).thenReturn(true);
        var service = new PlaylistCommentService(songs, comments, moderation,
                mock(PlaylistNotificationService.class), mock(JdbcTemplate.class));
        var pool = Executors.newFixedThreadPool(8);
        var start = new CountDownLatch(1);
        try {
            var futures = new ArrayList<Future<Integer>>();
            for (int i = 0; i < 8; i++) {
                futures.add(pool.submit(() -> {
                    start.await();
                    return tx.execute(status -> service.create(songId, UUID.randomUUID(), new PlaylistCommentRequest("댓글")).anonymousNumber());
                }));
            }
            start.countDown();
            var numbers = new ArrayList<Integer>();
            for (var future : futures) numbers.add(future.get(15, TimeUnit.SECONDS));
            assertThat(numbers).containsExactlyInAnyOrder(1, 2, 3, 4, 5, 6, 7, 8);
        } finally {
            pool.shutdownNow();
        }
    }
}
