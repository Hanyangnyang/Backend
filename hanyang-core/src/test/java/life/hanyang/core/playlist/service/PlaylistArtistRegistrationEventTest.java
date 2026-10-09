package life.hanyang.core.playlist.service;

import life.hanyang.core.playlist.client.SpotifyApiClient;
import life.hanyang.core.playlist.event.PlaylistTrackRegisteredEvent;
import life.hanyang.core.playlist.exception.SpotifyServiceUnavailableException;
import life.hanyang.core.playlist.repository.PlaylistArtistRepository;
import life.hanyang.core.playlist.repository.PlaylistTrackArtistRepository;
import life.hanyang.core.playlist.repository.PlaylistTrackRepository;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.util.UUID;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class PlaylistArtistRegistrationEventTest {
    @Test
    void onlyCommittedRegistrationTriggersSyncAndSpotifyFailureKeepsRegistration() {
        try (var context = new AnnotationConfigApplicationContext(TestConfig.class)) {
            JdbcTemplate jdbc = context.getBean(JdbcTemplate.class);
            jdbc.execute("CREATE TABLE registrations (id VARCHAR(30) PRIMARY KEY)");
            var transaction = new TransactionTemplate(context.getBean(DataSourceTransactionManager.class));
            SpotifyApiClient client = context.getBean(SpotifyApiClient.class);
            PlaylistTrackArtistRepository links = context.getBean(PlaylistTrackArtistRepository.class);
            when(client.getTrackArtists("committed")).thenThrow(new SpotifyServiceUnavailableException());

            transaction.executeWithoutResult(status -> {
                jdbc.update("INSERT INTO registrations VALUES ('committed')");
                context.publishEvent(new PlaylistTrackRegisteredEvent("committed"));
                verifyNoInteractions(client, links);
            });
            verify(client).getTrackArtists("committed");
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM registrations", Integer.class)).isEqualTo(1);

            transaction.executeWithoutResult(status -> {
                jdbc.update("INSERT INTO registrations VALUES ('rolled-back')");
                context.publishEvent(new PlaylistTrackRegisteredEvent("rolled-back"));
                status.setRollbackOnly();
            });
            verify(client, never()).getTrackArtists("rolled-back");
            verify(links, never()).existsByTrackTrackId("rolled-back");
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM registrations", Integer.class)).isEqualTo(1);
        }
    }

    @TestConfiguration
    @EnableAsync
    @EnableTransactionManagement
    static class TestConfig {
        @Bean DriverManagerDataSource dataSource() {
            return new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
        }
        @Bean DataSourceTransactionManager transactionManager(DriverManagerDataSource source) {
            return new DataSourceTransactionManager(source);
        }
        @Bean JdbcTemplate jdbcTemplate(DriverManagerDataSource source) { return new JdbcTemplate(source); }
        // Deterministic execution while retaining the actual Spring @Async/event listener proxies.
        @Bean(name = "scrapingTaskExecutor") Executor executor() { return new SyncTaskExecutor(); }
        @Bean SpotifyApiClient client() { return mock(SpotifyApiClient.class); }
        @Bean PlaylistTrackRepository tracks() { return mock(PlaylistTrackRepository.class); }
        @Bean PlaylistArtistRepository artists() { return mock(PlaylistArtistRepository.class); }
        @Bean PlaylistTrackArtistRepository links() { return mock(PlaylistTrackArtistRepository.class); }
        @Bean PlaylistArtistSyncService service(SpotifyApiClient client, PlaylistTrackRepository tracks,
                                                PlaylistArtistRepository artists, PlaylistTrackArtistRepository links,
                                                DataSourceTransactionManager manager) {
            return new PlaylistArtistSyncService(client, tracks, artists, links, manager);
        }
    }
}
