package life.hanyang.core.playlist.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.mockito.Mockito.*;

class PlaylistNotificationDispatcherTest {
    final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    final PlaylistFcmSender sender = mock(PlaylistFcmSender.class);
    final PlaylistNotificationDispatcher dispatcher = new PlaylistNotificationDispatcher(jdbc, sender, new ObjectMapper());
    final UUID id = UUID.randomUUID();
    final UUID song = UUID.randomUUID();
    final UUID comment = UUID.randomUUID();

    @BeforeEach void setup() {
        when(sender.configured()).thenReturn(true);
    }

    private void pending(int attempts, boolean targetExists) {
        when(jdbc.queryForList(anyString())).thenReturn(List.of(Map.of("id", id, "song_id", song,
                "comment_id", comment, "type", "COMMENT", "attempts", attempts)));
        when(jdbc.queryForList(anyString(), eq(song), eq("comments"), eq(comment), eq(comment)))
                .thenReturn(targetExists ? List.of(Map.of("fcm_token", "fake-token", "platform", "web", "track_id", "track")) : List.of());
    }

    @Test void successfulSendMarksSent() {
        pending(0, true);
        dispatcher.dispatchNext();
        verify(sender).send(id, "fake-token", "web", "COMMENT", song, "track");
        verify(jdbc).update(contains("status = 'SENT'"), eq(id));
    }

    @Test void deletedContentOrUnsubscribedRecipientSkipsWithoutSending() {
        pending(0, false);
        dispatcher.dispatchNext();
        verify(sender, never()).send(any(), any(), any(), any(), any(), any());
        verify(jdbc).update(contains("status = 'SKIPPED'"), eq(id));
    }

    @Test void transientFailureSchedulesRetry() {
        pending(0, true);
        doThrow(new IllegalStateException()).when(sender).send(any(), any(), any(), any(), any(), any());
        dispatcher.dispatchNext();
        verify(jdbc).update(contains("next_attempt_at"), eq(1), eq("PENDING"), eq("IllegalStateException"), eq(60), eq(id));
    }

    @Test void fifthFailureStopsRetrying() {
        pending(4, true);
        doThrow(new IllegalStateException()).when(sender).send(any(), any(), any(), any(), any(), any());
        dispatcher.dispatchNext();
        verify(jdbc).update(contains("next_attempt_at"), eq(5), eq("FAILED"), eq("IllegalStateException"), eq(960), eq(id));
    }

    @Test void unregisteredTokenIsTerminalWithoutLoggingResponseBody() {
        pending(0, true);
        var error = HttpClientErrorException.create(HttpStatus.NOT_FOUND, "Not found", new HttpHeaders(),
                "{\"error\":{\"details\":[{\"errorCode\":\"UNREGISTERED\"}]}}".getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
        doThrow(error).when(sender).send(any(), any(), any(), any(), any(), any());
        dispatcher.dispatchNext();
        verify(jdbc).update(contains("next_attempt_at"), eq(1), eq("FAILED"), eq("HTTP_404"), eq(60), eq(id));
    }
}
