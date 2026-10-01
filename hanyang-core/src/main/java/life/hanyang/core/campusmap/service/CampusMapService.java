package life.hanyang.core.campusmap.service;

import jakarta.validation.Validator;
import jakarta.persistence.EntityManager;
import life.hanyang.core.campusmap.domain.*;
import life.hanyang.core.campusmap.dto.CampusMapDto.*;
import life.hanyang.core.campusmap.repository.*;
import life.hanyang.core.global.exception.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CampusMapService {
    private static final int ID_QUERY_BATCH_SIZE = 500;

    private final CampusBuildingRepository buildings;
    private final OpenSpaceRepository spaces;
    private final SmokingAreaRepository smokingAreas;
    private final ParkingLotRepository parkingLots;
    private final Validator validator;
    private final EntityManager entityManager;

    public List<BuildingResponse> getBuildings(Campus campus) {
        return (campus == null ? buildings.findAll(org.springframework.data.domain.Sort.by("id"))
                : buildings.findAllByCampusOrderByIdAsc(campus)).stream().map(BuildingResponse::from).toList();
    }

    public List<SmokingAreaResponse> getSmokingAreas(Campus campus) {
        return (campus == null ? smokingAreas.findAll(org.springframework.data.domain.Sort.by("id"))
                : smokingAreas.findAllByCampusOrderByIdAsc(campus)).stream().map(SmokingAreaResponse::from).toList();
    }

    public BuildingResponse getBuilding(String id) { return BuildingResponse.from(building(id)); }
    public SmokingAreaResponse getSmokingArea(String id) { return SmokingAreaResponse.from(smokingArea(id)); }

    @Transactional
    public BuildingResponse createBuilding(BuildingRequest r) {
        importBuildings(List.of(r));
        return getBuilding(r.id());
    }

    @Transactional
    public ImportResult importBuildings(List<BuildingRequest> requests) {
        requireList(requests);
        Set<String> buildingIds = new HashSet<>();
        Set<String> spaceIds = new HashSet<>();
        for (BuildingRequest r : requests) {
            validate(r);
            unique(buildingIds, r.id());
            for (OpenSpaceRequest s : list(r.openSpaces())) {
                unique(spaceIds, s.id());
            }
        }
        rejectExistingIds(buildingIds, buildings::findExistingIds);
        rejectExistingIds(spaceIds, spaces::findExistingIds);
        for (BuildingRequest r : requests) {
            CampusBuilding b = new CampusBuilding(r.id(), r.buildingNumber(), r.name(), r.englishName(),
                    r.campus(), r.coordinates(), r.description(), r.aliases(), r.primaryColleges(), r.facilities(), r.imageUrl());
            for (OpenSpaceRequest s : list(r.openSpaces())) b.addOpenSpace(newSpace(s, b));
            entityManager.persist(b);
        }
        buildings.flush();
        return new ImportResult(requests.size(), spaceIds.size(), 0);
    }

    @Transactional
    public BuildingResponse updateBuilding(String id, BuildingRequest r) {
        validate(r); sameId(id, r.id());
        CampusBuilding b = building(id);
        if (r.openSpaces() != null) {
            synchronizeOpenSpaces(b, r.openSpaces());
        }
        b.update(r.buildingNumber(), r.name(), r.englishName(), r.campus(), r.coordinates(), r.description(),
                r.aliases(), r.primaryColleges(), r.facilities(), r.imageUrl());
        buildings.flush();
        return BuildingResponse.from(b);
    }

    private void synchronizeOpenSpaces(CampusBuilding b, List<OpenSpaceRequest> requests) {
        Map<String, OpenSpace> existing = b.getOpenSpaces().stream()
                .collect(Collectors.toMap(OpenSpace::getId, s -> s));
        Set<String> ids = new HashSet<>();
        for (OpenSpaceRequest s : requests) unique(ids, s.id());
        Set<String> newIds = new HashSet<>(ids);
        newIds.removeAll(existing.keySet());
        rejectExistingIds(newIds, spaces::findExistingIds);
        b.getOpenSpaces().removeIf(s -> !ids.contains(s.getId()));
        for (OpenSpaceRequest r : requests) {
            OpenSpace current = existing.get(r.id());
            if (current == null) b.addOpenSpace(newSpace(r, b));
            else current.update(r.name(), r.floor(), r.hint());
        }
    }

    @Transactional
    public OpenSpaceResponse createOpenSpace(String buildingId, OpenSpaceRequest r) {
        validate(r);
        CampusBuilding b = building(buildingId);
        if (spaces.existsById(r.id())) duplicate(r.id());
        OpenSpace s = newSpace(r, b);
        b.addOpenSpace(s);
        buildings.flush();
        return OpenSpaceResponse.from(s);
    }

    @Transactional
    public OpenSpaceResponse updateOpenSpace(String id, OpenSpaceRequest r) {
        validate(r); sameId(id, r.id());
        OpenSpace s = space(id);
        s.update(r.name(), r.floor(), r.hint());
        return OpenSpaceResponse.from(s);
    }

    @Transactional
    public SmokingAreaResponse createSmokingArea(SmokingAreaRequest r) {
        importSmokingAreas(List.of(r));
        return getSmokingArea(r.id());
    }

    @Transactional
    public ImportResult importSmokingAreas(List<SmokingAreaRequest> requests) {
        requireList(requests);
        Set<String> ids = new HashSet<>();
        for (SmokingAreaRequest r : requests) {
            validate(r); unique(ids, r.id());
        }
        rejectExistingIds(ids, smokingAreas::findExistingIds);
        for (SmokingAreaRequest r : requests) entityManager.persist(new SmokingArea(r.id(), r.name(), r.type(),
                r.campus(), r.coordinates(), r.hasAshtray(), r.description(), r.imageUrl()));
        smokingAreas.flush();
        return new ImportResult(0, 0, requests.size());
    }

    @Transactional
    public SmokingAreaResponse updateSmokingArea(String id, SmokingAreaRequest r) {
        validate(r); sameId(id, r.id());
        SmokingArea s = smokingArea(id);
        s.update(r.name(), r.type(), r.campus(), r.coordinates(), r.hasAshtray(), r.description(), r.imageUrl());
        return SmokingAreaResponse.from(s);
    }

    @Transactional
    public void deleteBuilding(String id) { buildings.delete(building(id)); }
    @Transactional
    public void deleteSmokingArea(String id) { smokingAreas.delete(smokingArea(id)); }
    @Transactional
    public void deleteOpenSpace(String id) {
        OpenSpace s = space(id);
        s.getBuilding().getOpenSpaces().remove(s);
    }

    public List<ParkingLotResponse> getParkingLots(Campus campus) {
        return (campus == null ? parkingLots.findAll(org.springframework.data.domain.Sort.by("id"))
                : parkingLots.findAllByCampusOrderByIdAsc(campus)).stream().map(ParkingLotResponse::from).toList();
    }

    public ParkingLotResponse getParkingLot(String id) { return ParkingLotResponse.from(parkingLot(id)); }

    @Transactional
    public ParkingLotResponse createParkingLot(ParkingLotRequest r) {
        importParkingLots(List.of(r));
        return getParkingLot(r.id());
    }

    @Transactional
    public ParkingImportResult importParkingLots(List<ParkingLotRequest> requests) {
        requireList(requests);
        Set<String> ids = new HashSet<>();
        for (ParkingLotRequest r : requests) {
            validate(r); unique(ids, r.id());
        }
        rejectExistingIds(ids, parkingLots::findExistingIds);
        for (ParkingLotRequest r : requests) entityManager.persist(new ParkingLot(r.id(), r.name(),
                r.campus(), r.coordinates(), r.capacity(), r.address(), r.description(), r.imageUrl()));
        parkingLots.flush();
        return new ParkingImportResult(requests.size());
    }

    @Transactional
    public ParkingLotResponse updateParkingLot(String id, ParkingLotRequest r) {
        validate(r); sameId(id, r.id());
        ParkingLot p = parkingLot(id);
        p.update(r.name(), r.campus(), r.coordinates(), r.capacity(), r.address(), r.description(), r.imageUrl());
        return ParkingLotResponse.from(p);
    }

    @Transactional
    public void deleteParkingLot(String id) { parkingLots.delete(parkingLot(id)); }

    private ParkingLot parkingLot(String id) {
        return parkingLots.findById(id).orElseThrow(() -> new EntityNotFoundException("주차장이 없습니다: " + id));
    }

    private CampusBuilding building(String id) { return buildings.findById(id).orElseThrow(() -> new EntityNotFoundException("건물이 없습니다: " + id)); }
    private OpenSpace space(String id) { return spaces.findById(id).orElseThrow(() -> new EntityNotFoundException("오픈스페이스가 없습니다: " + id)); }
    private SmokingArea smokingArea(String id) { return smokingAreas.findById(id).orElseThrow(() -> new EntityNotFoundException("흡연장이 없습니다: " + id)); }
    private OpenSpace newSpace(OpenSpaceRequest r, CampusBuilding b) { return new OpenSpace(r.id(), r.name(), r.floor(), r.hint(), b); }
    private static <T> List<T> list(List<T> values) { return values == null ? List.of() : values; }
    private static void requireList(List<?> values) {
        if (values == null || values.isEmpty()) throw new IllegalArgumentException("등록할 JSON 배열은 비어 있을 수 없습니다.");
    }
    private void validate(Object value) {
        if (value == null) throw new IllegalArgumentException("배열 항목은 null일 수 없습니다.");
        var errors = validator.validate(value);
        if (!errors.isEmpty()) throw new IllegalArgumentException(errors.stream()
                .map(e -> e.getPropertyPath() + ": " + e.getMessage()).sorted().collect(Collectors.joining(", ")));
    }
    private static void rejectExistingIds(Collection<String> ids,
                                          Function<List<String>, List<String>> query) {
        List<String> sortedIds = ids.stream().sorted().toList();
        for (int start = 0; start < sortedIds.size(); start += ID_QUERY_BATCH_SIZE) {
            List<String> existing = query.apply(sortedIds.subList(start,
                    Math.min(start + ID_QUERY_BATCH_SIZE, sortedIds.size())));
            if (!existing.isEmpty()) duplicate(existing.stream().sorted().findFirst().orElseThrow());
        }
    }

    private static void unique(Set<String> ids, String id) { if (!ids.add(id)) duplicate(id); }
    private static void duplicate(String id) { throw new BusinessException("중복 ID입니다: " + id, ErrorCode.DUPLICATE_RESOURCE); }
    private static void sameId(String path, String body) {
        if (!path.equals(body)) throw new IllegalArgumentException("경로 ID와 본문 ID가 일치해야 합니다.");
    }
}
