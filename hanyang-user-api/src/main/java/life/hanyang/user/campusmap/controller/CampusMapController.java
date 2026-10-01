package life.hanyang.user.campusmap.controller;

import life.hanyang.core.campusmap.domain.Campus;
import life.hanyang.core.campusmap.dto.CampusMapDto.*;
import life.hanyang.core.campusmap.service.*;
import life.hanyang.core.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/campus-map")
@Tag(name = "캠퍼스맵 API")
public class CampusMapController {
    private final CampusMapService service;

    @Operation(summary = "건물 목록 조회", description = "응답은 success/data/error 구조입니다. campus를 생략하면 전체 캠퍼스, 지정하면 ANSAN 또는 SEOUL만 조회합니다. ID 문자열 오름차순이며 페이지네이션은 없습니다. 결과가 없으면 data는 []입니다. 소속 openSpaces를 함께 반환하며 공간은 ID 오름차순입니다. 문자열 배열은 저장된 순서로 반환하며 값이 없으면 []입니다.", responses = {@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "C001: 올바르지 않은 파라미터", content = @Content(schema = @Schema(implementation = ApiResponse.class)))})
    @GetMapping("/buildings")
    public ApiResponse<List<BuildingResponse>> getBuildings(@Parameter(description = "생략 시 전체 캠퍼스. ANSAN: ERICA, SEOUL: 서울", example = "ANSAN") @RequestParam(required = false) Campus campus) {
        return ApiResponse.success(service.getBuildings(campus));
    }
    @Operation(summary = "건물 상세 조회", description = "응답은 success/data/error 구조입니다. 문자열 ID로 조회합니다. 소속 openSpaces를 함께 반환합니다. 없는 ID는 404(C003)입니다.", responses = {@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "C001: 올바르지 않은 파라미터", content = @Content(schema = @Schema(implementation = ApiResponse.class))), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "C003: 대상 리소스 없음", content = @Content(schema = @Schema(implementation = ApiResponse.class)))})
    @GetMapping("/buildings/{id}")
    public ApiResponse<BuildingResponse> getBuilding(@PathVariable String id) {
        return ApiResponse.success(service.getBuilding(id));
    }

    @Operation(summary = "흡연장 목록 조회", description = "응답은 success/data/error 구조입니다. campus를 생략하면 전체 캠퍼스, 지정하면 ANSAN 또는 SEOUL만 조회합니다. ID 문자열 오름차순이며 페이지네이션은 없습니다. 결과가 없으면 data는 []입니다. 문자열 배열은 저장된 순서로 반환하며 값이 없으면 []입니다.", responses = {@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "C001: 올바르지 않은 파라미터", content = @Content(schema = @Schema(implementation = ApiResponse.class)))})
    @GetMapping("/smoking-areas")
    public ApiResponse<List<SmokingAreaResponse>> getSmokingAreas(@Parameter(description = "생략 시 전체 캠퍼스. ANSAN: ERICA, SEOUL: 서울", example = "ANSAN") @RequestParam(required = false) Campus campus) {
        return ApiResponse.success(service.getSmokingAreas(campus));
    }
    @Operation(summary = "흡연장 상세 조회", description = "응답은 success/data/error 구조입니다. 문자열 ID로 조회합니다. 없는 ID는 404(C003)입니다.", responses = {@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "C001: 올바르지 않은 파라미터", content = @Content(schema = @Schema(implementation = ApiResponse.class))), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "C003: 대상 리소스 없음", content = @Content(schema = @Schema(implementation = ApiResponse.class)))})
    @GetMapping("/smoking-areas/{id}")
    public ApiResponse<SmokingAreaResponse> getSmokingArea(@PathVariable String id) {
        return ApiResponse.success(service.getSmokingArea(id));
    }
    @Operation(summary = "주차장 목록 조회", description = "응답은 success/data/error 구조입니다. campus를 생략하면 전체 캠퍼스, 지정하면 ANSAN 또는 SEOUL만 조회합니다. ID 문자열 오름차순이며 페이지네이션은 없습니다. 결과가 없으면 data는 []입니다. 문자열 배열은 저장된 순서로 반환하며 값이 없으면 []입니다.", responses = {@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "C001: 올바르지 않은 파라미터", content = @Content(schema = @Schema(implementation = ApiResponse.class)))})
    @GetMapping("/parking-lots")
    public ApiResponse<List<ParkingLotResponse>> getParkingLots(@Parameter(description = "생략 시 전체 캠퍼스. ANSAN: ERICA, SEOUL: 서울", example = "ANSAN") @RequestParam(required = false) Campus campus) {
        return ApiResponse.success(service.getParkingLots(campus));
    }
    @Operation(summary = "주차장 상세 조회", description = "응답은 success/data/error 구조입니다. 문자열 ID로 조회합니다. 없는 ID는 404(C003)입니다.", responses = {@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "C001: 올바르지 않은 파라미터", content = @Content(schema = @Schema(implementation = ApiResponse.class))), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "C003: 대상 리소스 없음", content = @Content(schema = @Schema(implementation = ApiResponse.class)))})
    @GetMapping("/parking-lots/{id}")
    public ApiResponse<ParkingLotResponse> getParkingLot(@PathVariable String id) {
        return ApiResponse.success(service.getParkingLot(id));
    }
}
