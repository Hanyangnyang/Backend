package life.hanyang.core.printer.dto;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import life.hanyang.core.campusmap.domain.Campus;
import life.hanyang.core.printer.domain.*;
import java.time.Instant;
import java.util.UUID;

public final class PrinterDto {
    private PrinterDto() {}

    public record Input(
            @NotBlank @Size(max = 100) String buildingId,
            @NotBlank @Size(max = 1000) String detailLocation,
            @Size(max = 100) String name,
            @Size(max = 2000) String price,
            @Size(max = 2000) String paymentMethod,
            @Size(max = 4000) String notes,
            @Schema(description = "등록 시 생략하면 false. null도 미입력으로 취급") Boolean broken,
            @Schema(description = "철거 여부. 등록 시 생략하면 false") Boolean removed) {}

    public record UpdateRequest(
            @NotNull @PositiveOrZero Long expectedVersion,
            @Schema(description = "변경 항목만 전달. name/price/paymentMethod/notes의 null은 값 삭제. 나머지는 null 불가")
            @NotNull ObjectNode changes) {}

    public record NewReportRequest(@NotNull UUID reporterDeviceId,
                                   @NotNull @Valid Input printer,
                                   @Size(max = 2000) String description) {}

    public record UpdateReportRequest(@NotNull UUID reporterDeviceId,
                                      @NotNull @PositiveOrZero Long expectedVersion,
                                      @NotNull ObjectNode changes,
                                      @Size(max = 2000) String description) {}

    @Schema(description = "NEW: printer 필수. UPDATE: expectedVersion과 changes 필수. 유형에 맞지 않는 필드는 전달하지 않음")
    public record ApproveRequest(@Valid Input printer, @PositiveOrZero Long expectedVersion, ObjectNode changes) {}
    public record RejectRequest(@NotBlank @Size(max = 1000) String reason) {}
    public record Receipt(UUID reportId, PrinterReportStatus status) {}

    public record Response(UUID id, String buildingId, String buildingName, Campus campus,
                           String detailLocation, String name, String price, String paymentMethod,
                           String notes, boolean broken, boolean removed, Instant updatedAt, long version) {
        public static Response from(Printer p) {
            return new Response(p.getId(), p.getBuilding().getId(), p.getBuilding().getName(), p.getBuilding().getCampus(),
                    p.getDetailLocation(), p.getName(), p.getPrice(), p.getPaymentMethod(), p.getNotes(),
                    p.isBroken(), p.isRemoved(), p.getUpdatedAt(), p.getVersion());
        }
    }

    public record ReportSummary(UUID id, PrinterReportType type, PrinterReportStatus status,
                                UUID printerId, String buildingName, ObjectNode proposal, Instant createdAt) {}
    public record ReportDetail(UUID id, PrinterReportType type, PrinterReportStatus status,
                               UUID reporterDeviceId, ObjectNode proposal, ObjectNode originalSnapshot, ObjectNode appliedSnapshot,
                               Long originalVersion, Response currentPrinter, boolean printerDeleted,
                               String description, Instant createdAt, Instant processedAt,
                               String processedBy, String rejectionReason) {}
}
