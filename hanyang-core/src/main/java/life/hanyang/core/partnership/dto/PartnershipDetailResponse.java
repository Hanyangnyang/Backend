package life.hanyang.core.partnership.dto;

import life.hanyang.core.partnership.domain.Department;
import life.hanyang.core.partnership.domain.Merchant;
import life.hanyang.core.partnership.domain.MerchantCategory;
import life.hanyang.core.partnership.domain.Partnership;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class PartnershipDetailResponse {
    private Long merchantId;
    private String storeName;
    private MerchantCategory merchantCategory;
    private Boolean isActive;
    private String emoji;
    private Double latitude;
    private Double longitude;
    private String fullAddress;
    private String kakaoPlaceId;
    private List<PartnershipInfo> partnerships;

    public PartnershipDetailResponse(Merchant merchant, List<PartnershipInfo> partnerships) {
        this.merchantId = merchant.getMerchantId();
        this.storeName = merchant.getStoreName();
        this.merchantCategory = merchant.getMerchantCategory();
        this.isActive = merchant.getIsActive();
        this.emoji = merchant.getEmoji();
        this.latitude = merchant.getLatitude();
        this.longitude = merchant.getLongitude();
        this.fullAddress = merchant.getFullAddress();
        this.kakaoPlaceId = merchant.getKakaoPlaceId();
        this.partnerships = partnerships;
    }

    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor
    public static class PartnershipInfo {
        private Long partnershipId;
        private Department department;
        private String benefit;
        private String conditions;
        private String sourceUrl;
        private Integer photoOrder;
        private LocalDate startDate;
        private LocalDate endDate;

        public PartnershipInfo(Partnership partnership) {
            this.partnershipId = partnership.getPartnershipId();
            this.department = partnership.getDepartment();
            this.benefit = partnership.getBenefit();
            this.conditions = partnership.getConditions();
            this.sourceUrl = partnership.getSourceUrl();
            this.photoOrder = partnership.getPhotoOrder();
            this.startDate = partnership.getStartDate();
            this.endDate = partnership.getEndDate();
        }
    }
}
