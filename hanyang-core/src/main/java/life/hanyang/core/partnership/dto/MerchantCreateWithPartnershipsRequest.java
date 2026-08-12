package life.hanyang.core.partnership.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import life.hanyang.core.partnership.domain.MerchantCategory;

import java.util.List;

public record MerchantCreateWithPartnershipsRequest(
        @NotBlank(message = "가게 이름은 필수 입력값입니다.")
        String storeName,

        @NotNull(message = "올바른 카테고리(food, cafe, pub, play, life)를 입력해야 합니다.")
        MerchantCategory category,

        Boolean isActive,

        @Valid
        LocationDto location,

        @NotBlank(message = "이모지는 필수 입력값입니다.")
        String emoji,

        String kakaoPlaceId,

        @Valid
        List<PartnershipDetailDto> partnerships
) {
}