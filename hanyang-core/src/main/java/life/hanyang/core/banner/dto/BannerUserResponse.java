package life.hanyang.core.banner.dto;
import life.hanyang.core.banner.domain.Banner;
import java.time.Instant;

public record BannerUserResponse(
        Long id,
        String imageUrl,
        String altText,
        String clickUrl,
        Integer displayOrder
){
    public BannerUserResponse(Banner banner) {
        this(
                banner.getId(),
                banner.getImageUrl(),
                banner.getAltText(),
                banner.getClickUrl(),
                banner.getDisplayOrder()
        );
    }
}
