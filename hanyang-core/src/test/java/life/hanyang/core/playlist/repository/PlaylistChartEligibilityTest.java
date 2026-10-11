package life.hanyang.core.playlist.repository;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.data.jpa.repository.Query;
import java.time.Instant;
import java.sql.Timestamp;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;

class PlaylistChartEligibilityTest {
    @Test
    void playOnlyAndLikeOnlyTracksEnterOverallButNotGenreCharts() throws Exception {
        var dataSource = new DriverManagerDataSource("jdbc:h2:mem:chart-eligibility;MODE=PostgreSQL;DB_CLOSE_DELAY=-1", "sa", "");
        var jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("CREATE TABLE playlist_tracks(track_id varchar PRIMARY KEY, title varchar, artist varchar, album_art_url varchar, created_at timestamp)");
        jdbc.execute("CREATE TABLE playlist_songs(id uuid PRIMARY KEY, track_id varchar, created_at timestamp, deleted_at timestamp)");
        jdbc.execute("CREATE TABLE playlist_song_genres(song_id uuid, genre varchar)");
        jdbc.execute("CREATE TABLE playlist_track_likes(id uuid, track_id varchar, device_id uuid, created_at timestamp)");
        jdbc.execute("CREATE TABLE playlist_track_hourly_plays(track_id varchar, play_hour timestamp, play_count int)");
        jdbc.execute("CREATE TABLE playlist_song_reactions(song_id uuid, device_id uuid, created_at timestamp)");
        jdbc.execute("CREATE TABLE playlist_track_artists(track_id varchar, artist_id uuid, artist_order int)");
        jdbc.execute("CREATE TABLE playlist_artists(id uuid, name varchar)");
        var time = Timestamp.from(Instant.parse("2026-10-10T12:00:00Z"));
        for (String id : List.of("play-only", "like-only", "band", "no-activity")) {
            jdbc.update("INSERT INTO playlist_tracks VALUES (?, ?, 'artist', null, ?)", id, id, time);
        }
        jdbc.update("INSERT INTO playlist_track_hourly_plays VALUES ('play-only', ?, 3)", time);
        jdbc.update("INSERT INTO playlist_track_likes VALUES (?, 'like-only', ?, ?)", UUID.randomUUID(), UUID.randomUUID(), time);
        UUID postId = UUID.randomUUID();
        jdbc.update("INSERT INTO playlist_songs VALUES (?, 'band', ?, null)", postId, time);
        jdbc.update("INSERT INTO playlist_song_genres VALUES (?, 'BAND')", postId);
        var named = new NamedParameterJdbcTemplate(dataSource);
        var parameters = new HashMap<String, Object>();
        parameters.put("h24", Timestamp.from(Instant.parse("2026-10-10T00:00:00Z")));
        parameters.put("h6", Timestamp.from(Instant.parse("2026-10-10T10:00:00Z")));
        parameters.put("upperBound", Timestamp.from(Instant.parse("2026-10-11T00:00:00Z")));
        parameters.put("startPeriod", parameters.get("h24"));
        parameters.put("endPeriod", parameters.get("upperBound"));
        parameters.put("limit", 100);
        for (var method : PlaylistTrackHourlyPlayRepository.class.getMethods()) {
            if (!Set.of("findRisingChartRaw", "findWeeklyChartRaw", "findMonthlyChartRaw").contains(method.getName())) continue;
            // Only PostgreSQL string_agg syntax differs in H2; eligibility and scoring SQL are unchanged.
            String sql = method.getAnnotation(Query.class).value().replace(
                    "string_agg(a.name, ', ' ORDER BY ta.artist_order)",
                    "LISTAGG(a.name, ', ') WITHIN GROUP (ORDER BY ta.artist_order)");
            parameters.put("genre", null);
            assertThat(named.query(sql, parameters, (rs, row) -> rs.getString("track_id")))
                    .containsExactlyInAnyOrder("play-only", "like-only", "band");
            parameters.put("genre", "BAND");
            assertThat(named.query(sql, parameters, (rs, row) -> rs.getString("track_id"))).containsExactly("band");
        }
    }
}
