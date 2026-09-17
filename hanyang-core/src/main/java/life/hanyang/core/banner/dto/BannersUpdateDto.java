package life.hanyang.core.banner.dto;

import life.hanyang.core.banner.domain.BannerPlacement;

public record BannersUpdateDto(
        Long id,
        BannerPlacement placement,
        String altText,
        String clickUrl,
        Integer displayOrder,
        Boolean isActive
) {}
