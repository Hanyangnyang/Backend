package life.hanyang.core.banner.dto;

import life.hanyang.core.banner.domain.BannerPlacement;

public record BannerRequest(
        BannerPlacement placement,
        String altText,
        String clickUrl,
        Integer displayOrder,
        Boolean isActive
){}
