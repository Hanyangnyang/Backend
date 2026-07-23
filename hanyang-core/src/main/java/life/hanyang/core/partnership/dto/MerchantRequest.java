package life.hanyang.core.partnership.dto;

import jakarta.validation.constraints.NotBlank;
import life.hanyang.core.partnership.domain.Merchant;
import life.hanyang.core.partnership.domain.MerchantCategory;

public record MerchantRequest(
        @NotBlank(message = "가게 이름은 필수 입력값입니다.")
        String storeName,
        @NotBlank(message = "카테고리는 필수 입력값입니다.")
        String category,
        Boolean isActive,
        @NotBlank(message = "이모지는 필수 입력값입니다.")
        String emoji,
        Double latitude,
        Double longitude,
        String fullAddress,
        String kakaoPlaceId
    ) {
    public Merchant toEntity() {
        return Merchant.builder()
                .storeName(this.storeName)
                .merchantCategory(MerchantCategory.fromValue(this.category)) // JSON 카테고리를 Enum으로 매핑
                .isActive(this.isActive) // null이 들어가도 앞서 구현한 생성자 null 체크 덕분에 Entity 내부에서 true로 변환됩니다!
                .emoji(this.emoji)
                .latitude(this.latitude)
                .longitude(this.longitude)
                .fullAddress(this.fullAddress)
                .kakaoPlaceId(this.kakaoPlaceId)
                .build();
    }
}