package life.hanyang.core.partnership.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Getter;
import lombok.NoArgsConstructor;

import life.hanyang.core.partnership.domain.Department;

@Getter
@NoArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class PartnershipDetailDto {
    private Department collegeName;
    private String benefit;
    private PartnershipPeriodDto period;
    private String conditions;
    private String sourceUrl;
}