package life.hanyang.core.global.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import lombok.Setter;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Setter
public class DiscordLogbackAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {

    private String webhookUrl;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final ZoneId KST_ZONE = ZoneId.of("Asia/Seoul");
    private static final Pattern LOCATION_PATTERN = Pattern.compile("life\\.hanyang\\.[a-zA-Z0-9_.]+\\.[a-zA-Z0-9_]+");

    @Override
    protected void append(ILoggingEvent eventObject) {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            return;
        }

        try {
            String rawMessage = eventObject.getFormattedMessage();
            String loggerName = eventObject.getLoggerName();
            String timestamp = LocalDateTime.now(KST_ZONE).format(TIME_FORMATTER);

            // 위치 및 예외 타입 파악
            String location = extractLocation(loggerName, eventObject.getThrowableProxy());
            String reasonMessage = extractReasonMessage(rawMessage, eventObject.getThrowableProxy());

            // 콤팩트한 디스코드 Embed JSON 조립
            String jsonPayload = String.format("""
                    {
                      "embeds": [
                        {
                          "title": "🚨 [서버 에러 발생]",
                          "color": 15158332,
                          "fields": [
                            { "name": "📌 발생 위치", "value": "`%s`", "inline": true },
                            { "name": "🕒 발생 시각", "value": "`%s`", "inline": true },
                            { "name": "📝 원인 내용", "value": "```text\\n%s\\n```", "inline": false }
                          ]
                        }
                      ]
                    }
                    """,
                    escapeJson(location),
                    escapeJson(timestamp),
                    escapeJson(reasonMessage)
            );

            sendWebhook(jsonPayload);
        } catch (Exception e) {
            addError("디스코드 웹훅 알림 전송 실패", e);
        }
    }

    private String extractLocation(String loggerName, IThrowableProxy throwableProxy) {
        if (throwableProxy != null && throwableProxy.getStackTraceElementProxyArray() != null) {
            for (var proxy : throwableProxy.getStackTraceElementProxyArray()) {
                String ste = proxy.toString();
                Matcher matcher = LOCATION_PATTERN.matcher(ste);
                if (matcher.find()) {
                    return ste;
                }
            }
        }
        return loggerName;
    }

    private String extractReasonMessage(String rawMessage, IThrowableProxy throwableProxy) {
        if (throwableProxy != null) {
            String exName = throwableProxy.getClassName();
            String exMsg = throwableProxy.getMessage();
            return String.format("[%s] %s", exName, exMsg != null ? exMsg : rawMessage);
        }
        return rawMessage;
    }

    private void sendWebhook(String jsonPayload) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(webhookUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; utf-8");
            conn.setRequestProperty("Accept", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
            conn.getResponseCode();
        } catch (Exception ignored) {
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private String escapeJson(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
