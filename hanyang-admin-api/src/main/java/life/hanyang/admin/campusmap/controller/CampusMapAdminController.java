package life.hanyang.admin.campusmap.controller;

import life.hanyang.core.campusmap.dto.CampusMapDto.*;
import life.hanyang.core.campusmap.service.*;
import life.hanyang.core.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/campus-map")
@Tag(name = "관리자 캠퍼스맵 API")
public class CampusMapAdminController {
    private final CampusMapService service;
    private final CampusMapImportReader importReader;

    @Operation(summary = "건물 및 소속 오픈스페이스 등록", description = "응답은 success/data/error 구조입니다. 클라이언트가 건물·공간 ID를 지정합니다. 기존 ID와 동일 캠퍼스의 건물 번호 중복은 409입니다. openSpaces 누락·null·[]은 공간 없이 등록합니다. 문자열 배열은 누락·null·[]이면 비워지며, 전달한 배열로 교체됩니다. 배열 항목은 null 또는 공백일 수 없습니다. ", responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "등록 성공", useReturnTypeSchema = true),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "C001: 입력 형식·필수값·ID 불일치·검증 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "관리자 인증 실패", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "관리자 권한 없음", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "C005: ID 또는 캠퍼스·건물 번호 중복", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "C004: 예상하지 못한 서버 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class)))
    })
    @PostMapping("/buildings")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<BuildingResponse> createBuilding(@RequestBody BuildingRequest request) {
        return ApiResponse.success(service.createBuilding(request));
    }
    @Operation(summary = "건물 전체 정보 수정", description = "응답은 success/data/error 구조입니다. 전체 정보 수정(PUT)입니다. 필수 필드를 다시 보내야 하며, 경로 id와 본문 id는 같아야 합니다. 생략한 선택 필드는 null로 변경됩니다. openSpaces 누락·null은 기존 공간을 유지합니다. 배열을 보내면 전체 공간 목록을 동기화하며, 기존 ID는 수정하고 새 ID는 추가하고 목록에서 빠진 공간은 삭제합니다. []는 소속 공간 전체 삭제입니다. 다른 건물의 공간 ID는 사용할 수 없습니다. 문자열 배열은 누락·null·[]이면 비워지며, 전달한 배열로 교체됩니다. 배열 항목은 null 또는 공백일 수 없습니다. 같은 캠퍼스 내 건물 번호 중복은 409입니다.", responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "처리 성공", useReturnTypeSchema = true),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "C001: 입력 형식·필수값·ID 불일치·검증 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "관리자 인증 실패", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "관리자 권한 없음", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "C003: 대상 리소스 없음", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "C005: ID 또는 캠퍼스·건물 번호 중복", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "C004: 예상하지 못한 서버 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class)))
    })
    @PutMapping("/buildings/{id}")
    public ApiResponse<BuildingResponse> updateBuilding(@PathVariable String id, @RequestBody BuildingRequest request) {
        return ApiResponse.success(service.updateBuilding(id, request));
    }
    @Operation(summary = "건물 및 소속 오픈스페이스 삭제", description = "응답은 success/data/error 구조입니다. 건물을 삭제하면 소속 오픈스페이스도 함께 삭제합니다. 성공 응답의 data는 null입니다.", responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "처리 성공", useReturnTypeSchema = true),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "C001: 입력 형식·필수값·ID 불일치·검증 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "관리자 인증 실패", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "관리자 권한 없음", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "C003: 대상 리소스 없음", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "C004: 예상하지 못한 서버 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class)))
    })
    @DeleteMapping("/buildings/{id}")
    public ApiResponse<Void> deleteBuilding(@PathVariable String id) {
        service.deleteBuilding(id);
        return ApiResponse.success();
    }
    @Operation(summary = "건물 및 소속 오픈스페이스 JSON 배열 파일 일괄 등록", description = "응답은 success/data/error 구조입니다. multipart/form-data의 file 필드에 비어 있지 않은 JSON 배열 파일을 전송합니다. 파일과 전체 multipart 요청의 제한은 각각 5MB(5×1024×1024바이트)이며, 요청 부가 정보가 포함되므로 파일은 5MB보다 작게 준비하세요. 알 수 없는 필드, 잘못된 타입, null 항목 및 빈 최상위 배열은 거부됩니다. 마지막 쉼표는 허용합니다. 파일 전체를 검증하며, 중복이나 오류가 하나라도 있으면 전체 등록이 취소됩니다. 기존 데이터 덮어쓰기나 전체 초기화는 하지 않습니다. 각 항목은 BuildingRequest 형식입니다. 건물·공간 ID 및 캠퍼스별 건물 번호는 중복될 수 없습니다. data는 buildings/openSpaces/smokingAreas 등록 건수이며 smokingAreas는 0입니다.", responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "등록 성공", useReturnTypeSchema = true),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "C001: 입력 형식·필수값·ID 불일치·검증 오류; F001: 업로드 용량 초과", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "관리자 인증 실패", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "관리자 권한 없음", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "C005: ID 또는 캠퍼스·건물 번호 중복", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "C004: 예상하지 못한 서버 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class)))
    })
    @PostMapping(value = "/buildings/import", consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ImportResult> importBuildings(@Parameter(description = "JSON 배열 파일. BuildingRequest/SmokingAreaRequest/ParkingLotRequest 예시를 배열로 감싸 업로드") @RequestParam("file") MultipartFile file) {
        return ApiResponse.success(service.importBuildings(importReader.read(file, BuildingRequest.class)));
    }

    @Operation(summary = "흡연장 등록", description = "응답은 success/data/error 구조입니다. 클라이언트가 ID를 지정합니다. 기존 ID는 409입니다. type은 BOOTH(부스) 또는 AREA(구역), hasAshtray는 필수 boolean입니다. 문자열 배열은 누락·null·[]이면 비워지며, 전달한 배열로 교체됩니다. 배열 항목은 null 또는 공백일 수 없습니다. ", responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "등록 성공", useReturnTypeSchema = true),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "C001: 입력 형식·필수값·ID 불일치·검증 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "관리자 인증 실패", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "관리자 권한 없음", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "C005: ID 또는 캠퍼스·건물 번호 중복", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "C004: 예상하지 못한 서버 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class)))
    })
    @PostMapping("/smoking-areas")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SmokingAreaResponse> createSmokingArea(@RequestBody SmokingAreaRequest request) {
        return ApiResponse.success(service.createSmokingArea(request));
    }
    @Operation(summary = "흡연장 전체 정보 수정", description = "응답은 success/data/error 구조입니다. 전체 정보 수정(PUT)입니다. 필수 필드를 다시 보내야 하며, 경로 id와 본문 id는 같아야 합니다. 생략한 선택 필드는 null로 변경됩니다. 문자열 배열은 누락·null·[]이면 비워지며, 전달한 배열로 교체됩니다. 배열 항목은 null 또는 공백일 수 없습니다. ", responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "처리 성공", useReturnTypeSchema = true),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "C001: 입력 형식·필수값·ID 불일치·검증 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "관리자 인증 실패", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "관리자 권한 없음", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "C003: 대상 리소스 없음", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "C004: 예상하지 못한 서버 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class)))
    })
    @PutMapping("/smoking-areas/{id}")
    public ApiResponse<SmokingAreaResponse> updateSmokingArea(@PathVariable String id, @RequestBody SmokingAreaRequest request) {
        return ApiResponse.success(service.updateSmokingArea(id, request));
    }
    @Operation(summary = "흡연장 삭제", description = "응답은 success/data/error 구조입니다. 흡연장을 삭제합니다. 성공 응답의 data는 null입니다.", responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "처리 성공", useReturnTypeSchema = true),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "C001: 입력 형식·필수값·ID 불일치·검증 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "관리자 인증 실패", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "관리자 권한 없음", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "C003: 대상 리소스 없음", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "C004: 예상하지 못한 서버 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class)))
    })
    @DeleteMapping("/smoking-areas/{id}")
    public ApiResponse<Void> deleteSmokingArea(@PathVariable String id) {
        service.deleteSmokingArea(id);
        return ApiResponse.success();
    }
    @Operation(summary = "흡연장 JSON 배열 파일 일괄 등록", description = "응답은 success/data/error 구조입니다. multipart/form-data의 file 필드에 비어 있지 않은 JSON 배열 파일을 전송합니다. 파일과 전체 multipart 요청의 제한은 각각 5MB(5×1024×1024바이트)이며, 요청 부가 정보가 포함되므로 파일은 5MB보다 작게 준비하세요. 알 수 없는 필드, 잘못된 타입, null 항목 및 빈 최상위 배열은 거부됩니다. 마지막 쉼표는 허용합니다. 파일 전체를 검증하며, 중복이나 오류가 하나라도 있으면 전체 등록이 취소됩니다. 기존 데이터 덮어쓰기나 전체 초기화는 하지 않습니다. 각 항목은 SmokingAreaRequest 형식입니다. data는 buildings/openSpaces/smokingAreas 등록 건수이며 buildings와 openSpaces는 0입니다.", responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "등록 성공", useReturnTypeSchema = true),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "C001: 입력 형식·필수값·ID 불일치·검증 오류; F001: 업로드 용량 초과", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "관리자 인증 실패", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "관리자 권한 없음", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "C005: ID 또는 캠퍼스·건물 번호 중복", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "C004: 예상하지 못한 서버 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class)))
    })
    @PostMapping(value = "/smoking-areas/import", consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ImportResult> importSmokingAreas(@Parameter(description = "JSON 배열 파일. BuildingRequest/SmokingAreaRequest/ParkingLotRequest 예시를 배열로 감싸 업로드") @RequestParam("file") MultipartFile file) {
        return ApiResponse.success(service.importSmokingAreas(importReader.read(file, SmokingAreaRequest.class)));
    }

    @Operation(summary = "건물 소속 오픈스페이스 등록", description = "응답은 success/data/error 구조입니다. 기존 건물의 buildingId를 경로에 지정하고 새 공간의 ID를 본문에 지정합니다. 공간 ID는 모든 건물에서 중복될 수 없습니다. name은 필수이며 floor와 hint는 선택입니다.", responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "등록 성공", useReturnTypeSchema = true),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "C001: 입력 형식·필수값·ID 불일치·검증 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "관리자 인증 실패", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "관리자 권한 없음", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "C003: 대상 리소스 없음", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "C005: ID 또는 캠퍼스·건물 번호 중복", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "C004: 예상하지 못한 서버 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class)))
    })
    @PostMapping("/buildings/{buildingId}/open-spaces")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<OpenSpaceResponse> createOpenSpace(@PathVariable String buildingId, @RequestBody OpenSpaceRequest request) {
        return ApiResponse.success(service.createOpenSpace(buildingId, request));
    }
    @Operation(summary = "오픈스페이스 전체 정보 수정", description = "응답은 success/data/error 구조입니다. 전체 정보 수정(PUT)입니다. 필수 필드를 다시 보내야 하며, 경로 id와 본문 id는 같아야 합니다. 생략한 선택 필드는 null로 변경됩니다. name은 필수이며 floor와 hint 누락·null은 해당 값을 비웁니다. 소속 건물 이동 및 ID 변경은 지원하지 않습니다.", responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "처리 성공", useReturnTypeSchema = true),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "C001: 입력 형식·필수값·ID 불일치·검증 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "관리자 인증 실패", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "관리자 권한 없음", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "C003: 대상 리소스 없음", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "C004: 예상하지 못한 서버 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class)))
    })
    @PutMapping("/open-spaces/{id}")
    public ApiResponse<OpenSpaceResponse> updateOpenSpace(@PathVariable String id, @RequestBody OpenSpaceRequest request) {
        return ApiResponse.success(service.updateOpenSpace(id, request));
    }
    @Operation(summary = "오픈스페이스 삭제", description = "응답은 success/data/error 구조입니다. 해당 공간만 삭제하며 소속 건물은 유지합니다. 성공 응답의 data는 null입니다.", responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "처리 성공", useReturnTypeSchema = true),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "C001: 입력 형식·필수값·ID 불일치·검증 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "관리자 인증 실패", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "관리자 권한 없음", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "C003: 대상 리소스 없음", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "C004: 예상하지 못한 서버 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class)))
    })
    @DeleteMapping("/open-spaces/{id}")
    public ApiResponse<Void> deleteOpenSpace(@PathVariable String id) {
        service.deleteOpenSpace(id);
        return ApiResponse.success();
    }

    @Operation(summary = "주차장 등록", description = "응답은 success/data/error 구조입니다. 클라이언트가 ID를 지정합니다. 기존 ID는 409입니다. capacity는 선택값이며 입력 시 0 이상의 정수여야 합니다. 문자열 배열은 누락·null·[]이면 비워지며, 전달한 배열로 교체됩니다. 배열 항목은 null 또는 공백일 수 없습니다. ", responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "등록 성공", useReturnTypeSchema = true),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "C001: 입력 형식·필수값·ID 불일치·검증 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "관리자 인증 실패", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "관리자 권한 없음", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "C005: ID 또는 캠퍼스·건물 번호 중복", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "C004: 예상하지 못한 서버 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class)))
    })
    @PostMapping("/parking-lots")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ParkingLotResponse> createParkingLot(@RequestBody ParkingLotRequest request) {
        return ApiResponse.success(service.createParkingLot(request));
    }
    @Operation(summary = "주차장 전체 정보 수정", description = "응답은 success/data/error 구조입니다. 전체 정보 수정(PUT)입니다. 필수 필드를 다시 보내야 하며, 경로 id와 본문 id는 같아야 합니다. 생략한 선택 필드는 null로 변경됩니다. 문자열 배열은 누락·null·[]이면 비워지며, 전달한 배열로 교체됩니다. 배열 항목은 null 또는 공백일 수 없습니다. capacity 누락·null은 수용 대수 미상으로 변경합니다.", responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "처리 성공", useReturnTypeSchema = true),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "C001: 입력 형식·필수값·ID 불일치·검증 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "관리자 인증 실패", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "관리자 권한 없음", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "C003: 대상 리소스 없음", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "C004: 예상하지 못한 서버 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class)))
    })
    @PutMapping("/parking-lots/{id}")
    public ApiResponse<ParkingLotResponse> updateParkingLot(@PathVariable String id, @RequestBody ParkingLotRequest request) {
        return ApiResponse.success(service.updateParkingLot(id, request));
    }
    @Operation(summary = "주차장 삭제", description = "응답은 success/data/error 구조입니다. 주차장을 삭제합니다. 성공 응답의 data는 null입니다.", responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "처리 성공", useReturnTypeSchema = true),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "C001: 입력 형식·필수값·ID 불일치·검증 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "관리자 인증 실패", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "관리자 권한 없음", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "C003: 대상 리소스 없음", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "C004: 예상하지 못한 서버 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class)))
    })
    @DeleteMapping("/parking-lots/{id}")
    public ApiResponse<Void> deleteParkingLot(@PathVariable String id) {
        service.deleteParkingLot(id);
        return ApiResponse.success();
    }
    @Operation(summary = "주차장 JSON 배열 파일 일괄 등록", description = "응답은 success/data/error 구조입니다. multipart/form-data의 file 필드에 비어 있지 않은 JSON 배열 파일을 전송합니다. 파일과 전체 multipart 요청의 제한은 각각 5MB(5×1024×1024바이트)이며, 요청 부가 정보가 포함되므로 파일은 5MB보다 작게 준비하세요. 알 수 없는 필드, 잘못된 타입, null 항목 및 빈 최상위 배열은 거부됩니다. 마지막 쉼표는 허용합니다. 파일 전체를 검증하며, 중복이나 오류가 하나라도 있으면 전체 등록이 취소됩니다. 기존 데이터 덮어쓰기나 전체 초기화는 하지 않습니다. 각 항목은 ParkingLotRequest 형식입니다. capacity는 0 이상의 정수 또는 null입니다. data.parkingLots에 등록 건수를 반환합니다.", responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "등록 성공", useReturnTypeSchema = true),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "C001: 입력 형식·필수값·ID 불일치·검증 오류; F001: 업로드 용량 초과", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "관리자 인증 실패", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "관리자 권한 없음", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "C005: ID 또는 캠퍼스·건물 번호 중복", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "C004: 예상하지 못한 서버 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class)))
    })
    @PostMapping(value = "/parking-lots/import", consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ParkingImportResult> importParkingLots(@Parameter(description = "JSON 배열 파일. BuildingRequest/SmokingAreaRequest/ParkingLotRequest 예시를 배열로 감싸 업로드") @RequestParam("file") MultipartFile file) {
        return ApiResponse.success(service.importParkingLots(importReader.read(file, ParkingLotRequest.class)));
    }

}
