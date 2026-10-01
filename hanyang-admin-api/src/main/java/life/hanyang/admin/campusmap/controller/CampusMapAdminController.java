package life.hanyang.admin.campusmap.controller;

import life.hanyang.core.campusmap.domain.Campus;
import life.hanyang.core.campusmap.dto.CampusMapDto.*;
import life.hanyang.core.campusmap.service.*;
import life.hanyang.core.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/campus-map")
@Tag(name = "관리자 캠퍼스맵 API")
public class CampusMapAdminController {
    private final CampusMapService service;
    private final CampusMapImportReader importReader;

    @GetMapping("/buildings")
    public ApiResponse<List<BuildingResponse>> getBuildings(@RequestParam(required = false) Campus campus) {
        return ApiResponse.success(service.getBuildings(campus));
    }
    @GetMapping("/buildings/{id}")
    public ApiResponse<BuildingResponse> getBuilding(@PathVariable String id) {
        return ApiResponse.success(service.getBuilding(id));
    }

    @PostMapping("/buildings")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<BuildingResponse> createBuilding(@RequestBody BuildingRequest request) {
        return ApiResponse.success(service.createBuilding(request));
    }
    @Operation(summary = "건물 전체 정보 수정", description = "openSpaces는 누락하거나 null이면 기존 공간을 유지합니다. 배열을 보내면 해당 목록으로 교체하며, 빈 배열은 전체 공간을 삭제합니다. 나머지 컬렉션은 누락하거나 빈 배열이면 비워집니다.")
    @PutMapping("/buildings/{id}")
    public ApiResponse<BuildingResponse> updateBuilding(@PathVariable String id, @RequestBody BuildingRequest request) {
        return ApiResponse.success(service.updateBuilding(id, request));
    }
    @DeleteMapping("/buildings/{id}")
    public ApiResponse<Void> deleteBuilding(@PathVariable String id) {
        service.deleteBuilding(id);
        return ApiResponse.success();
    }
    @Operation(summary = "건물 및 소속 오픈스페이스 JSON 배열 파일 일괄 등록", description = "5MB 이하 JSON 파일. 중복 ID 또는 잘못된 항목이 있으면 전체 등록 취소. 기존 데이터 덮어쓰기 없음.")
    @PostMapping(value = "/buildings/import", consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ImportResult> importBuildings(@RequestParam("file") MultipartFile file) {
        return ApiResponse.success(service.importBuildings(importReader.read(file, BuildingRequest.class)));
    }

    @GetMapping("/smoking-areas")
    public ApiResponse<List<SmokingAreaResponse>> getSmokingAreas(@RequestParam(required = false) Campus campus) {
        return ApiResponse.success(service.getSmokingAreas(campus));
    }
    @GetMapping("/smoking-areas/{id}")
    public ApiResponse<SmokingAreaResponse> getSmokingArea(@PathVariable String id) {
        return ApiResponse.success(service.getSmokingArea(id));
    }

    @PostMapping("/smoking-areas")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SmokingAreaResponse> createSmokingArea(@RequestBody SmokingAreaRequest request) {
        return ApiResponse.success(service.createSmokingArea(request));
    }
    @PutMapping("/smoking-areas/{id}")
    public ApiResponse<SmokingAreaResponse> updateSmokingArea(@PathVariable String id, @RequestBody SmokingAreaRequest request) {
        return ApiResponse.success(service.updateSmokingArea(id, request));
    }
    @DeleteMapping("/smoking-areas/{id}")
    public ApiResponse<Void> deleteSmokingArea(@PathVariable String id) {
        service.deleteSmokingArea(id);
        return ApiResponse.success();
    }
    @Operation(summary = "흡연장 JSON 배열 파일 일괄 등록", description = "5MB 이하 JSON 파일. 중복 ID 또는 잘못된 항목이 있으면 전체 등록 취소. 기존 데이터 덮어쓰기 없음.")
    @PostMapping(value = "/smoking-areas/import", consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ImportResult> importSmokingAreas(@RequestParam("file") MultipartFile file) {
        return ApiResponse.success(service.importSmokingAreas(importReader.read(file, SmokingAreaRequest.class)));
    }

    @PostMapping("/buildings/{buildingId}/open-spaces")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<OpenSpaceResponse> createOpenSpace(@PathVariable String buildingId, @RequestBody OpenSpaceRequest request) {
        return ApiResponse.success(service.createOpenSpace(buildingId, request));
    }
    @PutMapping("/open-spaces/{id}")
    public ApiResponse<OpenSpaceResponse> updateOpenSpace(@PathVariable String id, @RequestBody OpenSpaceRequest request) {
        return ApiResponse.success(service.updateOpenSpace(id, request));
    }
    @DeleteMapping("/open-spaces/{id}")
    public ApiResponse<Void> deleteOpenSpace(@PathVariable String id) {
        service.deleteOpenSpace(id);
        return ApiResponse.success();
    }
    @GetMapping("/parking-lots")
    public ApiResponse<List<ParkingLotResponse>> getParkingLots(@RequestParam(required = false) Campus campus) {
        return ApiResponse.success(service.getParkingLots(campus));
    }
    @GetMapping("/parking-lots/{id}")
    public ApiResponse<ParkingLotResponse> getParkingLot(@PathVariable String id) {
        return ApiResponse.success(service.getParkingLot(id));
    }

    @PostMapping("/parking-lots")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ParkingLotResponse> createParkingLot(@RequestBody ParkingLotRequest request) {
        return ApiResponse.success(service.createParkingLot(request));
    }
    @PutMapping("/parking-lots/{id}")
    public ApiResponse<ParkingLotResponse> updateParkingLot(@PathVariable String id, @RequestBody ParkingLotRequest request) {
        return ApiResponse.success(service.updateParkingLot(id, request));
    }
    @DeleteMapping("/parking-lots/{id}")
    public ApiResponse<Void> deleteParkingLot(@PathVariable String id) {
        service.deleteParkingLot(id);
        return ApiResponse.success();
    }
    @Operation(summary = "주차장 JSON 배열 파일 일괄 등록", description = "5MB 이하 JSON 파일. 중복 ID 또는 잘못된 항목이 있으면 전체 등록 취소. 기존 데이터 덮어쓰기 없음.")
    @PostMapping(value = "/parking-lots/import", consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ParkingImportResult> importParkingLots(@RequestParam("file") MultipartFile file) {
        return ApiResponse.success(service.importParkingLots(importReader.read(file, ParkingLotRequest.class)));
    }

}
