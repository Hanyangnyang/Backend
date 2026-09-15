package life.hanyang.core.banner.dto;
import life.hanyang.core.banner.domain.Banner;
import life.hanyang.core.banner.domain.BannerPlacement;

public record BannerUserResponse(
        Long id,
        String imageUrl,
        BannerPlacement placement,
        String altText,
        String clickUrl,
        Integer displayOrder
){
    public BannerUserResponse(Banner banner) {
        this(
                banner.getId(),
                banner.getImageUrl(),
                banner.getPlacement(),
                banner.getAltText(),
                banner.getClickUrl(),
                banner.getDisplayOrder()
        );
    }
}
