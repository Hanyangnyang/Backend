package life.hanyang.core.playlist.repository;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

class PlaylistDailyPlayConstraintTest {
    @Test
    void uniqueConstraintAllowsOneConcurrentInsertAndResetsByDate() throws Exception {
        String url = "jdbc:h2:mem:daily_" + UUID.randomUUID() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1";
        String device = UUID.randomUUID().toString();
        try (var connection = DriverManager.getConnection(url); var statement = connection.createStatement()) {
            statement.execute("CREATE TABLE playlist_tracks (track_id VARCHAR(255) PRIMARY KEY)");
            statement.execute("INSERT INTO playlist_tracks VALUES ('track')");
            Path migration = Path.of("database/migrations/20261009_add_playlist_track_daily_devices.sql");
            if (!Files.exists(migration)) {
                migration = Path.of("../database/migrations/20261009_add_playlist_track_daily_devices.sql");
            }
            statement.execute(Files.readString(migration));
        }
        var executor = Executors.newFixedThreadPool(8);
        var start = new CountDownLatch(1);
        try {
            List<Future<Integer>> results = new ArrayList<>();
            for (int i = 0; i < 24; i++) {
                results.add(executor.submit(() -> {
                    start.await();
                    return insert(url, device, "2026-10-09");
                }));
            }
            start.countDown();
            int inserted = 0;
            for (Future<Integer> result : results) {
                inserted += result.get();
            }
            assertThat(inserted).isEqualTo(1);
            assertThat(insert(url, device, "2026-10-10")).isEqualTo(1);
            assertThat(insert(url, UUID.randomUUID().toString(), "2026-10-09")).isEqualTo(1);
            // A failed transaction must not consume the device's daily allowance.
            String retryDevice = UUID.randomUUID().toString();
            try (var connection = DriverManager.getConnection(url)) {
                connection.setAutoCommit(false);
                try (var statement = connection.prepareStatement(
                        "INSERT INTO playlist_track_daily_devices VALUES ('track', ?, DATE '2026-10-09')")) {
                    statement.setString(1, retryDevice);
                    statement.executeUpdate();
                }
                connection.rollback();
            }
            assertThat(insert(url, retryDevice, "2026-10-09")).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }

    private int insert(String url, String device, String day) throws SQLException {
        try (var connection = DriverManager.getConnection(url);
             var statement = connection.prepareStatement(
                     "INSERT INTO playlist_track_daily_devices VALUES ('track', ?, ?)")) {
            statement.setString(1, device);
            statement.setDate(2, java.sql.Date.valueOf(day));
            return statement.executeUpdate();
        } catch (SQLException exception) {
            if ("23505".equals(exception.getSQLState())) {
                return 0;
            }
            throw exception;
        }
    }
}
