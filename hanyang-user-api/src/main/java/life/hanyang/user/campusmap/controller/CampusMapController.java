package life.hanyang.user.campusmap.controller;

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
@RequestMapping("/api/v1/campus-map")
@Tag(name = "캠퍼스맵 API")
public class CampusMapController {
    private final CampusMapService service;

    @GetMapping("/buildings")
    public ApiResponse<List<BuildingResponse>> getBuildings(@RequestParam(required = false) Campus campus) {
        return ApiResponse.success(service.getBuildings(campus));
    }
    @GetMapping("/buildings/{id}")
    public ApiResponse<BuildingResponse> getBuilding(@PathVariable String id) {
        return ApiResponse.success(service.getBuilding(id));
    }

    @GetMapping("/smoking-areas")
    public ApiResponse<List<SmokingAreaResponse>> getSmokingAreas(@RequestParam(required = false) Campus campus) {
        return ApiResponse.success(service.getSmokingAreas(campus));
    }
    @GetMapping("/smoking-areas/{id}")
    public ApiResponse<SmokingAreaResponse> getSmokingArea(@PathVariable String id) {
        return ApiResponse.success(service.getSmokingArea(id));
    }
    @GetMapping("/parking-lots")
    public ApiResponse<List<ParkingLotResponse>> getParkingLots(@RequestParam(required = false) Campus campus) {
        return ApiResponse.success(service.getParkingLots(campus));
    }
    @GetMapping("/parking-lots/{id}")
    public ApiResponse<ParkingLotResponse> getParkingLot(@PathVariable String id) {
        return ApiResponse.success(service.getParkingLot(id));
    }
}
