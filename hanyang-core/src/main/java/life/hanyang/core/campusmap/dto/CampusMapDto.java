package life.hanyang.core.campusmap.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import life.hanyang.core.campusmap.domain.*;
import java.util.List;

public final class CampusMapDto {
    private CampusMapDto() {}

    private static Coordinates coordinates(Coordinates value) {
        return value == null ? new Coordinates(null, null) : value;
    }

    public record OpenSpaceRequest(
            @NotBlank @Size(max = 100) String id,
            @NotBlank String name, String floor, String hint) {}

    public record BuildingRequest(
            @NotBlank @Size(max = 100) String id,
            @NotBlank @Size(max = 255) String buildingNumber,
            @NotBlank String name, String englishName,
            List<@NotBlank String> aliases,
            @NotNull Campus campus,
            @NotNull @Valid Coordinates coordinates,
            String description, List<@NotBlank String> primaryColleges,
            List<@NotNull @Valid OpenSpaceRequest> openSpaces,
            List<@NotBlank String> facilities, List<@NotBlank String> imageUrl) {}

    public record SmokingAreaRequest(
            @NotBlank @Size(max = 100) String id,
            @NotBlank String name, @NotNull SmokingAreaType type,
            @NotNull Campus campus, @NotNull @Valid Coordinates coordinates,
            @NotNull Boolean hasAshtray, String description,
            List<@NotBlank String> imageUrl) {}

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
    public record ParkingLotRequest(
            @NotBlank @Size(max = 100) String id,
            @NotBlank String name, @NotNull Campus campus,
            @NotNull @Valid Coordinates coordinates,
            @PositiveOrZero Integer capacity, String address, String description,
            List<@NotBlank String> imageUrl) {}

    public record ParkingLotResponse(String id, String name, Campus campus, Coordinates coordinates,
            Integer capacity, String address, String description, List<String> imageUrl) {
        public static ParkingLotResponse from(ParkingLot p) {
            return new ParkingLotResponse(p.getId(), p.getName(), p.getCampus(),
                    CampusMapDto.coordinates(p.getCoordinates()), p.getCapacity(), p.getAddress(),
                    p.getDescription(), List.copyOf(p.getImageUrl()));
        }
    }

    public record ParkingImportResult(int parkingLots) {}

    public record ImportResult(int buildings, int openSpaces, int smokingAreas) {}
}
