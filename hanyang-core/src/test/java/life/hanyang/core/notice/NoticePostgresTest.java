package life.hanyang.core.notice;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManagerFactory;
import jakarta.validation.Validator;
import life.hanyang.core.campusmap.repository.CampusBuildingRepository;
import life.hanyang.core.global.exception.EntityNotFoundException;
import life.hanyang.core.notice.domain.Notice;
import life.hanyang.core.notice.dto.NoticeDto.*;
import life.hanyang.core.notice.repository.*;
import life.hanyang.core.notice.service.*;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.*;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.*;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.mock;

// Run with NOTICE_POSTGRES_TEST=true and a disposable PostgreSQL server on localhost:55439,
// database=postgres, user=notice_test; password is NOTICE_POSTGRES_TEST_PASSWORD (default: trust auth).
// This test resets ONLY notice_integration_test. CI provisions a dedicated disposable service.
@EnabledIfEnvironmentVariable(named = "NOTICE_POSTGRES_TEST", matches = "true")
@DataJpaTest(showSql = false, properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:55439/postgres?currentSchema=notice_integration_test",
        "spring.datasource.driver-class-name=org.postgresql.Driver",
        "spring.datasource.username=notice_test", "spring.datasource.password=${NOTICE_POSTGRES_TEST_PASSWORD:}",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.properties.hibernate.default_schema=notice_integration_test",
        "spring.jpa.properties.hibernate.generate_statistics=true",
        "spring.jpa.properties.hibernate.session.events.log=false",
        "spring.sql.init.mode=always",
        "spring.sql.init.schema-locations=classpath:notice-postgres-reset.sql,file:../database/migrations/20261117_create_notices.sql"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = NoticePostgresTest.Config.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class NoticePostgresTest {
    @Configuration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = Notice.class)
    @EnableJpaRepositories(basePackageClasses = NoticeRepository.class)
    @Import({NoticeService.class, NoticeQueryService.class, NoticeTargetPolicy.class})
    static class Config {
        @Bean Validator validator() { return new LocalValidatorFactoryBean(); }
        @Bean ObjectMapper mapper() { return new ObjectMapper().findAndRegisterModules(); }
        @Bean CampusBuildingRepository buildings() { return mock(CampusBuildingRepository.class); }
    }
    @Autowired NoticeService admin;
    @Autowired NoticeQueryService user;
    @Autowired NoticeRepository notices;
    @Autowired NoticeTargetRepository targets;
    @Autowired EntityManagerFactory factory;
    @Autowired JdbcTemplate jdbc;
    private final Instant now = Instant.now();
    private final Map<String, String> centralBus = Map.of("transportType", "cityBus", "stationId", "jungang");

    @BeforeEach void clean() { notices.deleteAllInBatch(); }
    private TargetInput all(String page) { return new TargetInput(page, Map.of()); }
    private Input input(String content, boolean enabled, Instant published, Instant expires, TargetInput... targets) {
        return new Input(content, enabled, published, expires, List.of(targets));
    }
    private AdminResponse publish(String content, TargetInput... targets) {
        return admin.create(input(content, true, now.minusSeconds(60), null, targets));
    }

    @Test
    void jsonbMatchingAppliesAndWithinTargetOrAcrossTargetsAndDeduplicatesBeforePagination() {
        var all = publish("전체 교통", all("transport"));
        var central = publish("중앙역 버스", new TargetInput("transport", Map.of(
                "transportType", List.of("cityBus"), "stationId", List.of("jungang"))));
        publish("학교 셔틀", new TargetInput("transport", Map.of("transportType", List.of("schoolShuttle"))));
        var multiple = publish("여러 조건 일치", all("transport"), new TargetInput("transport", Map.of(
                "transportType", List.of("schoolShuttle", "cityBus"))), all("menu"));
        publish("지도만", all("campusMap"));
        var first = user.list("transport", centralBus, 0, 2);
        assertThat(first.getTotalElements()).isEqualTo(3);
        assertThat(first.getContent()).extracting(UserResponse::id).containsExactly(multiple.id(), central.id());
        assertThat(user.list("transport", centralBus, 1, 2).getContent()).extracting(UserResponse::id).containsExactly(all.id());
        assertThat(user.list("transport", Map.of(), 0, 10).getContent()).extracting(UserResponse::id)
                .containsExactly(multiple.id(), all.id());
        assertThat(user.list("transport", Map.of("transportType", "schoolShuttle", "stationId", "jungang"), 0, 10))
                .extracting(UserResponse::content).doesNotContain("중앙역 버스");
        assertThat(user.list("menu", Map.of(), 0, 10)).extracting(UserResponse::id).containsExactly(multiple.id());
    }

    @Test
    void unsupportedContextValuesDoNotHidePageWideNoticesOrBroadenSpecificTargets() {
        var general = publish("교통 전체", all("transport"));
        publish("중앙역만", new TargetInput("transport", Map.of("stationId", List.of("jungang"))));
        assertThat(user.list("transport", Map.of("stationId", "new-station", "newChip", "new-value"), 0, 10))
                .extracting(UserResponse::id).containsExactly(general.id());
        var mapGeneral = publish("지도 전체", all("campusMap"));
        assertThat(user.list("campusMap", Map.of("category", "building", "buildingId", "removed-building"), 0, 10))
                .extracting(UserResponse::id).containsExactly(mapGeneral.id());
    }

    @Test
    void publicationWindowsAndPublishedTimeOrderingAreEnforcedInDatabase() {
        var old = admin.create(input("이전 공지", true, now.minusSeconds(600), null, all("menu")));
        var recent = admin.create(input("최근 공지", true, now.minusSeconds(60), null, all("menu")));
        admin.create(input("비활성", false, now.minusSeconds(10), null, all("menu")));
        admin.create(input("예약", true, now.plusSeconds(3600), null, all("menu")));
        var expired = admin.create(input("종료", true, now.minusSeconds(600), now.minusSeconds(1), all("menu")));
        admin.update(old.id(), input("내용 수정만", true, old.publishedAt(), null, all("menu")));
        assertThat(user.list("menu", Map.of(), 0, 10)).extracting(UserResponse::id).containsExactly(recent.id(), old.id());
        assertThat(admin.listAdmin("menu", null, 0, 10).getTotalElements()).isEqualTo(5);
        assertThat(notices.findVisible("menu", "{}", recent.publishedAt(), org.springframework.data.domain.PageRequest.of(0, 10)))
                .extracting(Notice::getId).containsExactly(recent.id(), expired.id(), old.id());
        assertThat(notices.findVisible("menu", "{}", expired.expiresAt(), org.springframework.data.domain.PageRequest.of(0, 10)))
                .extracting(Notice::getId).doesNotContain(expired.id());
    }

    @Test
    void userAndAdminPaginationHaveBoundedQueryCounts() {
        for (int i = 0; i < 12; i++) publish("공지 " + i, all("transport"), all("menu"));
        var stats = factory.unwrap(SessionFactory.class).getStatistics();
        stats.clear();
        assertThat(user.list("transport", centralBus, 0, 5)).hasSize(5);
        assertThat(stats.getPrepareStatementCount()).isEqualTo(2);
        stats.clear();
        var result = admin.listAdmin("transport", null, 0, 5);
        assertThat(result.getTotalElements()).isEqualTo(12);
        assertThat(result).allSatisfy(n -> assertThat(n.targets()).hasSize(2));
        assertThat(stats.getPrepareStatementCount()).isEqualTo(3);
    }

    @Test
    void schemaCascadeRemovesChildrenForServiceAndDirectSqlDeletes() {
        var first = publish("첫 공지", all("menu"), all("transport"));
        var second = publish("둘째 공지", all("menu"), all("campusMap"));
        admin.delete(first.id());
        assertThat(targets.count()).isEqualTo(2);
        jdbc.update("delete from notice where id = ?", second.id());
        assertThat(targets.count()).isZero();
        assertThatThrownBy(() -> admin.delete(first.id())).isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void concurrentReplacementAndDeletionCannotLeaveOrphansOrForeignKeyErrors() throws Exception {
        for (int i = 0; i < 5; i++) {
            var created = publish("공지", all("menu"), all("transport"));
            var start = new CountDownLatch(1);
            var pool = Executors.newFixedThreadPool(2);
            try {
                var update = pool.submit(() -> {
                    start.await();
                    try { admin.update(created.id(), input("수정", true, created.publishedAt(), null, all("campusMap"))); }
                    catch (EntityNotFoundException expected) { /* Delete won the parent lock. */ }
                    return null;
                });
                var delete = pool.submit(() -> { start.await(); admin.delete(created.id()); return null; });
                start.countDown();
                update.get(10, TimeUnit.SECONDS);
                delete.get(10, TimeUnit.SECONDS);
                assertThat(notices.existsById(created.id())).isFalse();
                assertThat(targets.count()).isZero();
            } finally { pool.shutdownNow(); }
        }
    }
}
