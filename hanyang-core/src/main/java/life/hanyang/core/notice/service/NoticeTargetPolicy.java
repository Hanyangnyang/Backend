package life.hanyang.core.notice.service;

import life.hanyang.core.campusmap.domain.CampusBuilding;
import life.hanyang.core.campusmap.repository.CampusBuildingRepository;
import life.hanyang.core.menu.entity.CafeteriaCode;
import life.hanyang.core.notice.dto.NoticeDto.*;
import life.hanyang.core.shuttle.domain.ShuttleRoute;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
@RequiredArgsConstructor
public class NoticeTargetPolicy {
    private final CampusBuildingRepository buildings;
    private static final Set<String> PAGES = Set.of("menu", "transport", "news", "campusMap", "gym", "playlist", "clubs", "instagram");

    public void validatePage(String page) {
        if (page == null || !PAGES.contains(page)) throw new IllegalArgumentException("지원하지 않는 공지 페이지입니다.");
    }

    public List<PageOption> options() {
        return List.of(pageOptions("menu", List.of()),
                new PageOption("transport", "셔틀-지하철", pageOptions("transport", List.of()).conditions().stream()
                        .filter(c -> Set.of("transportType", "shuttleStop").contains(c.key())).toList()),
                pageOptions("news", List.of()), new PageOption("campusMap", "캠퍼스맵", List.of()),
                pageOptions("gym", List.of()), pageOptions("playlist", List.of()),
                pageOptions("clubs", List.of()), pageOptions("instagram", List.of()));
    }

    public void validateTargets(List<TargetInput> targets) {
        if (targets == null || targets.isEmpty() || targets.size() > 100) {
            throw new IllegalArgumentException("노출 대상은 1~100개가 필요합니다.");
        }
        boolean needsBuildings = targets.stream().filter(Objects::nonNull)
                .anyMatch(t -> t.conditions() != null && t.conditions().containsKey("buildingId"));
        List<CampusBuilding> knownBuildings = needsBuildings ? buildings.findAll() : List.of();
        Set<TargetInput> unique = new HashSet<>();
        for (TargetInput target : targets) {
            if (target == null) throw new IllegalArgumentException("노출 대상은 null일 수 없습니다.");
            validatePage(target.page());
            validateConditions(target.page(), target.conditions(), knownBuildings);
            if (!unique.add(target)) throw new IllegalArgumentException("중복된 노출 대상입니다.");
        }
    }

    public void validateContext(String page, Map<String, String> context) {
        validatePage(page);
        if (context == null || context.size() > 10) throw new IllegalArgumentException("잘못된 공지 선택 상태입니다.");
        // Read-side context can outlive catalog entries or arrive from a newer frontend.
        // Preserve unknown values: the database must still evaluate every stored condition.
        context.forEach((key, value) -> {
            if (key == null || !key.matches("[a-zA-Z][a-zA-Z0-9]{0,49}")
                    || value == null || value.isBlank() || value.length() > 100) {
                throw new IllegalArgumentException("공지 선택 상태의 형식 또는 길이가 올바르지 않습니다.");
            }
        });
    }

    private void validateConditions(String page, Map<String, List<String>> conditions, List<CampusBuilding> knownBuildings) {
        if (conditions == null || conditions.size() > 10) throw new IllegalArgumentException("노출 조건은 객체여야 합니다.");
        Map<String, Set<String>> allowed = new HashMap<>();
        pageOptions(page, knownBuildings).conditions().forEach(c -> {
            Set<String> values = new HashSet<>();
            c.values().forEach(v -> values.add(v.value()));
            allowed.put(c.key(), values);
        });
        conditions.forEach((key, values) -> {
            if (!allowed.containsKey(key) || values == null || values.isEmpty() || values.size() > 100
                    || values.stream().anyMatch(v -> v == null || !allowed.get(key).contains(v))
                    || new HashSet<>(values).size() != values.size()) {
                throw new IllegalArgumentException("허용되지 않는 공지 조건 또는 값입니다: " + key);
            }
        });
        if (conditions.containsKey("shuttleStop") && !List.of("schoolShuttle").equals(conditions.get("transportType"))) {
            throw new IllegalArgumentException("출발지 칩은 학교 셔틀을 선택해야 합니다.");
        }
        if (conditions.containsKey("shuttleRoute")
                && !List.of("schoolShuttle").equals(conditions.get("transportType"))) {
            throw new IllegalArgumentException("셔틀 노선 조건은 학교 셔틀을 선택해야 합니다.");
        }
        if (conditions.containsKey("buildingId")) {
            if (!List.of("building").equals(conditions.get("category"))) {
                throw new IllegalArgumentException("건물 조건은 지도 분류가 건물이어야 합니다.");
            }
            if (conditions.containsKey("campus")) {
                boolean incompatible = knownBuildings.stream()
                        .filter(b -> conditions.get("buildingId").contains(b.getId()))
                        .anyMatch(b -> !conditions.get("campus").contains(b.getCampus().name()));
                if (incompatible) throw new IllegalArgumentException("건물과 캠퍼스 조건이 일치하지 않습니다.");
            }
        }
    }

    private PageOption pageOptions(String page, List<CampusBuilding> knownBuildings) {
        return switch (page) {
            case "menu" -> new PageOption(page, "학식", List.of(new ConditionOption("cafeteriaCode", "식당",
                    Arrays.stream(CafeteriaCode.values()).map(c -> new Option(c.getCode(), c.getDefaultName())).toList())));
            // Frontend contract: labels may change; persisted values must remain stable.
            case "transport" -> new PageOption(page, "교통", List.of(
                    new ConditionOption("transportType", "교통수단", List.of(
                            new Option("schoolShuttle", "학교 셔틀"), new Option("cityBus", "일반버스"))),
                    new ConditionOption("shuttleStop", "출발지", List.of("기숙사", "셔틀콕", "한대앞", "셔틀콕 건너편", "예술인", "중앙역").stream()
                            .map(stop -> new Option(stop, stop)).toList()),
                    new ConditionOption("stationId", "정류장", List.of(new Option("jungang", "중앙역"))),
                    new ConditionOption("shuttleRoute", "셔틀 노선", Arrays.stream(ShuttleRoute.values())
                            .map(r -> new Option(r.name(), r.name())).toList())));
            case "campusMap" -> new PageOption(page, "캠퍼스맵", List.of(
                    new ConditionOption("category", "지도 분류", List.of(new Option("building", "건물"),
                            new Option("smokingArea", "흡연구역"), new Option("parkingLot", "주차장"))),
                    new ConditionOption("campus", "캠퍼스", List.of(new Option("ANSAN", "ERICA"), new Option("SEOUL", "서울"))),
                    new ConditionOption("buildingId", "건물", knownBuildings.stream()
                            .sorted(Comparator.comparing(CampusBuilding::getId))
                            .map(b -> new Option(b.getId(), b.getName())).toList())));
            case "news" -> new PageOption(page, "소식", List.of());
            case "gym" -> new PageOption(page, "체대 헬스장", List.of());
            case "playlist" -> new PageOption(page, "에리카 플레이리스트", List.of());
            case "clubs" -> new PageOption(page, "중앙동아리", List.of());
            case "instagram" -> new PageOption(page, "학교 인스타그램", List.of());
            default -> throw new IllegalArgumentException("지원하지 않는 공지 페이지입니다.");
        };
    }
}
