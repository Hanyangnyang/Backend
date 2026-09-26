package life.hanyang.core.global.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxy;
import org.junit.jupiter.api.Test;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DiscordLogbackAppenderTest {

    private final DiscordLogbackAppender appender = new DiscordLogbackAppender();

    @Test
    void suppressesOpenTelemetryExporterErrors() {
        ILoggingEvent event = mock(ILoggingEvent.class);
        when(event.getLoggerName()).thenReturn("io.opentelemetry.exporter.internal.http.HttpExporter");

        assertThat(appender.shouldSuppress(event)).isTrue();
    }

    @Test
    void suppressesDisconnectedClientErrors() {
        ILoggingEvent event = mock(ILoggingEvent.class);
        AsyncRequestNotUsableException exception = new AsyncRequestNotUsableException(
                "ServletOutputStream failed to write: Broken pipe",
                new IOException("Broken pipe")
        );
        when(event.getThrowableProxy()).thenReturn(new ThrowableProxy(exception));

        assertThat(appender.shouldSuppress(event)).isTrue();
    }

    @Test
    void keepsUnexpectedApplicationErrors() {
        ILoggingEvent event = mock(ILoggingEvent.class);
        when(event.getLoggerName()).thenReturn("life.hanyang.core.SomeService");
        when(event.getThrowableProxy()).thenReturn(new ThrowableProxy(new IllegalStateException("failure")));

        assertThat(appender.shouldSuppress(event)).isFalse();
    }
}
