package life.hanyang.core.notice;

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
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.mock;

@DataJpaTest(showSql = false, properties = {
        "spring.datasource.url=jdbc:h2:mem:notices;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.generate_statistics=true",
        "spring.jpa.properties.hibernate.session.events.log=false"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = NoticeAdminPersistenceTest.Config.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class NoticeAdminPersistenceTest {
    @Configuration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = Notice.class)
    @EnableJpaRepositories(basePackageClasses = NoticeRepository.class)
    @Import({NoticeService.class, NoticeTargetPolicy.class})
    static class Config {
        @Bean Validator validator() { return new LocalValidatorFactoryBean(); }
        @Bean CampusBuildingRepository buildings() { return mock(CampusBuildingRepository.class); }
    }
    @Autowired NoticeService service;
    @Autowired NoticeRepository notices;
    @Autowired NoticeTargetRepository targets;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManagerFactory factory;
    private static final Instant PUBLISHED = Instant.parse("2026-11-17T00:00:00Z");

    @BeforeEach void clean() { notices.deleteAllInBatch(); }

    private Input input(String content, List<TargetInput> targets) {
        return new Input(content, true, PUBLISHED, null, targets);
    }
    private TargetInput target(String page) { return new TargetInput(page, Map.of()); }

    @Test
    void createsReplacesAndDeletesAllTargetsWithoutForeignKeyConflicts() {
        var created = service.create(input("첫 공지", List.of(target("menu"), target("transport"))));
        assertThat(created.targets()).hasSize(2);
        assertThat(created.createdAt()).isNotNull();
        var oldIds = created.targets().stream().map(TargetResponse::id).toList();
        var updated = service.update(created.id(), input("수정 공지", List.of(target("campusMap"))));
        assertThat(updated.content()).isEqualTo("수정 공지");
        assertThat(updated.targets()).extracting(TargetResponse::page).containsExactly("campusMap");
        assertThat(targets.findAllById(oldIds)).isEmpty();
        service.delete(created.id());
        assertThat(notices.count()).isZero();
        assertThat(targets.count()).isZero();
        assertThatThrownBy(() -> service.delete(created.id())).isInstanceOf(EntityNotFoundException.class);
        assertThatThrownBy(() -> service.get(created.id())).isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void databaseCascadeAlsoProtectsDirectParentDeletion() {
        var created = service.create(input("공지", List.of(target("menu"), target("transport"))));
        jdbc.update("delete from notice where id = ?", created.id());
        assertThat(targets.count()).isZero();
    }

    @Test
    void invalidReplacementRollsBackWithoutRemovingOriginalTargetsOrContent() {
        var created = service.create(input("원본", List.of(target("menu"))));
        assertThatThrownBy(() -> service.update(created.id(), input("변경", List.of(target("unknown")))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.update(created.id(), new Input("변경", true, PUBLISHED,
                PUBLISHED.minusSeconds(1), List.of(target("transport")))))
                .isInstanceOf(IllegalArgumentException.class);
        var unchanged = service.get(created.id());
        assertThat(unchanged.content()).isEqualTo("원본");
        assertThat(unchanged.targets()).isEqualTo(created.targets());
    }

    @Test
    void validatesAllRequiredInputsAndPagination() {
        assertThatThrownBy(() -> service.create(new Input(" ", true, PUBLISHED, null, List.of(target("menu")))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.create(new Input("공지", null, PUBLISHED, null, List.of(target("menu")))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.listAdmin(null, null, -1, 10)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.listAdmin(null, null, 0, 51)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.listAdmin(null, null, Integer.MAX_VALUE, 50)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void adminPaginationUsesThreeQueriesRegardlessOfNoticeCountAndDoesNotDuplicateNotices() {
        for (int i = 0; i < 12; i++) {
            service.create(input("공지 " + i, List.of(target("menu"), target("transport"),
                    new TargetInput("menu", Map.of("cafeteriaCode", List.of("re11"))))));
        }
        var statistics = factory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        var result = service.listAdmin("menu", true, 0, 5);
        assertThat(result.getTotalElements()).isEqualTo(12);
        assertThat(result.getContent()).hasSize(5);
        assertThat(result.getContent()).allSatisfy(n -> assertThat(n.targets()).hasSize(3));
        assertThat(result.getContent()).extracting(AdminResponse::id).isSortedAccordingTo(Comparator.reverseOrder());
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(3);
    }
}
