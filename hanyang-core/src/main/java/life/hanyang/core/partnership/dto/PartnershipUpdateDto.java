package life.hanyang.core.partnership.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import life.hanyang.core.partnership.domain.Department;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class PartnershipUpdateDto {
    private String benefit;
    private String conditions;
    private LocalDate startDate;
    private LocalDate endDate;
    private Department department;
    private String sourceUrl;
    private Boolean isActive;
    private Long merchantId;
    private Integer photoOrder;
}
