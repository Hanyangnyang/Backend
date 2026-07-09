package life.hanyang.core.banner.dto;

public record BannerRequest(
        String altText,
        String clickUrl,
        Integer displayOrder,
        Boolean isActive
){}
