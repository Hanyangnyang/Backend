package life.hanyang.core.printer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.persistence.EntityManagerFactory;
import jakarta.validation.Validator;
import life.hanyang.core.campusmap.domain.*;
import life.hanyang.core.campusmap.repository.*;
import life.hanyang.core.campusmap.service.CampusMapService;
import life.hanyang.core.global.exception.BusinessException;
import life.hanyang.core.printer.domain.*;
import life.hanyang.core.printer.dto.PrinterDto.*;
import life.hanyang.core.printer.repository.*;
import life.hanyang.core.printer.service.PrinterService;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.*;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.*;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;

@DataJpaTest(showSql = false, properties = {
        "spring.datasource.url=jdbc:h2:mem:printers;MODE=PostgreSQL;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.properties.hibernate.generate_statistics=true",
        "spring.jpa.properties.hibernate.session.events.log=false",
        "spring.sql.init.mode=always",
        "spring.sql.init.schema-locations=file:../database/migrations/20261001_create_campus_map.sql,file:../database/migrations/20261001_create_campus_parking_lots.sql,file:../database/migrations/20261106_create_printers.sql"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = PrinterPersistenceTest.Config.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PrinterPersistenceTest {
    @Configuration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = {CampusBuilding.class, Printer.class})
    @EnableJpaRepositories(basePackageClasses = {CampusBuildingRepository.class, PrinterRepository.class})
    @Import({PrinterService.class, CampusMapService.class})
    static class Config {
        @Bean Validator validator() { return new LocalValidatorFactoryBean(); }
        @Bean ObjectMapper objectMapper() { return new ObjectMapper().findAndRegisterModules(); }
    }
    @Autowired PrinterService service;
    @Autowired CampusMapService campusService;
    @Autowired CampusBuildingRepository buildings;
    @Autowired PrinterRepository printers;
    @Autowired PrinterReportRepository reports;
    @Autowired ObjectMapper mapper;
    @Autowired EntityManagerFactory factory;
    @Autowired PlatformTransactionManager transactionManager;
    private final UUID deviceId = UUID.randomUUID();

    @BeforeEach
    void setup() {
        reports.deleteAllInBatch();
        printers.deleteAllInBatch();
        buildings.deleteAll();
        building("b-1", Campus.ANSAN);
    }

    private void building(String id, Campus campus) {
        buildings.saveAndFlush(new CampusBuilding(id, id, "건물 " + id, null, campus,
                new Coordinates(null, null), null, List.of(), List.of(), List.of(), List.of()));
    }
    private Input input() { return new Input("b-1", " 1층 로비 ", null, "흑백 100원, 컬러 500원", "카드", null, null, null); }
    private ObjectNode changes(String json) throws Exception { return (ObjectNode) mapper.readTree(json); }

    @Test
    void defaultsTextNormalizationPartialUpdatesRemovalAndDeletion() throws Exception {
        var created = service.create(input());
        assertThat(created.broken()).isFalse();
        assertThat(created.removed()).isFalse();
        assertThat(created.detailLocation()).isEqualTo("1층 로비");
        assertThat(created.price()).isEqualTo("흑백 100원, 컬러 500원");
        var updated = service.update(created.id(), new UpdateRequest(created.version(), changes("{\"broken\":true,\"paymentMethod\":null}")));
        assertThat(updated.broken()).isTrue();
        assertThat(updated.paymentMethod()).isNull();
        assertThat(updated.price()).isEqualTo(created.price());
        assertThat(updated.version()).isGreaterThan(created.version());
        assertThatThrownBy(() -> service.update(created.id(), new UpdateRequest(created.version(), changes("{\"name\":\"stale\"}"))))
                .isInstanceOf(BusinessException.class);
        var removed = service.update(created.id(), new UpdateRequest(updated.version(), changes("{\"removed\":true}")));
        assertThat(service.list(null, null, 0, 20)).isEmpty();
        assertThat(service.get(created.id()).removed()).isTrue();
        var restored = service.update(created.id(), new UpdateRequest(removed.version(), changes("{\"removed\":false}")));
        assertThat(service.list(Campus.ANSAN, "b-1", 0, 20)).hasSize(1);
        service.delete(created.id(), restored.version());
        assertThatThrownBy(() -> service.get(created.id())).isInstanceOf(BusinessException.class);
        assertThat(printers.existsById(created.id())).isTrue();
        assertThatThrownBy(() -> campusService.deleteBuilding("b-1")).isInstanceOf(BusinessException.class);
    }

    @Test
    void validatesUnknownNullAndWrongTypedChangesAndFilters() throws Exception {
        var p = service.create(input());
        for (String json : List.of("{}", "{\"broken\":null}", "{\"removed\":\"true\"}",
                "{\"detailLocation\":null}", "{\"buildingId\":\" \"}", "{\"price\":100}", "{\"deleted\":true}")) {
            assertThatThrownBy(() -> service.update(p.id(), new UpdateRequest(p.version(), changes(json))))
                    .isInstanceOf(IllegalArgumentException.class);
        }
        assertThatThrownBy(() -> service.list(Campus.SEOUL, "b-1", 0, 20)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.list(null, null, 0, 101)).isInstanceOf(IllegalArgumentException.class);
        assertThat(service.list(Campus.SEOUL, null, 0, 20)).isEmpty();
        var empty = service.update(p.id(), new UpdateRequest(p.version(), changes("{\"price\":\"   \"}")));
        assertThat(empty.price()).isNull();
    }

    @Test
    void newReportsStayPrivateUntilApprovedAndRetainOriginalProposal() {
        var receipt = service.reportNew(new NewReportRequest(deviceId, input(), "여기에 있어요"));
        assertThat(service.list(null, null, 0, 20)).isEmpty();
        var detail = service.getReport(receipt.reportId());
        assertThat(detail.proposal().has("broken")).isFalse();
        assertThat(detail.proposal().has("removed")).isFalse();
        assertThat(detail.currentPrinter()).isNull();
        var approved = service.approve(receipt.reportId(), new ApproveRequest(input(), null, null), "admin");
        assertThat(approved.status()).isEqualTo(PrinterReportStatus.APPROVED);
        assertThat(approved.processedBy()).isEqualTo("admin");
        assertThat(approved.processedAt()).isNotNull();
        assertThat(service.list(null, null, 0, 20)).hasSize(1);
        assertThatThrownBy(() -> service.approve(receipt.reportId(), new ApproveRequest(input(), null, null), "admin"))
                .isInstanceOf(BusinessException.class);
        assertThat(printers.count()).isEqualTo(1);
    }

    @Test
    void partialReportPreservesBrokenStatusAndSupportsExplicitFalse() throws Exception {
        var p = service.create(new Input("b-1", "1층", null, null, null, null, true, false));
        var receipt = service.reportUpdate(p.id(), new UpdateReportRequest(deviceId, p.version(), changes("{\"price\":\"200원\"}"), null));
        assertThat(service.get(p.id()).price()).isNull();
        var approved = service.approve(receipt.reportId(), new ApproveRequest(null, p.version(), changes("{\"price\":\"200원\"}")), "admin");
        assertThat(approved.currentPrinter().broken()).isTrue();
        assertThat(approved.originalSnapshot().get("price").isNull()).isTrue();
        assertThat(approved.proposal().has("broken")).isFalse();
        var current = approved.currentPrinter();
        var fixed = service.reportUpdate(p.id(), new UpdateReportRequest(deviceId, current.version(), changes("{\"broken\":false}"), null));
        service.approve(fixed.reportId(), new ApproveRequest(null, current.version(), changes("{\"broken\":false}")), "admin");
        assertThat(service.get(p.id()).broken()).isFalse();
    }

    @Test
    void staleApprovalRollsBackAndRejectedOrDeletedReportsCannotApply() throws Exception {
        var p = service.create(input());
        var receipt = service.reportUpdate(p.id(), new UpdateReportRequest(deviceId, p.version(), changes("{\"removed\":true}"), null));
        var edited = service.update(p.id(), new UpdateRequest(p.version(), changes("{\"notes\":\"관리자 수정\"}")));
        assertThatThrownBy(() -> service.approve(receipt.reportId(), new ApproveRequest(null, p.version(), changes("{\"removed\":true}")), "admin"))
                .isInstanceOf(BusinessException.class);
        assertThat(service.getReport(receipt.reportId()).status()).isEqualTo(PrinterReportStatus.PENDING);
        service.reject(receipt.reportId(), new RejectRequest("확인 불가"), "admin");
        assertThatThrownBy(() -> service.approve(receipt.reportId(), new ApproveRequest(null, edited.version(), changes("{\"removed\":true}")), "admin"))
                .isInstanceOf(BusinessException.class);
        var second = service.reportUpdate(p.id(), new UpdateReportRequest(deviceId, edited.version(), changes("{\"removed\":true}"), null));
        service.delete(p.id(), edited.version());
        assertThatThrownBy(() -> service.approve(second.reportId(), new ApproveRequest(null, edited.version(), changes("{\"removed\":true}")), "admin"))
                .isInstanceOf(BusinessException.class);
        assertThat(service.getReport(second.reportId()).printerDeleted()).isTrue();
        assertThat(service.getReport(second.reportId()).status()).isEqualTo(PrinterReportStatus.PENDING);
    }

    @Test
    void invalidApprovalDoesNotCreatePrinterOrProcessReport() {
        var receipt = service.reportNew(new NewReportRequest(deviceId, input(), null));
        assertThatThrownBy(() -> service.approve(receipt.reportId(), new ApproveRequest(null, null, null), "admin"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.approve(receipt.reportId(), new ApproveRequest(input(), null, null), null))
                .isInstanceOf(BusinessException.class);
        assertThat(printers.count()).isZero();
        assertThat(service.getReport(receipt.reportId()).status()).isEqualTo(PrinterReportStatus.PENDING);
    }

    @Test
    void printerAndReportPaginationUseBoundedQueriesAcrossDistinctBuildings() throws Exception {
        for (int i = 0; i < 12; i++) {
            String id = "distinct-" + i;
            building(id, Campus.ANSAN);
            var p = service.create(new Input(id, "1층", null, null, null, null, null, null));
            service.reportUpdate(p.id(), new UpdateReportRequest(deviceId, p.version(), changes("{\"broken\":true}"), null));
        }
        var stats = factory.unwrap(SessionFactory.class).getStatistics();
        stats.clear();
        var printersPage = service.list(null, null, 0, 5);
        assertThat(printersPage.getTotalElements()).isEqualTo(12);
        assertThat(printersPage.getContent()).allSatisfy(p -> assertThat(p.buildingName()).startsWith("건물 distinct-"));
        assertThat(stats.getPrepareStatementCount()).isEqualTo(2); // content + count, including DTO conversion
        stats.clear();
        service.list(null, null, 0, 10).getContent().forEach(p -> assertThat(p.campus()).isEqualTo(Campus.ANSAN));
        assertThat(stats.getPrepareStatementCount()).isEqualTo(2);
        stats.clear();
        assertThat(service.listAdmin(null, null, null, 0, 10)).hasSize(10);
        assertThat(stats.getPrepareStatementCount()).isEqualTo(2);
        stats.clear();
        var reportsPage = service.listReports(PrinterReportStatus.PENDING, PrinterReportType.UPDATE, 0, 5);
        assertThat(reportsPage.getTotalElements()).isEqualTo(12);
        assertThat(reportsPage.getContent()).allSatisfy(r -> assertThat(r.buildingName()).startsWith("건물 distinct-"));
        assertThat(stats.getPrepareStatementCount()).isEqualTo(2);
        stats.clear();
        service.listReports(null, null, 0, 10).getContent();
        assertThat(stats.getPrepareStatementCount()).isEqualTo(2);
    }

    @Test
    void adminCanFindRemovedPrintersAndRestoreThemWithoutExposingDeletedOnes() throws Exception {
        var active = service.create(input());
        var removed = service.create(new Input("b-1", "2층", null, null, null, null, false, true));
        var deleted = service.create(input());
        service.delete(deleted.id(), deleted.version());
        assertThat(service.listAdmin(null, null, null, 0, 20)).extracting(Response::id)
                .containsExactlyInAnyOrder(active.id(), removed.id());
        assertThat(service.listAdmin(Campus.ANSAN, "b-1", true, 0, 20)).extracting(Response::id).containsExactly(removed.id());
        assertThat(service.listAdmin(null, null, false, 0, 20)).extracting(Response::id).containsExactly(active.id());
        assertThat(service.list(null, null, 0, 20)).extracting(Response::id).containsExactly(active.id());
        service.update(removed.id(), new UpdateRequest(removed.version(), changes("{\"removed\":false}")));
        assertThat(service.listAdmin(null, null, true, 0, 20)).isEmpty();
        assertThat(service.list(null, null, 0, 20)).hasSize(2);
    }

    @Test
    void unchangedAdminEditsKeepTimestampVersionAndPendingUserReportValid() throws Exception {
        var p = service.create(input());
        var receipt = service.reportUpdate(p.id(), new UpdateReportRequest(deviceId, p.version(), changes("{\"broken\":true}"), null));
        var persisted = service.get(p.id());
        var stats = factory.unwrap(SessionFactory.class).getStatistics();
        stats.clear();
        var same = service.update(p.id(), new UpdateRequest(p.version(), changes("""
                {"detailLocation":" 1층 로비 ","notes":" ","broken":false}
                """)));
        assertThat(same.version()).isEqualTo(p.version());
        assertThat(same.updatedAt()).isEqualTo(persisted.updatedAt());
        assertThat(stats.getEntityUpdateCount()).isZero();
        var approved = service.approve(receipt.reportId(), new ApproveRequest(null, p.version(), changes("{\"broken\":true}")), "admin");
        assertThat(approved.currentPrinter().broken()).isTrue();
    }

    @Test
    void approvalKeepsFinalSnapshotEvenAfterLaterEditsAndDeletion() throws Exception {
        var receipt = service.reportNew(new NewReportRequest(deviceId, input(), null));
        var finalInput = new Input("b-1", "2층", "확인한 이름", "200원", "카드", null, false, false);
        var approved = service.approve(receipt.reportId(), new ApproveRequest(finalInput, null, null), "admin");
        assertThat(approved.proposal().get("price").asText()).isEqualTo(input().price());
        assertThat(approved.appliedSnapshot().get("price").asText()).isEqualTo("200원");
        assertThat(approved.appliedSnapshot().get("buildingName").asText()).isEqualTo("건물 b-1");
        var p = approved.currentPrinter();
        var edited = service.update(p.id(), new UpdateRequest(p.version(), changes("{\"price\":\"300원\"}")));
        service.delete(p.id(), edited.version());
        var reloaded = service.getReport(receipt.reportId());
        assertThat(reloaded.appliedSnapshot()).isEqualTo(approved.appliedSnapshot());
        assertThat(reloaded.currentPrinter().price()).isEqualTo("300원");
        assertThat(reloaded.printerDeleted()).isTrue();
    }

    @Test
    void unchangedApprovalRecordsSnapshotWithoutChangingPrinterVersion() throws Exception {
        var p = service.create(input());
        var receipt = service.reportUpdate(p.id(), new UpdateReportRequest(deviceId, p.version(), changes("{\"broken\":true}"), null));
        var corrected = service.update(p.id(), new UpdateRequest(p.version(), changes("{\"broken\":true}")));
        corrected = service.get(p.id());
        var approved = service.approve(receipt.reportId(), new ApproveRequest(null, corrected.version(), changes("{\"broken\":true}")), "admin");
        assertThat(approved.status()).isEqualTo(PrinterReportStatus.APPROVED);
        assertThat(approved.currentPrinter().version()).isEqualTo(corrected.version());
        assertThat(approved.currentPrinter().updatedAt()).isEqualTo(corrected.updatedAt());
        assertThat(approved.appliedSnapshot().get("broken").booleanValue()).isTrue();
        assertThat(approved.appliedSnapshot().get("version").longValue()).isEqualTo(corrected.version());
    }

    @Test
    void sortsByBuildingThenPrinterNameWithLocationFallbackAndStablePagination() {
        buildings.saveAndFlush(new CampusBuilding("z-building", "z", "가건물", null, Campus.ANSAN,
                new Coordinates(null, null), null, List.of(), List.of(), List.of(), List.of()));
        buildings.saveAndFlush(new CampusBuilding("a-building", "a", "나건물", null, Campus.ANSAN,
                new Coordinates(null, null), null, List.of(), List.of(), List.of(), List.of()));
        var laterBuilding = service.create(new Input("a-building", "1층", "가프린터", null, null, null, false, false));
        var named = service.create(new Input("z-building", "1층", "나프린터", null, null, null, false, false));
        var unnamed = service.create(new Input("z-building", "가위치", null, null, null, null, false, false));
        var tieLater = service.create(new Input("z-building", "다위치", "나프린터", null, null, null, false, false));
        var tieFirst = service.create(new Input("z-building", "나위치", "나프린터", null, null, null, false, false));
        var tieSame = service.create(new Input("z-building", "나위치", "나프린터", null, null, null, false, false));
        var tiedIds = new ArrayList<>(List.of(tieFirst.id(), tieSame.id()));
        tiedIds.sort(Comparator.comparing(UUID::toString));
        var expected = List.of(unnamed.id(), named.id(), tiedIds.get(0), tiedIds.get(1), tieLater.id(), laterBuilding.id());
        assertThat(service.list(null, null, 0, 20)).extracting(Response::id).containsExactlyElementsOf(expected);
        assertThat(service.listAdmin(null, null, null, 0, 20)).extracting(Response::id).containsExactlyElementsOf(expected);
        var pagedIds = new ArrayList<UUID>();
        for (int page = 0; page < 3; page++) {
            service.list(null, null, page, 2).forEach(p -> pagedIds.add(p.id()));
        }
        assertThat(pagedIds).containsExactlyElementsOf(expected);
        assertThat(service.list(Campus.ANSAN, "z-building", 0, 20)).extracting(Response::id)
                .containsExactlyElementsOf(expected.subList(0, 5));
    }

    @Test
    void optionalPrinterFiltersSupportNullAndNonNullValuesInContentAndCountQueries() {
        building("b-2", Campus.SEOUL);
        var visible = service.create(input());
        var removed = service.create(new Input("b-1", "2층", null, null, null, null, false, true));
        var seoul = service.create(new Input("b-2", "1층", null, null, null, null, false, false));
        for (Campus campus : new Campus[]{null, Campus.ANSAN, Campus.SEOUL}) {
            for (String buildingId : new String[]{null, "b-1", "b-2"}) {
                if (campus != null && buildingId != null && (campus == Campus.ANSAN) != buildingId.equals("b-1")) continue;
                for (Boolean removedFilter : new Boolean[]{null, false, true}) {
                    var expected = List.of(visible, removed, seoul).stream()
                            .filter(p -> campus == null || p.campus() == campus)
                            .filter(p -> buildingId == null || p.buildingId().equals(buildingId))
                            .filter(p -> removedFilter == null || p.removed() == removedFilter).toList();
                    var result = service.listAdmin(campus, buildingId, removedFilter, 0, 1);
                    assertThat(result.getTotalElements()).isEqualTo(expected.size());
                    assertThat(result.getContent()).allSatisfy(p -> assertThat(expected).extracting(Response::id).contains(p.id()));
                }
            }
        }
    }

    @Test
    void optionalReportFiltersSupportNullAndNonNullValuesInContentAndCountQueries() throws Exception {
        var printer = service.create(input());
        var pendingNew = service.reportNew(new NewReportRequest(deviceId, input(), null));
        var rejectedNew = service.reportNew(new NewReportRequest(deviceId, input(), null));
        service.reject(rejectedNew.reportId(), new RejectRequest("중복 제보"), "admin");
        var update = service.reportUpdate(printer.id(), new UpdateReportRequest(deviceId, printer.version(), changes("{\"broken\":true}"), null));
        service.approve(update.reportId(), new ApproveRequest(null, printer.version(), changes("{\"broken\":true}")), "admin");
        var all = List.of(service.getReport(pendingNew.reportId()), service.getReport(rejectedNew.reportId()), service.getReport(update.reportId()));
        for (PrinterReportStatus status : new PrinterReportStatus[]{null, PrinterReportStatus.PENDING, PrinterReportStatus.APPROVED, PrinterReportStatus.REJECTED}) {
            for (PrinterReportType type : new PrinterReportType[]{null, PrinterReportType.NEW, PrinterReportType.UPDATE}) {
                var expected = all.stream().filter(r -> status == null || r.status() == status)
                        .filter(r -> type == null || r.type() == type).toList();
                var result = service.listReports(status, type, 0, 1);
                assertThat(result.getTotalElements()).isEqualTo(expected.size());
                assertThat(result.getContent()).allSatisfy(r -> assertThat(expected).extracting(ReportDetail::id).contains(r.id()));
            }
        }
    }

    @Test
    void concurrentPrinterWritesCannotOverwriteEachOther() throws Exception {
        var p = service.create(input());
        var barrier = new CyclicBarrier(2);
        var pool = Executors.newFixedThreadPool(2);
        try {
            Callable<Boolean> edit = () -> {
                try {
                    new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                        var entity = printers.findActive(p.id()).orElseThrow();
                        try { barrier.await(5, TimeUnit.SECONDS); }
                        catch (Exception e) { throw new RuntimeException(e); }
                        entity.update(entity.getBuilding(), entity.getDetailLocation(), "수정", entity.getPrice(),
                                entity.getPaymentMethod(), entity.getNotes(), true, false);
                        printers.flush();
                    });
                    return true;
                } catch (ObjectOptimisticLockingFailureException e) { return false; }
            };
            var first = pool.submit(edit);
            var second = pool.submit(edit);
            assertThat(List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(true, false);
            assertThat(service.get(p.id()).version()).isEqualTo(p.version() + 1);
        } finally { pool.shutdownNow(); }
    }

    @Test
    void concurrentApprovalsCreateExactlyOnePrinter() throws Exception {
        var receipt = service.reportNew(new NewReportRequest(deviceId, input(), null));
        var pool = Executors.newFixedThreadPool(2);
        var start = new CountDownLatch(1);
        try {
            Callable<Boolean> approve = () -> {
                start.await();
                try {
                    service.approve(receipt.reportId(), new ApproveRequest(input(), null, null), "admin");
                    return true;
                } catch (BusinessException e) { return false; }
            };
            var first = pool.submit(approve);
            var second = pool.submit(approve);
            start.countDown();
            assertThat(List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(true, false);
            assertThat(printers.count()).isEqualTo(1);
            assertThat(service.getReport(receipt.reportId()).status()).isEqualTo(PrinterReportStatus.APPROVED);
        } finally { pool.shutdownNow(); }
    }
}
