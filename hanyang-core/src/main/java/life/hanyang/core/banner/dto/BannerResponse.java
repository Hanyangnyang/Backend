package life.hanyang.core.banner.dto;
import life.hanyang.core.banner.domain.Banner;
import java.time.Instant;

public record BannerResponse(
        Long id,
        String imageUrl,
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
                banner.getAltText(),
                banner.getClickUrl(),
                banner.getDisplayOrder(),
                banner.getIsActive(),
                banner.getCreatedAt()
        );
    }
}
