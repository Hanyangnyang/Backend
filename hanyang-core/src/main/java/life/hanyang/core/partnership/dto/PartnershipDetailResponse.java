package life.hanyang.core.partnership.dto;

import life.hanyang.core.partnership.domain.Department;
import life.hanyang.core.partnership.domain.Merchant;
import life.hanyang.core.partnership.domain.MerchantCategory;
import life.hanyang.core.partnership.domain.Partnership;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
public class PartnershipDetailResponse {
    private final Long merchantId;
    private final String storeName;
    private final MerchantCategory merchantCategory;
    private final Boolean isActive;
    private final String emoji;
    private final Double latitude;
    private final Double longitude;
    private final String fullAddress;
    private final String kakaoPlaceId;
    private final List<PartnershipInfo> partnerships;

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
    public static class PartnershipInfo {
        private final Long partnershipId;
        private final Department department;
        private final String benefit;
        private final String conditions;
        private final String sourceUrl;
        private final Integer photoOrder;
        private final LocalDate startDate;
        private final LocalDate endDate;

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
