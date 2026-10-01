package life.hanyang.core.campusmap.domain;

import jakarta.persistence.Embeddable;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Embeddable
@NoArgsConstructor
public class Coordinates {
    @Schema(description = "위도. 경도와 함께 입력하거나 둘 다 null", example = "37.2979595559148", nullable = true, minimum = "-90", maximum = "90")
    @DecimalMin("-90") @DecimalMax("90")
    private Double latitude;
    @Schema(description = "경도. 위도와 함께 입력하거나 둘 다 null", example = "126.834367540313", nullable = true, minimum = "-180", maximum = "180")
    @DecimalMin("-180") @DecimalMax("180")
    private Double longitude;

    public Coordinates(Double latitude, Double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    @AssertTrue(message = "위도와 경도는 함께 입력하거나 함께 비워야 합니다.")
    @com.fasterxml.jackson.annotation.JsonIgnore
    public boolean isPaired() {
        return (latitude == null) == (longitude == null);
    }
}
