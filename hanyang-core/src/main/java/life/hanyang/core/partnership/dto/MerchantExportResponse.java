package life.hanyang.core.partnership.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import life.hanyang.core.partnership.domain.Department;
import life.hanyang.core.partnership.domain.Merchant;
import life.hanyang.core.partnership.domain.MerchantCategory;
import life.hanyang.core.partnership.domain.Partnership;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record MerchantExportResponse(
        @JsonProperty("name") String storeName,
        MerchantCategory category,
        Boolean isActive,
        Location location,
        String emoji,
        String kakaoPlaceId,
        List<String> representativeMenus,
        List<PartnershipExport> partnerships) {

    public static MerchantExportResponse from(Merchant merchant) {
        return new MerchantExportResponse(merchant.getStoreName(), merchant.getMerchantCategory(),
                merchant.getIsActive(), new Location(merchant.getLatitude(), merchant.getLongitude(), merchant.getFullAddress()),
                merchant.getEmoji(), merchant.getKakaoPlaceId(), new ArrayList<>(merchant.getRepresentativeMenus()),
                merchant.getPartnerships().stream().sorted(Comparator.comparing(Partnership::getPartnershipId))
                        .map(PartnershipExport::from).toList());
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Location(Double latitude, Double longitude, String fullAddress) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record PartnershipExport(Department collegeName, String benefit, Period period,
                                    String conditions, String sourceUrl, Integer photoOrder) {
        public static PartnershipExport from(Partnership p) {
            return new PartnershipExport(p.getDepartment(), p.getBenefit(),
                    new Period(p.getStartDate(), p.getEndDate(), p.getIsActive()),
                    p.getConditions(), p.getSourceUrl(), p.getPhotoOrder());
        }
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Period(@JsonFormat(shape = JsonFormat.Shape.STRING) LocalDate startDate,
                         @JsonFormat(shape = JsonFormat.Shape.STRING) LocalDate endDate, Boolean isActive) {}
}
