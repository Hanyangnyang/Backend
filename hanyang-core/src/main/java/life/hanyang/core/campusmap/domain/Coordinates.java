package life.hanyang.core.campusmap.domain;

import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Embeddable
@NoArgsConstructor
public class Coordinates {
    @DecimalMin("-90") @DecimalMax("90")
    private Double latitude;
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
