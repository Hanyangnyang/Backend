package life.hanyang.core.playlist.event;

import java.util.UUID;

public record PlaylistReportCreatedEvent(
        UUID reportId,
        UUID songId,
        String title,
        String artist,
        String comment,
        String reason
) {}
