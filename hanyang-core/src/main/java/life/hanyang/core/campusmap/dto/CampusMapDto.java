package life.hanyang.core.campusmap.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import life.hanyang.core.campusmap.domain.*;
import java.util.List;

public final class CampusMapDto {
    private CampusMapDto() {}

    private static Coordinates coordinates(Coordinates value) {
        return value == null ? new Coordinates(null, null) : value;
    }

    @Schema(description = "오픈스페이스 등록·전체 수정 요청", example = "{\"id\": \"openspace-01\", \"name\": \"오픈스페이스\", \"floor\": \"3층\", \"hint\": \"국제처와 카페ING 사이\"}")
    public record OpenSpaceRequest(
            @NotBlank @Size(max = 100) @Schema(description = "클라이언트가 지정하는 문자열 ID. 요청 시 필수·공백 불가·최대 100자. 수정 시 경로 ID와 동일", example = "openspace-01") String id,
            @NotBlank @Schema(description = "이름. 필수이며 공백 불가") String name, @Schema(description = "공간 층 안내. 선택 문자열", example = "3층", nullable = true) String floor, @Schema(description = "공간 위치 안내. 선택값", example = "국제처와 카페ING 사이", nullable = true) String hint) {}

    @Schema(description = "건물 등록·전체 수정 요청. JSON 일괄 등록 시 이 객체들을 배열로 감싸 전송", example = "{\"id\": \"building-102\", \"buildingNumber\": \"102\", \"name\": \"학생복지관\", \"englishName\": \"Student Welfare Building\", \"aliases\": [\"복지관\"], \"campus\": \"ANSAN\", \"coordinates\": {\"latitude\": 37.2979595559148, \"longitude\": 126.834367540313}, \"description\": \"\", \"primaryColleges\": [], \"openSpaces\": [{\"id\": \"openspace-01\", \"name\": \"오픈스페이스\", \"floor\": \"3층\", \"hint\": \"국제처와 카페ING 사이\"}], \"facilities\": [\"학생식당\", \"편의점\"], \"imageUrl\": [\"https://example.com/building.png\"]}")
    public record BuildingRequest(
            @NotBlank @Size(max = 100) @Schema(description = "클라이언트가 지정하는 문자열 ID. 요청 시 필수·공백 불가·최대 100자. 수정 시 경로 ID와 동일", example = "building-102") String id,
            @NotBlank @Size(max = 255) @Schema(description = "캠퍼스 내 고유 건물 번호. 필수·최대 255자", example = "102") String buildingNumber,
            @NotBlank @Schema(description = "이름. 필수이며 공백 불가") String name, @Schema(description = "영문 건물명. 선택값", example = "Student Welfare Building", nullable = true) String englishName,
            @Schema(description = "건물 별칭 목록. 등록·수정 시 누락/null/[]은 빈 목록. 항목은 공백·null 불가", example = "[\"복지관\"]") List<@NotBlank String> aliases,
            @NotNull @Schema(description = "캠퍼스. 요청 시 필수. ANSAN: ERICA, SEOUL: 서울", example = "ANSAN") Campus campus,
            @NotNull @Valid @Schema(description = "요청 시 객체 필수. latitude/longitude는 함께 입력하거나 둘 다 null. 위치 미상 예: {\"latitude\":null,\"longitude\":null}") Coordinates coordinates,
            @Schema(description = "장소 설명. 선택값", nullable = true) String description, @Schema(description = "주요 소속 대학·기관 목록. 누락/null/[]은 빈 목록. 항목은 공백·null 불가", example = "[\"공학대학\"]") List<@NotBlank String> primaryColleges,
            @Schema(description = "등록 시 누락/null은 공간 없음. 수정 시 누락/null은 유지, 배열은 전체 동기화, []는 전체 삭제. 공간 ID 중복 및 다른 건물 공간 사용 불가") List<@NotNull @Valid OpenSpaceRequest> openSpaces,
            @Schema(description = "편의시설 목록. 누락/null/[]은 빈 목록. 항목은 공백·null 불가", example = "[\"카페\",\"편의점\"]") List<@NotBlank String> facilities, @Schema(description = "이미지 URL 문자열 배열. 순서 유지. 누락/null/[]은 빈 목록. 항목은 공백·null 불가. URL 형식 자체는 검증하지 않음", example = "[\"https://example.com/building.png\"]") List<@NotBlank String> imageUrl) {}

    @Schema(description = "흡연장 등록·전체 수정 요청. JSON 일괄 등록 시 이 객체들을 배열로 감싸 전송", example = "{\"id\": \"ansan-smoking-01\", \"name\": \"문과대 건물 흡연부스\", \"type\": \"BOOTH\", \"campus\": \"ANSAN\", \"coordinates\": {\"latitude\": 37.3002924187283, \"longitude\": 126.83539445133155}, \"hasAshtray\": true, \"description\": \"솔성관 근처 흡연부스\", \"imageUrl\": []}")
    public record SmokingAreaRequest(
            @NotBlank @Size(max = 100) @Schema(description = "클라이언트가 지정하는 문자열 ID. 요청 시 필수·공백 불가·최대 100자. 수정 시 경로 ID와 동일", example = "ansan-smoking-01") String id,
            @NotBlank @Schema(description = "이름. 필수이며 공백 불가") String name, @NotNull @Schema(description = "흡연장 유형. 필수. BOOTH: 흡연부스, AREA: 흡연구역", example = "BOOTH") SmokingAreaType type,
            @NotNull @Schema(description = "캠퍼스. 요청 시 필수. ANSAN: ERICA, SEOUL: 서울", example = "ANSAN") Campus campus, @NotNull @Valid @Schema(description = "요청 시 객체 필수. latitude/longitude는 함께 입력하거나 둘 다 null. 위치 미상 예: {\"latitude\":null,\"longitude\":null}") Coordinates coordinates,
            @NotNull @Schema(description = "재떨이 유무. 필수 boolean", example = "true") Boolean hasAshtray, @Schema(description = "장소 설명. 선택값", nullable = true) String description,
            @Schema(description = "이미지 URL 문자열 배열. 순서 유지. 누락/null/[]은 빈 목록. 항목은 공백·null 불가. URL 형식 자체는 검증하지 않음", example = "[\"https://example.com/building.png\"]") List<@NotBlank String> imageUrl) {}

