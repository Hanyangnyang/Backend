package life.hanyang.user.menu.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import life.hanyang.core.global.response.ApiResponse;
import life.hanyang.core.menu.dto.DailyMenuResponse;
import life.hanyang.core.menu.dto.MenuResponse;
import life.hanyang.core.menu.entity.CafeteriaCode;
import life.hanyang.core.menu.service.MenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/menu")
@RequiredArgsConstructor
@Tag(name = "학식 API", description = "학식 관련 정보를 조회합니다.")
public class MenuController {

    private final MenuService menuService;

    @Operation(summary = "식단 정보를 불러옵니다. 기본값: 일주일 전 ~ 일주일 뒤, 전체 식당")
    @GetMapping
    public ResponseEntity<ApiResponse<Map<LocalDate, List<MenuResponse>>>> getMenus(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) List<CafeteriaCode> codes
    ) {
        List<DailyMenuResponse> dailyMenus = menuService.getDailyMenus(startDate, endDate, codes);
        Map<LocalDate, List<MenuResponse>> response = dailyMenus.stream()
                .collect(Collectors.toMap(
                        DailyMenuResponse::date,
                        DailyMenuResponse::cafeterias,
                        (existing, replacement) -> existing,
                        LinkedHashMap::new
                ));
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
