package life.hanyang.core.partnership.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class MerchantCreateWithPartnershipsRequest {
    private String name;
    private String category;
    private Boolean isActive;
    private LocationDto location;
    private String emoji;
    private String kakaoPlaceId;
    private List<PartnershipDetailDto> partnerships;
}