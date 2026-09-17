package life.hanyang.core.banner.dto;
import life.hanyang.core.banner.domain.Banner;
import life.hanyang.core.banner.domain.BannerPlacement;
import java.time.Instant;

public record BannerResponse(
        Long id,
        String imageUrl,
        BannerPlacement placement,
        String altText,
        String clickUrl,
        Integer displayOrder,
        Boolean isActive,
        Instant createdAt
){
    public BannerResponse(Banner banner) {
        this(
                banner.getId(),
                banner.getImageUrl(),
                banner.getPlacement(),
                banner.getAltText(),
                banner.getClickUrl(),
                banner.getDisplayOrder(),
                banner.getIsActive(),
                banner.getCreatedAt()
        );
    }
}
