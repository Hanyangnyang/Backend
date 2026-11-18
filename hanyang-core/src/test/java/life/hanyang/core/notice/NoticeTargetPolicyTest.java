package life.hanyang.core.notice;

import life.hanyang.core.campusmap.domain.*;
import life.hanyang.core.campusmap.repository.CampusBuildingRepository;
import life.hanyang.core.notice.domain.Notice;
import life.hanyang.core.notice.dto.NoticeDto.TargetInput;
import life.hanyang.core.notice.service.NoticeTargetPolicy;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class NoticeTargetPolicyTest {
    private final CampusBuildingRepository buildings = mock(CampusBuildingRepository.class);
    private final NoticeTargetPolicy policy = new NoticeTargetPolicy(buildings);

    @Test
    void simplifiedCatalogIncludesAllPagesAndShuttleDepartureChipsWithoutBuildingQuery() {
        var options = policy.options();
        assertThat(options).extracting(o -> o.page()).containsExactly(
                "menu", "transport", "news", "campusMap", "gym", "playlist", "clubs", "instagram");
        assertThat(options.get(3).conditions()).isEmpty();
        assertThat(options.get(1).conditions()).extracting(c -> c.key()).containsExactly("transportType", "shuttleStop");
        for (String stop : List.of("기숙사", "셔틀콕", "한대앞", "셔틀콕 건너편", "예술인", "중앙역")) {
            policy.validateTargets(List.of(new TargetInput("transport", Map.of(
                    "transportType", List.of("schoolShuttle"), "shuttleStop", List.of(stop)))));
        }
        for (String page : List.of("news", "gym", "playlist", "clubs", "instagram")) {
            policy.validateTargets(List.of(new TargetInput(page, Map.of())));
            policy.validateContext(page, Map.of());
        }
        assertThatThrownBy(() -> policy.validateTargets(List.of(new TargetInput("transport", Map.of(
                "transportType", List.of("cityBus"), "shuttleStop", List.of("중앙역"))))))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(buildings);
    }

    @Test
    void allowsPageWideAndKnownMultiValueConditionsWithoutDatabaseReads() {
        policy.validateTargets(List.of(new TargetInput("transport", Map.of()),
                new TargetInput("menu", Map.of("cafeteriaCode", List.of("re11", "re12")))));
        policy.validateContext("transport", Map.of("transportType", "cityBus", "stationId", "jungang"));
        verifyNoInteractions(buildings);
    }

    @Test
    void readContextAcceptsUnknownOrRemovedSelectionsWithoutCatalogLookup() {
        policy.validateContext("transport", Map.of("transportType", "cityBus", "stationId", "new-station"));
        policy.validateContext("campusMap", Map.of("category", "building", "buildingId", "removed-building"));
        policy.validateContext("menu", Map.of("newChip", "new-value"));
        verifyNoInteractions(buildings);
        assertThatThrownBy(() -> policy.validateContext("menu", Map.of("", "value")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> policy.validateContext("menu", Map.of("chip", " ")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> policy.validateContext("menu", Map.of("chip", "a".repeat(101))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsUnknownPagesKeysValuesEmptyArraysAndInvalidRouteCombination() {
        List<TargetInput> invalid = List.of(
                new TargetInput("unknown", Map.of()),
                new TargetInput("menu", Map.of("typo", List.of("re11"))),
                new TargetInput("menu", Map.of("cafeteriaCode", List.of("unknown"))),
                new TargetInput("menu", Map.of("cafeteriaCode", List.of())),
                new TargetInput("menu", Map.of("cafeteriaCode", List.of("re11", "re11"))),
                new TargetInput("transport", Map.of("transportType", List.of("cityBus"), "shuttleRoute", List.of("CENTRAL"))));
        for (TargetInput target : invalid) {
            assertThatThrownBy(() -> policy.validateTargets(List.of(target))).isInstanceOf(IllegalArgumentException.class);
        }
        assertThatThrownBy(() -> policy.validateTargets(List.of())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> policy.validateTargets(List.of(new TargetInput("menu", Map.of()),
                new TargetInput("menu", Map.of())))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void validatesBuildingCategoryAndCampusAgainstExistingDataInOneRead() {
        var building = new CampusBuilding("b1", "1", "건물", null, Campus.ANSAN,
                new Coordinates(null, null), null, List.of(), List.of(), List.of(), List.of());
        when(buildings.findAll()).thenReturn(List.of(building));
        policy.validateTargets(List.of(new TargetInput("campusMap", Map.of(
                "category", List.of("building"), "buildingId", List.of("b1"), "campus", List.of("ANSAN")))));
        verify(buildings, times(1)).findAll();
        assertThatThrownBy(() -> policy.validateTargets(List.of(new TargetInput("campusMap", Map.of(
                "category", List.of("building"), "buildingId", List.of("b1"), "campus", List.of("SEOUL"))))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> policy.validateTargets(List.of(new TargetInput("campusMap", Map.of(
                "category", List.of("parkingLot"), "buildingId", List.of("b1"))))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsBlankContentAndInvalidPublicationWindow() {
        Instant now = Instant.parse("2026-11-17T00:00:00Z");
        assertThatThrownBy(() -> new Notice(" \n ", true, now, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Notice("공지", true, now, now)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Notice("공지", true, now, now.minusSeconds(1))).isInstanceOf(IllegalArgumentException.class);
        assertThat(new Notice(" 공지 ", true, now, null).getContent()).isEqualTo("공지");
    }
}
