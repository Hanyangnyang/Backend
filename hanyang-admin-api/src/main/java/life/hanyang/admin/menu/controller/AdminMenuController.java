package life.hanyang.admin.menu.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import life.hanyang.core.global.response.ApiResponse;
import life.hanyang.core.menu.entity.CafeteriaCode;
import life.hanyang.core.menu.dto.MenuUpdateRequest;
import life.hanyang.core.menu.service.MenuScrapingService;
import life.hanyang.core.menu.service.MenuSaveService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/menu")
@RequiredArgsConstructor
@Tag(name = "관리자 학식 API", description = "관리자용 학식 관리 및 스크래핑 API")
public class AdminMenuController {

    private final MenuScrapingService menuScrapingService;
    private final MenuSaveService menuSaveService;

    @Operation(summary = "수동으로 학식 스크래핑을 실행합니다. (기본 범위 D-7 ~ D+7 전체 식당 실행)")
    @PostMapping("/scrape")
    public ResponseEntity<ApiResponse<Void>> triggerScraping(
            @RequestParam(required = false) List<CafeteriaCode> codes,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) List<LocalDate> dates
    ) {
        menuScrapingService.scrapeCafeterias(codes, dates);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Operation(summary = "특정 식단의 화면 표시 메뉴 텍스트를 수동으로 변경(재정의)합니다.")
    @PatchMapping("/{menuId}")
    public ResponseEntity<ApiResponse<Void>> updateMenu(
            @PathVariable Long menuId,
            @RequestBody MenuUpdateRequest request
    ) {
        menuSaveService.updateMenuDisplay(menuId, request.displayMenu());
        return ResponseEntity.ok(ApiResponse.success());
    }
}
