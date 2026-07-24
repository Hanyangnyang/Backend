package life.hanyang.core.menu.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record MenuUpdateRequest(
        @Schema(description = "수정할 화면 표시 메뉴 텍스트", example = "• <b>맛있는 갈비탕</b>\n• 김치")
        String displayMenu
) {}
