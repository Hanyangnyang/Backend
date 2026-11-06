package life.hanyang.core.printer.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "printer_reports")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PrinterReport {
    @Id private UUID id;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private PrinterReportType type;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private PrinterReportStatus status;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "printer_id") private Printer printer;
    @Column(nullable = false) private UUID reporterDeviceId;
    @Column(nullable = false, columnDefinition = "text") private String proposal;
    @Column(columnDefinition = "text") private String originalSnapshot;
    @Column(columnDefinition = "text") private String appliedSnapshot;
    private Long originalVersion;
    @Column(length = 2000) private String description;
    @Column(nullable = false) private Instant createdAt;
    private Instant processedAt;
    @Column(length = 255) private String processedBy;
    @Column(length = 1000) private String rejectionReason;

    public PrinterReport(PrinterReportType type, Printer printer, UUID reporterDeviceId,
                         String proposal, String originalSnapshot, Long originalVersion, String description) {
        this.id = UUID.randomUUID();
        this.type = type;
        this.status = PrinterReportStatus.PENDING;
        this.printer = printer;
        this.reporterDeviceId = reporterDeviceId;
        this.proposal = proposal;
        this.originalSnapshot = originalSnapshot;
        this.originalVersion = originalVersion;
        this.description = description;
        this.createdAt = Instant.now();
    }

    public void approve(Printer printer, String appliedSnapshot, String admin) {
        this.printer = printer;
        this.appliedSnapshot = appliedSnapshot;
        process(PrinterReportStatus.APPROVED, admin);
    }

    public void reject(String reason, String admin) {
        this.rejectionReason = reason;
        process(PrinterReportStatus.REJECTED, admin);
    }

    private void process(PrinterReportStatus status, String admin) {
        this.status = status;
        this.processedBy = admin;
        this.processedAt = Instant.now();
    }
}
