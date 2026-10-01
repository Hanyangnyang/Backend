package life.hanyang.core.partnership;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.persistence.EntityManagerFactory;
import jakarta.validation.Validator;
import life.hanyang.core.partnership.domain.Merchant;
import life.hanyang.core.partnership.domain.MerchantCategory;
import life.hanyang.core.partnership.dto.*;
import life.hanyang.core.partnership.repository.MerchantRepository;
import life.hanyang.core.partnership.service.MerchantService;
import life.hanyang.core.partnership.service.PartnershipService;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.FileSystemResource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import javax.sql.DataSource;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;
import static org.assertj.core.api.Assertions.*;

@DataJpaTest(showSql = false, properties = {
        "spring.datasource.url=jdbc:h2:mem:merchant-menus;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.generate_statistics=true",
        "spring.jpa.properties.hibernate.session.events.log=false"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = MerchantRepresentativeMenusTest.Config.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MerchantRepresentativeMenusTest {
    @Configuration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = Merchant.class)
    @EnableJpaRepositories(basePackageClasses = MerchantRepository.class)
    @Import({MerchantService.class, PartnershipService.class})
    static class Config {
        @Bean Validator validator() { return new LocalValidatorFactoryBean(); }
    }

    @Autowired MerchantService merchantService;
    @Autowired PartnershipService partnershipService;
    @Autowired MerchantRepository merchants;
    @Autowired EntityManagerFactory entityManagerFactory;
    @Autowired JdbcTemplate jdbc;
    @Autowired DataSource dataSource;
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

    @BeforeAll
    void applyActualMenuMigrationToTestDatabase() {
        jdbc.execute("DROP TABLE merchant_representative_menus");
        new ResourceDatabasePopulator(new FileSystemResource(
                "../database/migrations/20261001_add_merchant_representative_menus.sql")).execute(dataSource);
    }

    @BeforeEach
    void clearData() { merchants.deleteAll(); }

    private MerchantRequest request(String name, List<String> menus) {
        return new MerchantRequest(name, MerchantCategory.RESTAURANT, true, "🍚", 37.0, 126.0,
                "안산", null, menus);
    }

    private MerchantCreateWithPartnershipsRequest dump(String name, List<String> menus) {
        return new MerchantCreateWithPartnershipsRequest(name, MerchantCategory.RESTAURANT,
                true, null, "🍚", null, List.of(), menus);
    }

    private Statistics resetStatistics() {
        var statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        return statistics;
    }

    @ParameterizedTest
    @ValueSource(ints = {50, 205})
    void merchantAndAvailablePartnershipListsBatchMenuQueries(int count) {
        List<String> menus = List.of("알밥", "특알밥", "돈까스", "우동", "냉모밀", "덮밥");
        merchantService.createMerchants(IntStream.range(0, count).mapToObj(i -> request("식당" + i, menus)).toList());
        var statistics = resetStatistics();
        var merchantResponses = merchantService.getAllMerchants();
        assertThat(merchantResponses).hasSize(count).allSatisfy(r -> assertThat(r.representativeMenus()).containsExactlyElementsOf(menus));
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(1 + (count + 99) / 100);
        statistics.clear();
        var partnerships = partnershipService.getAvailablePartnerships();
        assertThat(partnerships).hasSize(count).allSatisfy(r -> assertThat(r.getRepresentativeMenus()).containsExactlyElementsOf(menus));
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(1 + (count + 99) / 100);
        statistics.clear();
        var exported = partnershipService.exportMerchants();
        assertThat(exported).hasSize(count).allSatisfy(r -> assertThat(r.representativeMenus()).containsExactlyElementsOf(menus));
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(1 + (count + 99) / 100);
        assertThat(statistics.getEntityInsertCount()).isZero();
        assertThat(statistics.getEntityUpdateCount()).isZero();
    }

    @Test
    void updatePreservesNullReplacesArrayAndClearsExplicitEmptyArray() throws Exception {
        merchantService.createMerchants(List.of(request("식당", List.of("알밥", "우동"))));
        Long id = merchantService.getAllMerchants().get(0).merchantId();
        var omitted = mapper.readValue("""
                {"storeName":"식당","category":"food","emoji":"🍚","isActive":true}
                """, MerchantRequest.class);
        merchantService.updateMerchant(id, omitted);
        assertThat(merchantService.getAllMerchants().get(0).representativeMenus()).containsExactly("알밥", "우동");
        merchantService.updateMerchant(id, request("식당", null));
        assertThat(merchantService.getAllMerchants().get(0).representativeMenus()).containsExactly("알밥", "우동");
        merchantService.updateMerchant(id, request("식당", List.of("돈까스")));
        assertThat(merchantService.getAllMerchants().get(0).representativeMenus()).containsExactly("돈까스");
        merchantService.updateMerchant(id, request("식당", List.of()));
        assertThat(merchantService.getAllMerchants().get(0).representativeMenus()).isEmpty();
    }

    @Test
    void rejectsInvalidMenuElementsBeforeSavingAnyMerchantOrChangingExistingMenus() {
        for (List<String> invalid : List.of(List.of(""), List.of("   "), List.of("가".repeat(101)), Arrays.asList("알밥", null))) {
            assertThatThrownBy(() -> merchantService.createMerchants(List.of(request("정상", List.of("알밥")), request("오류", invalid))))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThat(merchants.count()).isZero();
        }
        merchantService.createMerchants(List.of(request("기존", List.of("알밥"))));
        Long id = merchantService.getAllMerchants().get(0).merchantId();
        assertThatThrownBy(() -> merchantService.updateMerchant(id, request("수정", List.of(" "))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(merchantService.getAllMerchants().get(0).storeName()).isEqualTo("기존");
        assertThat(merchantService.getAllMerchants().get(0).representativeMenus()).containsExactly("알밥");
    }

    @Test
    void acceptsAbsentMenusAndUnrestrictedMenuCount() {
        merchantService.createMerchants(List.of(request("정보없음", null), request("많은메뉴",
                IntStream.range(0, 20).mapToObj(i -> "메뉴" + i).toList())));
        assertThat(merchantService.getAllMerchants()).anySatisfy(r -> assertThat(r.representativeMenus()).isEmpty())
                .anySatisfy(r -> assertThat(r.representativeMenus()).hasSize(20));
        assertThat(mapper.valueToTree(merchantService.getAllMerchants().get(0)).has("representativeMenus")).isTrue();
        assertThat(mapper.valueToTree(partnershipService.getAvailablePartnerships().get(0)).has("representativeMenus")).isTrue();
    }

    @Test
    void dumpUsesSnakeCaseAndValidatesBeforeResettingExistingData() throws Exception {
        merchantService.createMerchants(List.of(request("기존", List.of("알밥"))));
        var parsed = mapper.readValue("""
                {"name":"새식당","category":"food","emoji":"🍚","representative_menus":["돈까스","우동"]}
                """, MerchantCreateWithPartnershipsRequest.class);
        assertThat(parsed.representativeMenus()).containsExactly("돈까스", "우동");
        assertThat(mapper.valueToTree(parsed).has("representative_menus")).isTrue();
        assertThatThrownBy(() -> partnershipService.resetAndLoadPartnerships(List.of(parsed, dump("오류", List.of(" ")))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(merchantService.getAllMerchants()).singleElement().satisfies(r -> {
            assertThat(r.storeName()).isEqualTo("기존");
            assertThat(r.representativeMenus()).containsExactly("알밥");
        });
        partnershipService.resetAndLoadPartnerships(List.of(parsed));
        assertThat(merchantService.getAllMerchants()).singleElement().satisfies(r -> {
            assertThat(r.storeName()).isEqualTo("새식당");
            assertThat(r.representativeMenus()).containsExactly("돈까스", "우동");
        });
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM merchant_representative_menus", Long.class)).isEqualTo(2L);
    }

    @Test
    void deletionCascadesMenusAndEmptyResetIsRejected() {
        merchantService.createMerchants(List.of(request("식당", List.of("알밥"))));
        assertThatThrownBy(() -> partnershipService.resetAndLoadPartnerships(List.of())).isInstanceOf(IllegalArgumentException.class);
        assertThat(merchants.count()).isEqualTo(1);
        merchantService.deleteMerchants(List.of(merchantService.getAllMerchants().get(0).merchantId()));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM merchant_representative_menus", Long.class)).isZero();
    }
    @Test
    void exportIncludesInactiveAndExpiredDataAndRoundTripsAllEditableFields() throws Exception {
        var input = mapper.readValue("""
                [
                  {"name":"비활성 식당","category":"food","is_active":false,"emoji":"🍚",
                   "location":{"latitude":37.123,"longitude":126.456,"full_address":"안산시 주소"},
                   "kakao_place_id":"12345","representative_menus":["알밥","우동"],
                   "partnerships":[
                     {"college_name":"공학대학","benefit":"과거 혜택","conditions":"학생증 필요",
                      "source_url":"https://example.com/source","photo_order":7,
                      "period":{"start_date":"2020-01-01","end_date":"2020-12-31","is_active":false}},
                     {"college_name":"학생회","benefit":"할인","conditions":null,"source_url":null,"photo_order":null,
                      "period":{"start_date":"2026-01-01","end_date":"2027-12-31","is_active":true}}
                   ]},
                  {"name":"제휴 없는 카페","category":"cafe","is_active":true,"emoji":"☕",
                   "location":{"latitude":null,"longitude":null,"full_address":null},
                   "kakao_place_id":null,"representative_menus":[],"partnerships":[]}
                ]
                """, new TypeReference<List<MerchantCreateWithPartnershipsRequest>>() {});
        partnershipService.resetAndLoadPartnerships(input);
        var exported = partnershipService.exportMerchants();
        assertThat(exported).hasSize(2);
        assertThat(exported.get(0).isActive()).isFalse();
        assertThat(exported.get(0).partnerships()).hasSize(2);
        String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(exported);
        var tree = mapper.readTree(json);
        assertThat(tree.get(0).get("category").asText()).isEqualTo("food");
        assertThat(tree.get(0).get("representative_menus").get(0).asText()).isEqualTo("알밥");
        assertThat(tree.get(0).get("partnerships").get(0).get("college_name").asText()).isEqualTo("공학대학");
        assertThat(tree.get(0).get("partnerships").get(0).get("photo_order").asInt()).isEqualTo(7);
        assertThat(tree.get(0).get("partnerships").get(0).get("period").get("start_date").asText()).isEqualTo("2020-01-01");
        assertThat(tree.get(0).has("merchant_id")).isFalse();
        var reloaded = mapper.readValue(json, new TypeReference<List<MerchantCreateWithPartnershipsRequest>>() {});
        partnershipService.resetAndLoadPartnerships(reloaded);
        assertThat(mapper.readTree(mapper.writeValueAsString(partnershipService.exportMerchants()))).isEqualTo(tree);
    }

    @Test
    void exportOfEmptyDatabaseIsAnEmptyArray() throws Exception {
        var exported = partnershipService.exportMerchants();
        assertThat(exported).isEmpty();
        assertThat(mapper.writeValueAsString(exported)).isEqualTo("[]");
    }

}
