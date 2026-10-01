package life.hanyang.core.campusmap;

import jakarta.validation.Validator;
import life.hanyang.core.campusmap.domain.*;
import life.hanyang.core.campusmap.dto.CampusMapDto.*;
import life.hanyang.core.campusmap.repository.*;
import life.hanyang.core.campusmap.service.CampusMapService;
import life.hanyang.core.global.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import java.util.List;
import java.util.stream.IntStream;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.mock.mockito.SpyBean;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.doReturn;
import static org.assertj.core.api.Assertions.*;

@DataJpaTest(showSql = false, properties = {
        "spring.datasource.url=jdbc:h2:mem:campus-map;MODE=PostgreSQL;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.properties.hibernate.generate_statistics=true",
        "spring.jpa.properties.hibernate.session.events.log=false",
        "spring.sql.init.mode=always",
        "spring.sql.init.schema-locations=file:../database/migrations/20261001_create_campus_map.sql,file:../database/migrations/20261001_create_campus_parking_lots.sql"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = CampusMapPersistenceTest.Config.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class CampusMapPersistenceTest {
    @Configuration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = CampusBuilding.class)
    @EnableJpaRepositories(basePackageClasses = CampusBuildingRepository.class)
    @Import(CampusMapService.class)
    static class Config {
        @Bean Validator validator() { return new LocalValidatorFactoryBean(); }
    }

    @Autowired CampusMapService service;
    @Autowired CampusBuildingRepository buildings;
    @Autowired OpenSpaceRepository spaces;
    @SpyBean SmokingAreaRepository smokingAreas;
    @SpyBean ParkingLotRepository parkingLots;
    @Autowired EntityManagerFactory entityManagerFactory;

    @BeforeEach
    void clear() {
        buildings.deleteAll();
        smokingAreas.deleteAll();
        parkingLots.deleteAll();
    }

    private BuildingRequest building(String id, String number, List<OpenSpaceRequest> spaces) {
        return new BuildingRequest(id, number, "본관", "Administration", List.of("본부", "본관별칭"),
                Campus.ANSAN, new Coordinates(null, null), "착공중", List.of("대학본부"), spaces,
                List.of("카페", "식당"), List.of("https://example.com/1.png", "https://example.com/2.png"));
    }
    private OpenSpaceRequest space(String id) { return new OpenSpaceRequest(id, "열람실", null, null); }

    @Test
    void importsBuildingsAndSpacesPreservingCollectionsAndNullCoordinates() {
        var result = service.importBuildings(List.of(building("building-101", "101", List.of(space("openspace-01")))));
        assertThat(result).isEqualTo(new ImportResult(1, 1, 0));
        var response = service.getBuilding("building-101");
        assertThat(response.aliases()).containsExactly("본부", "본관별칭");
        assertThat(response.facilities()).containsExactly("카페", "식당");
        assertThat(response.imageUrl()).containsExactly("https://example.com/1.png", "https://example.com/2.png");
        assertThat(response.coordinates()).isNotNull();
        assertThat(response.coordinates().getLatitude()).isNull();
        assertThat(response.openSpaces()).extracting(OpenSpaceResponse::id).containsExactly("openspace-01");
        assertThat(service.getBuildings(Campus.SEOUL)).isEmpty();
    }

    @Test
    void rollsBackAllBuildingsAndNestedSpacesOnDatabaseConflict() {
        assertThatThrownBy(() -> service.importBuildings(List.of(
                building("building-101", "101", List.of(space("openspace-01"))),
                building("building-other", "101", List.of(space("openspace-02"))))))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(buildings.count()).isZero();
        assertThat(spaces.count()).isZero();
    }

    @Test
    void rejectsDuplicateNestedIdsAndInvalidCoordinatesBeforeWriting() {
        assertThatThrownBy(() -> service.importBuildings(List.of(
                building("building-101", "101", List.of(space("openspace-01"))),
                building("building-102", "102", List.of(space("openspace-01"))))))
                .isInstanceOf(BusinessException.class);
        var invalid = new BuildingRequest("bad", "103", "건물", null, null, Campus.ANSAN,
                new Coordinates(37.0, null), null, null, null, null, null);
        assertThatThrownBy(() -> service.importBuildings(List.of(building("good", "101", List.of()), invalid)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(buildings.count()).isZero();
    }

    @Test
    void managesSpacesIndividuallyAndThroughBuildingReplacementAndCascadesDeletion() {
        service.createBuilding(building("building-101", "101", List.of(space("openspace-01"))));
        service.createOpenSpace("building-101", space("openspace-02"));
        service.updateOpenSpace("openspace-02", new OpenSpaceRequest("openspace-02", "스터디룸", "2층", "계단 옆"));
        assertThat(service.getBuilding("building-101").openSpaces()).hasSize(2);
        service.updateBuilding("building-101", building("building-101", "101", List.of(
                new OpenSpaceRequest("openspace-02", "새 이름", "3층", null), space("openspace-03"))));
        assertThat(spaces.existsById("openspace-01")).isFalse();
        assertThat(service.getBuilding("building-101").openSpaces()).extracting(OpenSpaceResponse::name)
                .containsExactly("새 이름", "열람실");
        service.deleteOpenSpace("openspace-02");
        assertThat(spaces.existsById("openspace-02")).isFalse();
        service.deleteBuilding("building-101");
        assertThat(spaces.count()).isZero();
    }

    @Test
    void existingIdsAreNeverOverwrittenAndSpacesCannotBeClaimedByAnotherBuilding() {
        service.createBuilding(building("building-101", "101", List.of(space("openspace-01"))));
        service.createBuilding(building("building-102", "102", List.of()));
        assertThatThrownBy(() -> service.createBuilding(building("building-101", "999", List.of())))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.updateBuilding("building-102",
                building("building-102", "102", List.of(space("openspace-01")))))
                .isInstanceOf(BusinessException.class);
        assertThat(service.getBuilding("building-101").buildingNumber()).isEqualTo("101");
        assertThat(service.getBuilding("building-102").openSpaces()).isEmpty();
    }

    @Test
    void importsSmokingAreasAndRejectsInvalidLaterItemsAtomically() {
        var valid = new SmokingAreaRequest("ansan-smoking-01", "흡연부스", SmokingAreaType.BOOTH, Campus.ANSAN,
                new Coordinates(37.3002924187283, 126.83539445133155), true, "설명", List.of());
        var invalid = new SmokingAreaRequest("ansan-smoking-02", "흡연장", SmokingAreaType.AREA, Campus.ANSAN,
                new Coordinates(91.0, 126.0), true, null, null);
        assertThatThrownBy(() -> service.importSmokingAreas(List.of(valid, invalid))).isInstanceOf(IllegalArgumentException.class);
        assertThat(smokingAreas.count()).isZero();
        assertThat(service.importSmokingAreas(List.of(valid))).isEqualTo(new ImportResult(0, 0, 1));
        assertThat(service.getSmokingAreas(Campus.ANSAN).get(0).coordinates().getLatitude()).isEqualTo(37.3002924187283);
        assertThatThrownBy(() -> service.importSmokingAreas(List.of(valid))).isInstanceOf(BusinessException.class);
    }
    private ParkingLotRequest parking(String id, Integer capacity) {
        return new ParkingLotRequest(id, "제1주차장", Campus.ANSAN,
                new Coordinates(37.29937450655232, 126.83785523939314), capacity,
                "경기 안산시 상록구 학사4길 1", "정문 근처", List.of("https://example.com/1.png", "https://example.com/2.png"));
    }

    @Test
    void parkingImportAndCrudPreserveJsonFieldsAndImageOrder() {
        assertThat(service.importParkingLots(List.of(parking("ansan-parking-01", 143))))
                .isEqualTo(new ParkingImportResult(1));
        var result = service.getParkingLot("ansan-parking-01");
        assertThat(result.capacity()).isEqualTo(143);
        assertThat(result.address()).isEqualTo("경기 안산시 상록구 학사4길 1");
        assertThat(result.coordinates().getLongitude()).isEqualTo(126.83785523939314);
        assertThat(result.imageUrl()).containsExactly("https://example.com/1.png", "https://example.com/2.png");
        assertThat(service.getParkingLots(Campus.SEOUL)).isEmpty();
        service.createParkingLot(parking("ansan-parking-02", null));
        assertThat(service.getParkingLots(Campus.ANSAN)).hasSize(2);
        service.updateParkingLot("ansan-parking-01", parking("ansan-parking-01", 0));
        assertThat(service.getParkingLot("ansan-parking-01").capacity()).isZero();
        service.deleteParkingLot("ansan-parking-01");
        assertThat(parkingLots.existsById("ansan-parking-01")).isFalse();
    }

    @Test
    void parkingImportRejectsNegativeCapacityAndDuplicatesWithoutPartialWrites() {
        assertThatThrownBy(() -> service.importParkingLots(List.of(parking("one", 143), parking("two", -1))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(parkingLots.count()).isZero();
        assertThatThrownBy(() -> service.importParkingLots(List.of(parking("one", 143), parking("one", 12))))
                .isInstanceOf(BusinessException.class);
        assertThat(parkingLots.count()).isZero();
        service.createParkingLot(parking("existing", 143));
        assertThatThrownBy(() -> service.importParkingLots(List.of(parking("new", 1), parking("existing", 99))))
                .isInstanceOf(BusinessException.class);
        assertThat(parkingLots.count()).isEqualTo(1);
        assertThat(service.getParkingLot("existing").capacity()).isEqualTo(143);
        assertThatThrownBy(() -> service.updateParkingLot("existing", parking("other-id", 10)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private Statistics resetStatistics() {
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        return statistics;
    }

    @ParameterizedTest
    @CsvSource({"50,true", "50,false", "205,true", "205,false"})
    void buildingListLoadsCollectionsInBatchesInsteadOfPerBuilding(int count, boolean filterCampus) {
        service.importBuildings(IntStream.range(0, count)
                .mapToObj(i -> building("building-" + i, "" + i, List.of(space("space-" + i)))).toList());
        Statistics statistics = resetStatistics();
        var result = service.getBuildings(filterCampus ? Campus.ANSAN : null);
        assertThat(result).hasSize(count);
        assertThat(result).allSatisfy(r -> {
            assertThat(r.openSpaces()).hasSize(1);
            assertThat(r.aliases()).hasSize(2);
            assertThat(r.primaryColleges()).hasSize(1);
            assertThat(r.facilities()).hasSize(2);
            assertThat(r.imageUrl()).hasSize(2);
        });
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(1 + 5L * ((count + 99) / 100));
    }

    @ParameterizedTest
    @CsvSource({"50,true", "50,false", "205,true", "205,false"})
    void smokingAndParkingListsBatchImageQueries(int count, boolean filterCampus) {
        service.importSmokingAreas(IntStream.range(0, count).mapToObj(i -> new SmokingAreaRequest(
                "smoking-" + i, "흡연장", SmokingAreaType.AREA, Campus.ANSAN, new Coordinates(37.0, 126.0),
                true, null, List.of("https://example.com/image.png"))).toList());
        service.importParkingLots(IntStream.range(0, count).mapToObj(i -> parking("parking-" + i, 143)).toList());
        Statistics statistics = resetStatistics();
        var smoking = service.getSmokingAreas(filterCampus ? Campus.ANSAN : null);
        assertThat(smoking).hasSize(count).allSatisfy(r -> assertThat(r.imageUrl()).hasSize(1));
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(1 + (count + 99) / 100);
        statistics.clear();
        var parking = service.getParkingLots(filterCampus ? Campus.ANSAN : null);
        assertThat(parking).hasSize(count).allSatisfy(r -> assertThat(r.imageUrl()).hasSize(2));
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(1 + (count + 99) / 100);
    }

    @ParameterizedTest
    @ValueSource(ints = {20, 501})
    void importDuplicateChecksUseBoundedIdQueries(int count) {
        Statistics statistics = resetStatistics();
        service.importBuildings(IntStream.range(0, count).mapToObj(i -> new BuildingRequest(
                "building-" + i, "" + i, "건물", null, null, Campus.ANSAN, new Coordinates(null, null),
                null, null, List.of(space("space-" + i)), null, null)).toList());
        long batches = (count + 499) / 500;
        assertThat(statistics.getQueryExecutionCount()).isEqualTo(2 * batches);
        statistics.clear();
        service.importSmokingAreas(IntStream.range(0, count).mapToObj(i -> new SmokingAreaRequest(
                "smoking-" + i, "흡연장", SmokingAreaType.AREA, Campus.ANSAN,
                new Coordinates(37.0, 126.0), true, null, null)).toList());
        assertThat(statistics.getQueryExecutionCount()).isEqualTo(batches);
        statistics.clear();
        service.importParkingLots(IntStream.range(0, count).mapToObj(i -> parking("parking-" + i, null)).toList());
        assertThat(statistics.getQueryExecutionCount()).isEqualTo(batches);
    }

    @Test
    void omittedSpacesArePreservedButExplicitEmptyArrayDeletesThem() {
        service.createBuilding(building("building-101", "101", List.of(space("space-01"))));
        service.updateBuilding("building-101", building("building-101", "101", null));
        assertThat(service.getBuilding("building-101").openSpaces()).extracting(OpenSpaceResponse::id)
                .containsExactly("space-01");
        assertThat(spaces.existsById("space-01")).isTrue();
        service.updateBuilding("building-101", building("building-101", "101", List.of()));
        assertThat(service.getBuilding("building-101").openSpaces()).isEmpty();
        assertThat(spaces.existsById("space-01")).isFalse();
    }

    @Test
    void failedBuildingUpdateRestoresRemovedSpacesAndOtherFields() {
        service.createBuilding(building("building-101", "101", List.of(space("space-01"))));
        service.createBuilding(building("building-102", "102", List.of()));
        assertThatThrownBy(() -> service.updateBuilding("building-101", building("building-101", "102", List.of())))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(service.getBuilding("building-101").buildingNumber()).isEqualTo("101");
        assertThat(service.getBuilding("building-101").openSpaces()).extracting(OpenSpaceResponse::id)
                .containsExactly("space-01");
    }

    @Test
    void parkingDatabaseConflictAfterPrecheckRollsBackAllNewRowsAndImages() {
        service.createParkingLot(parking("existing", 143));
        doReturn(List.of()).when(parkingLots).findExistingIds(anyCollection());
        assertThatThrownBy(() -> service.importParkingLots(List.of(parking("new", 12), parking("existing", 99))))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(parkingLots.count()).isEqualTo(1);
        assertThat(service.getParkingLot("existing").capacity()).isEqualTo(143);
        assertThat(service.getParkingLot("existing").imageUrl()).hasSize(2);
    }

    @Test
    void smokingDatabaseConflictAfterPrecheckRollsBackAllNewRowsAndImages() {
        var existing = new SmokingAreaRequest("existing", "흡연장", SmokingAreaType.BOOTH, Campus.ANSAN,
                new Coordinates(37.0, 126.0), true, null, List.of("https://example.com/image.png"));
        service.createSmokingArea(existing);
        doReturn(List.of()).when(smokingAreas).findExistingIds(anyCollection());
        var newArea = new SmokingAreaRequest("new", "흡연장", SmokingAreaType.AREA, Campus.ANSAN,
                new Coordinates(37.0, 126.0), false, null, existing.imageUrl());
        assertThatThrownBy(() -> service.importSmokingAreas(List.of(newArea, existing)))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(smokingAreas.count()).isEqualTo(1);
        assertThat(service.getSmokingArea("existing").type()).isEqualTo(SmokingAreaType.BOOTH);
        assertThat(service.getSmokingArea("existing").imageUrl()).hasSize(1);
    }

}