    public record OpenSpaceResponse(String id, String floor, String name, String hint) {
        public static OpenSpaceResponse from(OpenSpace s) {
            return new OpenSpaceResponse(s.getId(), s.getFloor(), s.getName(), s.getHint());
        }
    }

    public record BuildingResponse(String id, String buildingNumber, String name, String englishName,
            List<String> aliases, Campus campus, Coordinates coordinates, String description,
            List<String> primaryColleges, List<OpenSpaceResponse> openSpaces,
            List<String> facilities, List<String> imageUrl) {
        public static BuildingResponse from(CampusBuilding b) {
            return new BuildingResponse(b.getId(), b.getBuildingNumber(), b.getName(), b.getEnglishName(),
                    List.copyOf(b.getAliases()), b.getCampus(), CampusMapDto.coordinates(b.getCoordinates()), b.getDescription(),
                    List.copyOf(b.getPrimaryColleges()), b.getOpenSpaces().stream().map(OpenSpaceResponse::from).toList(),
                    List.copyOf(b.getFacilities()), List.copyOf(b.getImageUrl()));
        }
    }

    public record SmokingAreaResponse(String id, String name, SmokingAreaType type, Campus campus,
            Coordinates coordinates, Boolean hasAshtray, String description, List<String> imageUrl) {
        public static SmokingAreaResponse from(SmokingArea s) {
            return new SmokingAreaResponse(s.getId(), s.getName(), s.getType(), s.getCampus(),
                    CampusMapDto.coordinates(s.getCoordinates()), s.getHasAshtray(), s.getDescription(), List.copyOf(s.getImageUrl()));
        }
    }
    @Schema(description = "주차장 등록·전체 수정 요청. JSON 일괄 등록 시 이 객체들을 배열로 감싸 전송", example = "{\"id\": \"ansan-parking-01\", \"name\": \"제1주차장\", \"campus\": \"ANSAN\", \"coordinates\": {\"latitude\": 37.29937450655232, \"longitude\": 126.83785523939314}, \"capacity\": 143, \"address\": \"경기 안산시 상록구 학사4길 1\", \"description\": \"정문 근처\", \"imageUrl\": []}")
    public record ParkingLotRequest(
            @NotBlank @Size(max = 100) @Schema(description = "클라이언트가 지정하는 문자열 ID. 요청 시 필수·공백 불가·최대 100자. 수정 시 경로 ID와 동일", example = "ansan-parking-01") String id,
            @NotBlank @Schema(description = "이름. 필수이며 공백 불가") String name, @NotNull @Schema(description = "캠퍼스. 요청 시 필수. ANSAN: ERICA, SEOUL: 서울", example = "ANSAN") Campus campus,
            @NotNull @Valid @Schema(description = "요청 시 객체 필수. latitude/longitude는 함께 입력하거나 둘 다 null. 위치 미상 예: {\"latitude\":null,\"longitude\":null}") Coordinates coordinates,
            @PositiveOrZero @Schema(description = "주차 수용 대수. 선택값이며 null은 미상. 입력 시 0 이상 정수", example = "143", nullable = true) Integer capacity, @Schema(description = "주차장 주소. 선택값", example = "경기 안산시 상록구 학사4길 1", nullable = true) String address, @Schema(description = "장소 설명. 선택값", nullable = true) String description,
            @Schema(description = "이미지 URL 문자열 배열. 순서 유지. 누락/null/[]은 빈 목록. 항목은 공백·null 불가. URL 형식 자체는 검증하지 않음", example = "[\"https://example.com/building.png\"]") List<@NotBlank String> imageUrl) {}

    public record ParkingLotResponse(String id, String name, Campus campus, Coordinates coordinates,
            Integer capacity, String address, String description, List<String> imageUrl) {
        public static ParkingLotResponse from(ParkingLot p) {
            return new ParkingLotResponse(p.getId(), p.getName(), p.getCampus(),
                    CampusMapDto.coordinates(p.getCoordinates()), p.getCapacity(), p.getAddress(),
                    p.getDescription(), List.copyOf(p.getImageUrl()));
        }
    }

    public record ParkingImportResult(@Schema(description = "등록된 주차장 수", example = "1") int parkingLots) {}

    public record ImportResult(@Schema(description = "등록된 건물 수. 흡연장 가져오기는 0", example = "1") int buildings, @Schema(description = "등록된 소속 공간 수. 흡연장 가져오기는 0", example = "1") int openSpaces, @Schema(description = "등록된 흡연장 수. 건물 가져오기는 0", example = "0") int smokingAreas) {}
}
