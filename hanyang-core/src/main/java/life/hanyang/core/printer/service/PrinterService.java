package life.hanyang.core.printer.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.validation.Validator;
import life.hanyang.core.campusmap.domain.*;
import life.hanyang.core.printer.domain.*;
import life.hanyang.core.printer.dto.PrinterDto.*;
import life.hanyang.core.printer.repository.*;
import life.hanyang.core.global.exception.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PrinterService {
    private static final Map<String, Integer> TEXT_LIMITS = Map.of(
            "buildingId", 100, "detailLocation", 1000, "name", 100,
            "price", 2000, "paymentMethod", 2000, "notes", 4000);
    private static final Set<String> NULLABLE_FIELDS = Set.of("name", "price", "paymentMethod", "notes");
    private final PrinterRepository printers;
    private final PrinterReportRepository reports;
    private final EntityManager entityManager;
    private final ObjectMapper mapper;
    private final Validator validator;

    public Page<Response> list(Campus campus, String buildingId, int page, int size) {
        return list(campus, buildingId, false, page, size);
    }

    public Page<Response> listAdmin(Campus campus, String buildingId, Boolean removed, int page, int size) {
        return list(campus, buildingId, removed, page, size);
    }

    private Page<Response> list(Campus campus, String buildingId, Boolean removed, int page, int size) {
        if (buildingId != null) {
            var building = building(buildingId);
            if (campus != null && building.getCampus() != campus) invalid("건물이 요청한 캠퍼스에 속하지 않습니다.");
        }
        return printers.findForListing(campus, buildingId, removed, page(page, size, Sort.unsorted())).map(Response::from);
    }

    public Response get(UUID id) { return Response.from(printer(id)); }

    @Transactional
    public Response create(Input input) {
        return Response.from(createPrinter(input));
    }

    @Transactional
    public Response update(UUID id, UpdateRequest request) {
        validate(request);
        Printer printer = printer(id);
        checkVersion(printer, request.expectedVersion());
        apply(printer, merge(printer, request.changes()));
        printers.flush();
        return Response.from(printer);
    }

    @Transactional
    public void delete(UUID id, long expectedVersion) {
        Printer printer = printer(id);
        checkVersion(printer, expectedVersion);
        printer.delete();
        printers.flush();
    }

    @Transactional
    public Receipt reportNew(NewReportRequest request) {
        validate(request);
        Input input = normalized(request.printer());
        building(input.buildingId());
        // Keep omitted booleans absent: a report must not claim a status the reporter did not supply.
        ObjectNode proposal = mapper.valueToTree(input);
        if (input.broken() == null) proposal.remove("broken");
        if (input.removed() == null) proposal.remove("removed");
        var report = reports.save(new PrinterReport(PrinterReportType.NEW, null, request.reporterDeviceId(),
                proposal.toString(), null, null, clean(request.description())));
        return new Receipt(report.getId(), report.getStatus());
    }

    @Transactional
    public Receipt reportUpdate(UUID id, UpdateReportRequest request) {
        validate(request);
        Printer printer = printer(id);
        checkVersion(printer, request.expectedVersion());
        ObjectNode changes = normalizedChanges(request.changes());
        Input proposed = merge(printer, changes);
        if (proposed.buildingId() != null) building(proposed.buildingId());
        if (mapper.valueToTree(input(printer)).equals(mapper.valueToTree(proposed))) invalid("변경된 정보가 없습니다.");
        var report = reports.save(new PrinterReport(PrinterReportType.UPDATE, printer, request.reporterDeviceId(),
                changes.toString(), mapper.valueToTree(input(printer)).toString(), printer.getVersion(), clean(request.description())));
        return new Receipt(report.getId(), report.getStatus());
    }

    public Page<ReportSummary> listReports(PrinterReportStatus status, PrinterReportType type, int page, int size) {
        return reports.findReports(status, type, page(page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")))).map(r -> new ReportSummary(
                r.getId(), r.getType(), r.getStatus(), r.getPrinter() == null ? null : r.getPrinter().getId(),
                r.getPrinter() == null ? null : r.getPrinter().getBuilding().getName(), json(r.getProposal()), r.getCreatedAt()));
    }

    public ReportDetail getReport(UUID id) {
        return detail(reports.findDetail(id).orElseThrow(() -> notFound("제보")));
    }

    @Transactional
    public ReportDetail approve(UUID id, ApproveRequest request, String admin) {
        validate(request);
        requireAdmin(admin);
        PrinterReport report = pending(id);
        Printer printer;
        if (report.getType() == PrinterReportType.NEW) {
            if (request.printer() == null || request.changes() != null || request.expectedVersion() != null)
                invalid("신규 제보 승인에는 최종 printer 정보만 전달해주세요.");
            printer = createPrinter(request.printer());
        } else {
            if (request.printer() != null || request.changes() == null || request.expectedVersion() == null)
                invalid("수정 제보 승인에는 expectedVersion과 changes가 필요합니다.");
            printer = report.getPrinter();
            if (printer.isDeleted()) throw notFound("프린터");
            checkVersion(printer, request.expectedVersion());
            apply(printer, merge(printer, request.changes()));
            printers.flush();
        }
        report.approve(printer, mapper.valueToTree(Response.from(printer)).toString(), admin);
        reports.flush();
        return detail(report);
    }

    @Transactional
    public ReportDetail reject(UUID id, RejectRequest request, String admin) {
        validate(request);
        requireAdmin(admin);
        PrinterReport report = pending(id);
        report.reject(request.reason().strip(), admin);
        reports.flush();
        return detail(report);
    }

    private PrinterReport pending(UUID id) {
        // Serialize processing of the same report. Printer @Version also protects competing reports/admin edits.
        PrinterReport report = reports.findForProcessing(id).orElseThrow(() -> notFound("제보"));
        if (report.getStatus() != PrinterReportStatus.PENDING)
            throw new BusinessException("이미 처리된 제보입니다.", ErrorCode.PRINTER_CONFLICT);
        return report;
    }

    private Printer createPrinter(Input input) {
        Input normalized = normalized(input);
        var printer = new Printer(building(normalized.buildingId()), normalized.detailLocation(), normalized.name(),
                normalized.price(), normalized.paymentMethod(), normalized.notes(),
                Boolean.TRUE.equals(normalized.broken()), Boolean.TRUE.equals(normalized.removed()));
        return printers.saveAndFlush(printer);
    }

    private void apply(Printer printer, Input input) {
        if (input(printer).equals(input)) {
            // Validate the reviewed version at commit even when no UPDATE is necessary.
            entityManager.lock(printer, LockModeType.OPTIMISTIC);
            return;
        }
        printer.update(building(input.buildingId()), input.detailLocation(), input.name(), input.price(),
                input.paymentMethod(), input.notes(), Boolean.TRUE.equals(input.broken()), Boolean.TRUE.equals(input.removed()));
    }

    private Input merge(Printer printer, ObjectNode changes) {
        ObjectNode merged = mapper.valueToTree(input(printer));
        merged.setAll(normalizedChanges(changes));
        return normalized(mapper.convertValue(merged, Input.class));
    }

    private ObjectNode normalizedChanges(ObjectNode changes) {
        if (changes == null || changes.isEmpty()) invalid("변경할 항목이 필요합니다.");
        ObjectNode result = changes.deepCopy();
        result.fields().forEachRemaining(entry -> {
            String field = entry.getKey();
            var value = entry.getValue();
            if (field.equals("broken") || field.equals("removed")) {
                if (!value.isBoolean()) invalid(field + "는 true 또는 false여야 합니다.");
            } else if (TEXT_LIMITS.containsKey(field)) {
                if (value.isNull()) {
                    if (!NULLABLE_FIELDS.contains(field)) invalid(field + "는 null일 수 없습니다.");
                } else {
                    if (!value.isTextual()) invalid(field + "는 문자열이어야 합니다.");
                    String text = clean(value.textValue());
                    if (text == null && !NULLABLE_FIELDS.contains(field)) invalid(field + "는 필수입니다.");
                    if (text != null && text.length() > TEXT_LIMITS.get(field)) invalid(field + "의 최대 길이를 초과했습니다.");
                    if (text == null) entry.setValue(mapper.nullNode());
                    else entry.setValue(mapper.getNodeFactory().textNode(text));
                }
            } else invalid("수정할 수 없는 항목입니다: " + field);
        });
        return result;
    }

    private Input normalized(Input input) {
        validate(input);
        return new Input(clean(input.buildingId()), clean(input.detailLocation()), clean(input.name()), clean(input.price()),
                clean(input.paymentMethod()), clean(input.notes()), input.broken(), input.removed());
    }

    private Input input(Printer p) {
        return new Input(p.getBuilding().getId(), p.getDetailLocation(), p.getName(), p.getPrice(),
                p.getPaymentMethod(), p.getNotes(), p.isBroken(), p.isRemoved());
    }

    private ReportDetail detail(PrinterReport r) {
        Printer printer = r.getPrinter();
        return new ReportDetail(r.getId(), r.getType(), r.getStatus(), r.getReporterDeviceId(), json(r.getProposal()),
                json(r.getOriginalSnapshot()), json(r.getAppliedSnapshot()), r.getOriginalVersion(), printer == null ? null : Response.from(printer),
                printer != null && printer.isDeleted(), r.getDescription(), r.getCreatedAt(), r.getProcessedAt(),
                r.getProcessedBy(), r.getRejectionReason());
    }

    private Printer printer(UUID id) { return printers.findActive(id).orElseThrow(() -> notFound("프린터")); }
    private CampusBuilding building(String id) {
        CampusBuilding building = entityManager.find(CampusBuilding.class, id);
        if (building == null) throw notFound("건물");
        return building;
    }

    private void checkVersion(Printer p, Long expectedVersion) {
        if (expectedVersion == null || p.getVersion() != expectedVersion)
            throw new BusinessException("프린터 정보가 변경되었습니다. 최신 정보를 확인해주세요.", ErrorCode.PRINTER_CONFLICT);
    }
    private void requireAdmin(String admin) {
        if (admin == null || admin.isBlank()) throw new BusinessException(ErrorCode.UNAUTHORIZED);
    }
    private void validate(Object input) {
        if (input == null) invalid("요청값은 필수입니다.");
        var violations = validator.validate(input);
        if (!violations.isEmpty()) invalid(violations.iterator().next().getPropertyPath() + ": " + violations.iterator().next().getMessage());
    }
    private Pageable page(int page, int size, Sort sort) {
        if (page < 0 || size < 1 || size > 100) invalid("page는 0 이상, size는 1~100이어야 합니다.");
        return PageRequest.of(page, size, sort);
    }
    private ObjectNode json(String value) {
        if (value == null) return null;
        try { return (ObjectNode) mapper.readTree(value); }
        catch (JsonProcessingException e) { throw new IllegalStateException("저장된 제보 형식이 올바르지 않습니다.", e); }
    }
    private static String clean(String value) { return value == null || value.isBlank() ? null : value.strip(); }
    private static void invalid(String message) { throw new IllegalArgumentException(message); }
    private static BusinessException notFound(String name) { return new BusinessException(name + "가 없습니다.", ErrorCode.ENTITY_NOT_FOUND); }
}
