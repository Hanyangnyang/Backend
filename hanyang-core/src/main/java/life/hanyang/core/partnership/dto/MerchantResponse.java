package life.hanyang.core.partnership.dto;

import life.hanyang.core.partnership.domain.Merchant;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public record MerchantResponse(
        Long merchantId,
        String storeName,
        String category,
        Boolean isActive,
        String emoji,
        Double latitude,
        Double longitude,
        String fullAddress,
        String kakaoPlaceId,
        Instant updatedAt,
        List<String> representativeMenus

) {

    public static MerchantResponse from(Merchant merchant) {
        return new MerchantResponse(
                merchant.getMerchantId(),
                merchant.getStoreName(),
                merchant.getMerchantCategory().name(), // Enum을 String으로 변환하거나 Enum 타입 그대로 사용
                merchant.getIsActive(),
                merchant.getEmoji(),
                merchant.getLatitude(),
                merchant.getLongitude(),
                merchant.getFullAddress(),
                merchant.getKakaoPlaceId(),
                merchant.getUpdatedAt(),
                new ArrayList<>(merchant.getRepresentativeMenus())
        );
    }
}