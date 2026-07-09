package life.hanyang.core.banner.dto;

public record BannersUpdateDto(
        Long id,
        String altText,
        String clickUrl,
        Integer displayOrder,
        Boolean isActive
) {}