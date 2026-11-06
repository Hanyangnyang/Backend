package life.hanyang.core.printer.event;

import life.hanyang.core.campusmap.domain.Campus;
import life.hanyang.core.printer.domain.PrinterReportType;
import java.util.UUID;

public record PrinterReportCreatedEvent(
        UUID reportId,
        PrinterReportType type,
        UUID printerId,
        Campus campus,
        String buildingName,
        String detailLocation,
        String proposal,
        String description
) {}
